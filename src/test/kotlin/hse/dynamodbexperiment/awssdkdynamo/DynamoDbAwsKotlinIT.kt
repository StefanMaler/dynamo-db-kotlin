package hse.dynamodbexperiment.awssdkdynamo

import aws.sdk.kotlin.runtime.endpoint.AwsEndpoint
import aws.sdk.kotlin.runtime.endpoint.AwsEndpointResolver
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeDefinition
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.CreateTableRequest
import aws.sdk.kotlin.services.dynamodb.model.DynamoDbException
import aws.sdk.kotlin.services.dynamodb.model.KeySchemaElement
import aws.sdk.kotlin.services.dynamodb.model.KeyType
import aws.sdk.kotlin.services.dynamodb.model.ScalarAttributeType
import aws.sdk.kotlin.services.dynamodb.model.TableStatus
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.http.Url
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.epochMilliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * using implementation("aws.sdk.kotlin:dynamodb:0.15.0")
 *
 * FAILS (it seems like the library is not ready for production use; also indicated by the leading 0 in its version number)
 *
 * Exception in thread "DefaultDispatcher-worker-1 @request-context:execute-ktor-request#3" java.lang.NoSuchFieldError: Companion
 * at io.ktor.client.engine.okhttp.OkHttpEngineKt.convertToOkHttpBody(OkHttpEngine.kt:203)
 * at io.ktor.client.engine.okhttp.OkHttpEngineKt.convertToOkHttpRequest(OkHttpEngine.kt:192)
 * at io.ktor.client.engine.okhttp.OkHttpEngineKt.access$convertToOkHttpRequest(OkHttpEngine.kt:1)
 * at io.ktor.client.engine.okhttp.OkHttpEngine.execute(OkHttpEngine.kt:71)
 * at io.ktor.client.engine.HttpClientEngine$executeWithinCallContext$2.invokeSuspend(HttpClientEngine.kt:85)
 * (Coroutine boundary)
 * at io.ktor.client.engine.HttpClientEngine$DefaultImpls.executeWithinCallContext(HttpClientEngine.kt:86)
 * at io.ktor.client.engine.HttpClientEngine$install$1.invokeSuspend(HttpClientEngine.kt:65)
 * at io.ktor.client.features.HttpSend$DefaultSender.execute(HttpSend.kt:128)
 * at io.ktor.client.features.HttpSend$Feature$install$1.invokeSuspend(HttpSend.kt:89)
 * at io.ktor.client.features.HttpCallValidator$Companion$install$1.invokeSuspend(HttpCallValidator.kt:112)
 * at io.ktor.client.features.HttpRequestLifecycle$Feature$install$1.invokeSuspend(HttpRequestLifecycle.kt:37)
 * at io.ktor.client.HttpClient.execute(HttpClient.kt:191)
 * at io.ktor.client.statement.HttpStatement.executeUnsafe(HttpStatement.kt:104)
 * at io.ktor.client.statement.HttpStatement.execute(HttpStatement.kt:43)
 * at aws.smithy.kotlin.runtime.http.engine.ktor.KtorEngine.execute(KtorEngine.kt:99)
 * at aws.smithy.kotlin.runtime.http.engine.ktor.KtorEngine$roundTrip$2.invokeSuspend(KtorEngine.kt:72)
 * Caused by: java.lang.NoSuchFieldError: Companion
 * at io.ktor.client.engine.okhttp.OkHttpEngineKt.convertToOkHttpBody(OkHttpEngine.kt:203)
 * at io.ktor.client.engine.okhttp.OkHttpEngineKt.convertToOkHttpRequest(OkHttpEngine.kt:192)
 * at io.ktor.client.engine.okhttp.OkHttpEngineKt.access$convertToOkHttpRequest(OkHttpEngine.kt:1)
 * at io.ktor.client.engine.okhttp.OkHttpEngine.execute(OkHttpEngine.kt:71)
 * at io.ktor.client.engine.HttpClientEngine$executeWithinCallContext$2.invokeSuspend(HttpClientEngine.kt:85)
 * at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
 * at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:106)
 * at kotlinx.coroutines.scheduling.CoroutineScheduler.runSafely(CoroutineScheduler.kt:571)
 * at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.executeTask(CoroutineScheduler.kt:750)
 * at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runWorker(CoroutineScheduler.kt:678)
 * at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:665)
 *
 */
@SpringBootTest
@Testcontainers
class DynamoDbAwsKotlinIT {

    @Container
    val localStack: LocalStackContainer =
        LocalStackContainer(DockerImageName.parse("localstack/localstack:0.14.2"))
            .withServices(
                LocalStackContainer.Service.DYNAMODB,
            )

