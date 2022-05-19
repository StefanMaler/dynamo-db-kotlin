package hse.dynamodbexperiment.testcontainer

import java.net.URI
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

/**
 * trying TestcontainersLocalstackDynamoDBTests with ApplicationContextInitializer
 * consider a base class (like we have for Postgres test that could offer a member variable to access
 * localstack, dynamoDbClient
 */
@SpringBootTest
@ContextConfiguration(initializers = [LocalStackInitializer::class])
class DynamoDbLocalstackInitializerIT {

    // intellij idea marks this been as not autowireable and it seems there is no way to mark this as ok
    @Autowired
    lateinit var dynamoDbEndpoint: URI

    @Test
    fun contextLoads() {
        println(dynamoDbEndpoint) // http://127.0.0.1:64992
        assertThat(dynamoDbEndpoint).isNotNull()
    }
}
