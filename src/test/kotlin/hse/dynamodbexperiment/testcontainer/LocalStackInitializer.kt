package hse.dynamodbexperiment.testcontainer

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import java.net.URI
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName

internal class LocalStackInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    override fun initialize(context: ConfigurableApplicationContext) {
        val localStackContainer: LocalStackContainer =
            LocalStackContainer(
                DockerImageName.parse("localstack/localstack:0.14.2") // latest version pushed by localstackci
            )
                .withServices(LocalStackContainer.Service.DYNAMODB)
                // consider .withReuse(true)

        localStackContainer.start()

        val credentialsProvider: AWSCredentialsProvider = AWSStaticCredentialsProvider(
            BasicAWSCredentials(
                localStackContainer.accessKey,
                localStackContainer.secretKey
            )
        )

        val dynamoDbEndpoint: URI = localStackContainer.getEndpointOverride(LocalStackContainer.Service.DYNAMODB)

        context.beanFactory.registerSingleton("localStackContainer", localStackContainer)
        context.beanFactory.registerSingleton("credentialsProvider", credentialsProvider)
        context.beanFactory.registerSingleton("dynamoDbEndpoint", dynamoDbEndpoint)

        // consider shutdown of testcontainer. Should be done automatically after a delay which might help with reruns/withReuse (or result in non-repeatable tests...):
        //  https://github.com/testcontainers/moby-ryuk
//        context.addApplicationListener(
//            ApplicationListener { _: ContextClosedEvent ->
//                localStackContainer.stop()
//            }
//        )
    }
}