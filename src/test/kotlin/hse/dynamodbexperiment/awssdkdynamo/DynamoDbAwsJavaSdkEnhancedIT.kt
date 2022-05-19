package hse.dynamodbexperiment.awssdkdynamo

import hse.dynamodbexperiment.testcontainer.DynamoDbLocalstackCompanionObjectIT
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.core.async.SdkPublisher
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.enhanced.dynamodb.Expression
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException


/**
 * using:
 *   * implementation("software.amazon.awssdk:dynamodb-enhanced:2.17.191")
 *
 * following: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-dynamodb-enhanced.html
 *   (but using async variant of api)
 *   https://aws.amazon.com/blogs/developer/introducing-enhanced-dynamodb-client-in-the-aws-sdk-for-java-v2/
 *      says: True asynchronous support... CompletableFuture ...‘iterable’ results ... process results as an asynchronous stream
 *      https://elizarov.medium.com/reactive-streams-and-kotlin-flows-bfd12772cda4 : how to bridge reactive streams (java) and flow (kotlin)
 *
 * https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/dynamodb-enhanced-client.html
 * https://github.com/aws/aws-sdk-java-v2/tree/master/services-custom/dynamodb-enhanced
 *      > Mid-level DynamoDB mapper/abstraction for Java using the v2 AWS SDK.
 */
@Testcontainers
@OptIn(ExperimentalCoroutinesApi::class)
class DynamoDbAwsJavaSdkEnhancedIT {

    @Container
    val localStackContainer: LocalStackContainer =
        LocalStackContainer(DockerImageName.parse("localstack/localstack:0.14.2"))
            .withServices(
                LocalStackContainer.Service.DYNAMODB,
            )

    @Test
    fun crud() = runTest {
        val region = Region.of(localStackContainer.region)
        val dynamoDbEndpoint =
            DynamoDbLocalstackCompanionObjectIT.localStack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB)
        val credentialsProvider: () -> AwsCredentials = {
            AwsBasicCredentials.create(
                localStackContainer.accessKey,
                localStackContainer.secretKey
            )
        }

        val dynamoDbAsyncClient = DynamoDbAsyncClient.builder()
            .credentialsProvider(credentialsProvider)
            .region(region)
            .endpointOverride(dynamoDbEndpoint)
            .build()

        // but there is...
        val dynamoDbEnhancedAsyncClient =
            DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(dynamoDbAsyncClient).build()

        // create table
        // https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-dynamodb-enhanced.html
        // maybe done implicitly?
        // Table.createTable exists...
        createTable(dynamoDbEnhancedAsyncClient)

        // create
        putRecord(dynamoDbEnhancedAsyncClient)

        // read
        val email: String? = getItem(dynamoDbEnhancedAsyncClient)
        assertThat(email).isEqualTo("sred@noserver.com")

        // read collection
        val customerFlow: Flow<Customer> = getAll(dynamoDbEnhancedAsyncClient)
        val customerFromFlow: Customer = customerFlow.first()
        assertThat(customerFromFlow.email).isEqualTo(customerTestInstance().email)

        // update
        // read
        // delete
        // read