    @Test
    fun crud() {
        println(localStack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB)) // http://127.0.0.1:52686
        assertThat(localStack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB)).isNotNull

        // use kotlin library
        // aws.sdk.kotlin:dynamodb
        // aws.sdk.kotlin:dynamodb-streams  probably not what we want: https://aws.amazon.com/blogs/database/dynamodb-streams-use-cases-and-design-patterns/
        // https://docs.aws.amazon.com/sdk-for-kotlin/latest/developer-guide/home.html
        // https://docs.aws.amazon.com/sdk-for-kotlin/latest/developer-guide/get-started.html#get-started-code
        //   WIP...
        // https://docs.aws.amazon.com/sdk-for-kotlin/latest/developer-guide/kotlin_code_examples_categorized.html
        // https://blog.jetbrains.com/kotlin/2022/01/the-new-aws-sdk-for-kotlin-with-coroutines-support/
        // https://docs.aws.amazon.com/sdk-for-kotlin/latest/developer-guide/home.html
        //
        // software:amazon:awssdk:dynamodb : offers software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

        // aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
        //   aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
        // com.amazonaws.auth.AWSStaticCredentialsProvider.AWSStaticCredentialsProvider

        // API for DynamoDbClient is horrible for setup in kotlin. https://stackoverflow.com/questions/33552299/how-to-create-an-anonymous-implementation-of-an-interface
        // aws.smithy.kotlin.runtime.http.Url contains alarning TODOs

        val endpointResolverInstance = object : AwsEndpointResolver {
            override suspend fun resolve(service: String, region: String): AwsEndpoint {
                // Note: anything but dynamodb we cannot serve

                println("requesting endpoint for service: ${service.uppercase()}")
                println(
                    try {
                        "got: " + LocalStackContainer.Service.valueOf(service.uppercase())
                    } catch (e: Exception) {
                        "exception : $e"
                    }
                )

                return when (LocalStackContainer.Service.valueOf(service.uppercase())) {
                    LocalStackContainer.Service.DYNAMODB -> localStack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB)
                        ?.let {
                            val url: Url = Url.parse(it.toString())
                            return AwsEndpoint(url)
                        }
                        ?: throw IllegalArgumentException("'$service' is not a service provided by local stack setup")
                    else -> throw IllegalArgumentException("'$service' is not a service provided by local stack setup")
                }
            }
        }



        val dynamoDbClient = DynamoDbClient {
            // https://kotlinlang.org/docs/fun-interfaces.html#functional-interfaces-vs-type-aliases
            credentialsProvider = object: CredentialsProvider {
                override suspend fun getCredentials() = Credentials(localStack.accessKey, localStack.secretKey)
            }
            region = localStack.region
            // aws.sdk.kotlin.runtime.endpoint.AwsEndpointResolver
            endpointResolver = endpointResolverInstance
        }

        // create table
        // https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/best-practices.html
        // https://docs.aws.amazon.com/sdk-for-kotlin/latest/developer-guide/get-started.html#get-started-code

        val newTable = "TestSDK" + Instant.now().epochMilliseconds
        val key = "key"

        runBlocking {
            println(endpointResolverInstance.resolve("dynamodb", localStack.region))

            try {

                println("tutorialSetup ...")

                tutorialSetup(dynamoDbClient, newTable, key)

                println("Writing to table...")

                dynamoDbClient.putItem {
                    tableName = newTable
                    item = mapOf(
                        key to AttributeValue.S("${key}_value"),
                        "propertyA" to AttributeValue.S("propertyA-value")
                    )
                }

                println("Completed writing to table.")
                println()

//                println("Reading from table...")
//                val getItemResponse: GetItemResponse = dynamoDbClient.getItem {
//                    this.tableName = newTable
//                    this.key = mapOf(
//                        key to AttributeValue.S("${key}_value"),
//                    )
//                }
//
//                println("Completed reading to table. Value: ${getItemResponse?.item?.get("propertyA")}")
//                println()

                cleanUp(dynamoDbClient, newTable)

            } catch (e: DynamoDbException) {
                println("ERROR (DynamoDbException): " + e.message)
            }
            catch (e: Exception) { // does not exist in "aws.sdk.kotlin:dynamodb:0.15.0"
                println("ERROR (some Exception): " + e.message)
            }
//            catch (e: UnknownServiceErrorException) { // does not exist in "aws.sdk.kotlin:dynamodb:0.15.0"
//                println("ERROR (UnknownServiceErrorException): " + e.message)
//            }
            finally {
                println("closing dynamoDbClient...")
                dynamoDbClient.close()
                println("dynamoDbClient closed")
            }
            println("Exiting...")
        }

        // create
        // read
        // update
        // read
        // delete
        // read

        // delete table
    }

    private suspend fun tutorialSetup(dynamoDbClient: DynamoDbClient, newTable: String, key: String) {

        val createTableRequest = CreateTableRequest {
            tableName = newTable
            attributeDefinitions = listOf(
                AttributeDefinition {
                    attributeName = key
                    attributeType = ScalarAttributeType.S
                }
            )
            keySchema = listOf(
                KeySchemaElement {
                    attributeName = key
                    keyType = KeyType.Hash
                }
            )
            provisionedThroughput {
                readCapacityUnits = 10
                writeCapacityUnits = 10
            }
        }
        println("Creating table: $newTable...")
        dynamoDbClient.createTable(createTableRequest)
        println("Waiting for table to be active...")
        var tableIsActive = dynamoDbClient.describeTable {
            tableName = newTable
        }.table?.tableStatus == TableStatus.Active
        do {
            if (!tableIsActive) {
                delay(500)
                tableIsActive = dynamoDbClient.describeTable {
                    tableName = newTable
                }.table?.tableStatus == TableStatus.Active
            }
        } while(!tableIsActive)
        println("$newTable is ready.")
        println()
    }

    private suspend fun cleanUp(dynamoDbClient: DynamoDbClient, newTable: String) {
        println("Cleaning up...")
        println("Deleting table: $newTable...")
        dynamoDbClient.deleteTable {
            tableName = newTable
        }
        println("$newTable has been deleted.")
        println()
        println("Cleanup complete")
        println()
    }
}
