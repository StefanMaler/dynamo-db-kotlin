package hse.dynamodbexperiment.testcontainer

import java.net.URI
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.core.env.get
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName

/**
 * following: https://rieckpil.de/testing-spring-boot-applications-with-kotlin-and-testcontainers/
 *
 * potential issue: container is hard to shut down
 */
@SpringBootTest
class DynamoDbLocalstackCompanionObjectIT {

    companion object {
        // TODO: consider shutdown of testcontainer
        val localStack = LocalStackContainer(DockerImageName.parse("localstack/localstack:0.14.2"))
            .withServices(
                LocalStackContainer.Service.DYNAMODB,
            ).apply {
                start()
            }

        @JvmStatic
        @DynamicPropertySource // @DynamicPropertySource has to be static, so it cannot access @Container on members (not sure if we can use that annotation on static
        fun properties(registry: DynamicPropertyRegistry) {
            val endpointOverride: URI = localStack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB)
            println(endpointOverride)
            registry.add("dynamodb.url") { endpointOverride.toString() }
        }
    }

    @Value("\${dynamodb.url}")
    lateinit var dynamoDbUrl: String

    @Autowired
    lateinit var context : ApplicationContext

    @Test
    fun containerRunsAndWeGetData() {
        println(context.environment.get("dynamodb.url"))

        assertThat(context.environment.get("dynamodb.url")).isNotEmpty
        assertThat(dynamoDbUrl).isNotEmpty
    }
}