        // delete table
        deleteTable(dynamoDbEnhancedAsyncClient)
        // describe
    }

    suspend fun createTable(enhancedClient: DynamoDbEnhancedAsyncClient): String? {
        try {
            val mappedTable = enhancedClient.table(
                "Customer", TableSchema.fromBean(
                    Customer::class.java
                )
            )
            mappedTable.createTable().await()
        } catch (e: DynamoDbException) {
            System.err.println(e.message)
            System.exit(1)
        }
        return ""
    }

    // https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-dynamodb-enhanced.html
    // Customer from https://github.com/awsdocs/aws-doc-sdk-examples/blob/main/javav2/example_code/dynamodb/src/main/java/com/example/dynamodb/Customer.java
    suspend fun putRecord(enhancedClient: DynamoDbEnhancedAsyncClient) {
        val custTable = enhancedClient.table(
            "Customer", TableSchema.fromBean(
                Customer::class.java
            )
        )

        // Create an Instant
        val localDate: LocalDate = LocalDate.parse("2020-04-07")
        val localDateTime: LocalDateTime = localDate.atStartOfDay()
        val instant: Instant = localDateTime.toInstant(ZoneOffset.UTC)

        // Populate the Table
        val custRecord = Customer()
        custRecord.custName = "Susan red"
        custRecord.id = "id146"
        custRecord.email = "sred@noserver.com"
        custRecord.registrationDate = instant

        // Put the customer data into a DynamoDB table
        custTable.putItem(custRecord).await() // throws: DynamoDbException
    }

    private suspend fun getItem(enhancedClient: DynamoDbEnhancedAsyncClient): String {
        try {
            //Create a DynamoDbTable object
            val mappedTable = enhancedClient.table(
                "Customer", TableSchema.fromBean(
                    Customer::class.java
                )
            )

            //Create a KEY object
            val key: Key = Key.builder()
                .partitionValue("id146")
                .build()

            // Get the item by using the key
            val result = mappedTable.getItem { r: GetItemEnhancedRequest.Builder ->
                r.key(key)
            }
            return requireNotNull(result.await().email)
        } catch (e: DynamoDbException) {
            System.err.println(e.message)
            System.exit(1)
        }
        return ""
    }

    // TODO: add test for filtering
    fun queryTableFilter(enhancedClient: DynamoDbEnhancedAsyncClient): Flow<Customer> {
        try {
            val mappedTable = enhancedClient.table(
                "Customer", TableSchema.fromBean(
                    Customer::class.java
                )
            )
            val att: AttributeValue = AttributeValue.builder()
                .s("sblue@noserver.com")
                .build()
            val expressionValues: MutableMap<String, AttributeValue> = HashMap()
            expressionValues[":value"] = att
            val expression: Expression = Expression.builder()
                .expression("email = :value")
                .expressionValues(expressionValues)
                .build()

            // Create a QueryConditional object that is used in the query operation.
            val queryConditional = QueryConditional
                .keyEqualTo(
                    Key.builder().partitionValue("id146") // example was:  id103
                        .build()
                )

            // Get items in the Customer table and write out the ID value.
            // instance of org.reactivestreams.Publisher
            val results: SdkPublisher<Customer>? = mappedTable.query { r: QueryEnhancedRequest.Builder ->
                r.queryConditional(
                    queryConditional
                ).filterExpression(expression)
            }.items()

            // possible with sync client (Iterator instead of Publisher)
//            while (results.hasNext()) {
//                val rec = results.next()
//                println("The record id is " + rec.id)
//            }

            val customerFlow = results?.asFlow() ?: emptyFlow()

            return customerFlow

        } catch (e: DynamoDbException) {
            System.err.println(e.message)
            System.exit(1)
        }

        return emptyFlow()
    }

    // https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-dynamodb-enhanced.html
    // Retrieve (get) all items from a table
    suspend fun getAll(enhancedClient: DynamoDbEnhancedAsyncClient) : Flow<Customer>{
        val mappedTable = enhancedClient.table(
            "Customer", TableSchema.fromBean(
                Customer::class.java
            )
        )

        return mappedTable.scan().items().asFlow()
    }

    suspend fun deleteTable(enhancedClient: DynamoDbEnhancedAsyncClient): String? {
        try {
            val mappedTable = enhancedClient.table(
                "Customer", TableSchema.fromBean(
                    Customer::class.java
                )
            )
            mappedTable.deleteTable().await()
        } catch (e: DynamoDbException) {
            System.err.println(e.message)
            System.exit(1)
        }
        return ""
    }

    fun customerTestInstance(): Customer {       // Create an Instant
        val localDate: LocalDate = LocalDate.parse("2020-04-07")
        val localDateTime: LocalDateTime = localDate.atStartOfDay()
        val instant: Instant = localDateTime.toInstant(ZoneOffset.UTC)

        // Populate the Table
        val custRecord = Customer()
        custRecord.custName = "Susan red"
        custRecord.id = "id146"
        custRecord.email = "sred@noserver.com"
        custRecord.registrationDate = instant

        return custRecord
    }

}

// from: https://github.com/aws/aws-sdk-java-v2/tree/master/services-custom/dynamodb-enhanced
//@DynamoDbBean
//class Customer2 {
//    @get:DynamoDbPartitionKey
//    var accountId: String? = null
//
//    @get:DynamoDbSortKey
//    var subId = 0 // primitive types are supported
//
//
//    // Defines a GSI (customers_by_name) with a partition key of 'name'
//    @get:DynamoDbSecondaryPartitionKey(indexNames = ["customers_by_name"])
//    var name: String? = null
//
//    // Defines an LSI (customers_by_date) with a sort key of 'createdDate' and also declares the
//    // same attribute as a sort key for the GSI named 'customers_by_name'
//    @get:DynamoDbSecondarySortKey(indexNames = ["customers_by_date", "customers_by_name"])
//    var createdDate: Instant? = null
//}

// from https://github.com/awsdocs/aws-doc-sdk-examples/blob/main/javav2/example_code/dynamodb/src/main/java/com/example/dynamodb/Customer.java
// following: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-dynamodb-enhanced.html
@DynamoDbBean
class Customer {
    @get:DynamoDbPartitionKey
    var id: String? = null
    var custName: String? = null
    var email: String? = null
    var registrationDate: Instant? = null
}

