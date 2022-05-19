package hse.dynamodbexperiment.awssdkdynamo

import hse.dynamodbexperiment.testcontainer.DynamoDbLocalstackCompanionObjectIT
import java.util.stream.Collectors
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType


/**
 * using implementation("software.amazon.awssdk:dynamodb:2.17.190")
 *
 * following: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/asynchronous.html
 *
 * based on: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-dynamodb-tables.html
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/best-practices.html
 */
@Testcontainers
class DynamoDbAwsJavaSdkIT {

    @Container
    val localStackContainer: LocalStackContainer =
        LocalStackContainer(DockerImageName.parse("localstack/localstack:0.14.2"))
            .withServices(
                LocalStackContainer.Service.DYNAMODB,
            )

    @Test
    fun crud() {
        val region = Region.of(localStackContainer.region)
        val dynamoDbEndpoint = DynamoDbLocalstackCompanionObjectIT.localStack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB)
        val credentialsProvider: () -> AwsCredentials = { // software.amazon.awssdk.auth.credentials.AwsCredentials
            AwsBasicCredentials.create( // software.amazon.awssdk.auth.credentials.AwsCredentials
                localStackContainer.accessKey,
                localStackContainer.secretKey
            )
        }

        val dynamoDbClient = DynamoDbClient.builder()
            .region(region)
            .endpointOverride(dynamoDbEndpoint)
            .credentialsProvider(credentialsProvider)
            .build()

        // create table
        // https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-dynamodb-tables.html
        val createTableName = createTable(dynamoDbClient, "myTable", "id")
        assertThat(createTableName).isEqualTo("myTable")

        // create
        putItemInTable(
            dynamoDbClient, "myTable",
            "id", "1",
            "albumTitle", "Nevermind",
            "awards", "Bambi",
            "songTitle", "Hello"
        )

        // read
        val item = getItem(dynamoDbClient, "myTable", "id", "1")
        assertThat(item).isEqualTo(mapOf("1" to "1", "Nevermind" to "Nevermind", "Bambi" to "Bambi", "Hello" to "Hello"))

        // update
        // read
        // delete
        // read

        // delete table
        deleteDynamoDBTable(dynamoDbClient, "myTable")
        // describe table
    }

    fun createTable(ddb: DynamoDbClient, tableName: String, key: String): String {
        val waiter = ddb.waiter()
        val createTableRequest = CreateTableRequest.builder()
            .attributeDefinitions(
                AttributeDefinition.builder()
                    .attributeName(key)
                    .attributeType(ScalarAttributeType.S)
                    .build()
            )
            .keySchema(
                KeySchemaElement.builder()
                    .attributeName(key)
                    .keyType(KeyType.HASH)
                    .build()
            )
            .provisionedThroughput(
                ProvisionedThroughput.builder()
                    .readCapacityUnits(10L)
                    .writeCapacityUnits(10L)
                    .build()
            )
            .tableName(tableName)
            .build()

        val createTableResponse: CreateTableResponse = ddb.createTable(createTableRequest)

        val describeTableRequest = DescribeTableRequest.builder()
            .tableName(tableName)
            .build()

        val waitUntilTableExists = waiter.waitUntilTableExists(describeTableRequest)
        waitUntilTableExists.matched().response().ifPresent(System.out::println)

        val tableName: String = createTableResponse.tableDescription().tableName()
        return tableName
    }

    fun deleteDynamoDBTable(ddb: DynamoDbClient, tableName: String) {
        val request: DeleteTableRequest = DeleteTableRequest.builder()
            .tableName(tableName)
            .build()
        try {
            ddb.deleteTable(request)
        } catch (e: DynamoDbException) {
            System.err.println(e.message)
            System.exit(1)
        }
        println("$tableName was successfully deleted!")
    }

    // https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-dynamodb-tables.html
    fun putItemInTable(
        ddb: DynamoDbClient,
        tableName: String,
        key: String,
        keyVal: String?,
        albumTitle: String,
        albumTitleValue: String?,
        awards: String,
        awardVal: String?,
        songTitle: String,
        songTitleVal: String?
    ) {
        val itemValues: HashMap<String, AttributeValue> = HashMap()

        // Add all content to the table
        itemValues[key] = AttributeValue.builder().s(keyVal).build()
        itemValues[songTitle] = AttributeValue.builder().s(songTitleVal).build()
        itemValues[albumTitle] = AttributeValue.builder().s(albumTitleValue).build()
        itemValues[awards] = AttributeValue.builder().s(awardVal).build()
        val request: PutItemRequest = PutItemRequest.builder()
            .tableName(tableName)
            .item(itemValues)
            .build()
        try {
            ddb.putItem(request)
            println("$tableName was successfully updated")
        } catch (e: ResourceNotFoundException) {
            System.err.format("Error: The Amazon DynamoDB table \"%s\" can't be found.\n", tableName)
            System.err.println("Be sure that it exists and that you've typed its name correctly!")
            System.exit(1)
        } catch (e: DynamoDbException) {
            System.err.println(e.message)
            System.exit(1)
        }
    }

    // TODO
    // https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-dynamodb-tables.html
    // uses DynamoDbAsyncClient
    fun getItem(client: DynamoDbClient, tableName: String, key: String, keyVal: String): Map<String, String> {

        val keyToGet = HashMap<String, AttributeValue>()

        keyToGet[key] = AttributeValue.builder()
            .s(keyVal).build()

        try {
            val request = GetItemRequest.builder()
                .key(keyToGet)
                .tableName(tableName)
                .build()

            // Invoke the DynamoDbAsyncClient object's getItem
            // Note: client.getItem(request) returns a CompletableFuture; we should be able to hanlde this via coroutines:
            // coroutines java 8: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-jdk8/index.html
            // kotlinx-coroutines-jdk8
            val item: Map<String, AttributeValue> = client.getItem(request).item()
            val returnedItem: Collection<AttributeValue> = item.values

            // Convert Set to Map
            val map: Map<String, String> = returnedItem.stream().collect(
                Collectors.toMap(
                    AttributeValue::s,
                    AttributeValue::s
                ))

            val keys = map.keys
            for (sinKey: String in keys) {
                System.out.format("%s: %s\n", sinKey, map[sinKey].toString())
            }

            return map

        } catch (e: DynamoDbException) {
            System.err.println(e.message)
            // System.exit(1)
            return mapOf()
        }
    }
}


