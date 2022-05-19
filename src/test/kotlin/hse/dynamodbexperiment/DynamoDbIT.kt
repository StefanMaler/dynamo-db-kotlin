package hse.dynamodbexperiment

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

/**
 * uses org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.1 to bridge CompletableFuture to kotlin's Deferred
 */
@Testcontainers
@OptIn(ExperimentalCoroutinesApi::class) // TODO: > This annotation should be used with the compiler argument '-opt-in=kotlin.RequiresOptIn'
class CleanDynamoDbIT {

    @Container
    val localStackContainer: LocalStackContainer =
        LocalStackContainer(DockerImageName.parse("localstack/localstack:0.14.2"))
            .withServices(
                LocalStackContainer.Service.DYNAMODB,
                LocalStackContainer.Service.CLOUDFORMATION,
            )

    /**
     * To run the test against a container started via docker compose (see README.md), use
     * [cloudFormationAsyncClientManual], [dynamoDbAsyncClientManual]
     */
    @Test
    fun crud() = runTest {
        // create table (via cloudformation)
        val cloudFormationAsyncClient = cloudFormationAsyncClientFor(localStackContainer)
//        val cloudFormationAsyncClient = cloudFormationAsyncClientManual
        cloudFormationAsyncClient.createStack {
            it.stackName("created-by-test")
            it.templateBody(fileContents("cloudformation/customer_table.yaml"))
        }.await()

        cloudFormationAsyncClient.waiter().waitUntilStackCreateComplete {
            it.stackName("created-by-test")
        }.await()

        val dynamoDbAsyncClient = dynamoDbAsyncClientFor(localStackContainer)
//        val dynamoDbAsyncClient = dynamoDbAsyncClientManual

        // uses software.amazon.awssdk:dynamodb-enhanced:2.17.191
        val dynamoDbEnhancedAsyncClient =
            DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(dynamoDbAsyncClient).build()

        // Note: table is created via cloudformation, the @DynamoDbBean class has to match
        val customerTable = dynamoDbEnhancedAsyncClient.table("Customer", TableSchema.fromBean(Customer::class.java))

        // create table
        // customerTable.createTable().await()
        // not needed: dynamoDbAsyncClient.waiter().waitUntilTableExists { it.tableName("Customer") }

        val listTablesResponse = dynamoDbAsyncClient.listTables().await()
        assertThat(listTablesResponse.tableNames()).containsExactly("Customer")

        // write
        val customer = Customer(
            id = "id146",
            custName = "Susan red",
            email = "sred@noserver.com",
            registrationDate = LocalDate.parse("2020-04-07").atStartOfDay().toInstant(ZoneOffset.UTC),
        )
        customerTable.putItem(customer).await()

        // read
        val customerFromTable = customerTable.getItem(Customer("id146")).await()
        assertThat(customerFromTable).isEqualTo(customer)

        // read collection
        val customerFlow: Flow<Customer> = customerTable.scan().items().asFlow()
        assertThat(customerFlow.first()).isEqualTo(customer)

        // delete table
        // customerTable.deleteTable().await()
    }
}

/**
 * Note:
 *  - id needs to be var; if not, on put item attempt: java.lang.IllegalArgumentException: Attempt to execute an operation that requires a primary index without defining any primary key attributes in the table metadata.
 *  - all fields but id need to be optional to allow query-by-example (get-item expects a parameter of the type that defines the table schema, not just its primary key)
 *  - all fields need to be writeable (*var* - if not they are silently skipped by get-item and retain their default value)
 *  - [attribute conversion](https://github.com/aws/aws-sdk-java-v2/tree/master/services-custom/dynamodb-enhanced#control-attribute-conversion) works out of the box for certain types (map, list)
 */
@DynamoDbBean
data class Customer(
    @get:DynamoDbPartitionKey // Hash; @DynamoDbSortKey for Sort/Range
    var id: String? = null,
    var custName: String? = null,
    var email: String? = null,
    var registrationDate: Instant? = null,
    var someNestedObjectMap: Map<String, String> = mapOf("street" to "home"),
    var someArray: List<String> = listOf("a", "b"),
    var someIntegerArray: List<Int> = listOf(1, 2),
)
