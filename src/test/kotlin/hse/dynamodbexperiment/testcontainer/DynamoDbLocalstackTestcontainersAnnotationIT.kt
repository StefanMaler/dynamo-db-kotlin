package hse.dynamodbexperiment.testcontainer

import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName


/**
 * following: https://github.com/spring-cloud/spring-cloud-stream/blob/main/binders/kinesis-binder/spring-cloud-stream-binder-kinesis/src/test/java/org/springframework/cloud/stream/binder/kinesis/LocalstackContainerTest.java
 *
 * using testcontainers annotations should manage the container (no need to start and stop)
 */
@SpringBootTest
@Testcontainers
class DynamoDbLocalstackTestcontainersAnnotationIT {

    @Container
    val localStack =
        LocalStackContainer(DockerImageName.parse("localstack/localstack:0.14.2"))
            .withServices(
                LocalStackContainer.Service.DYNAMODB,
            )

    @Test
    fun contextLoads() {
        println(localStack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB)) // http://127.0.0.1:52686
        assertThat(localStack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB)).isNotNull
    }
}
