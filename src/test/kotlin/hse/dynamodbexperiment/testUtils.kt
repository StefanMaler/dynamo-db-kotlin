package hse.dynamodbexperiment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import java.io.File
import java.net.URI
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

private val om by lazy {
    jacksonMapperBuilder()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .findAndAddModules() // registers eg java 8 datatime module when com.fasterxml.jackson.datatype:jackson-datatype-jdk8 is on the classpath
        .build()
}

fun Any?.toJson(): String? {
    if (this == null) return null
    return om.writeValueAsString(this)
}

fun fileContents(path: String) = File(path).bufferedReader().readText()

// uses software.amazon.awssdk:cloudformation:2.17.191
fun cloudFormationAsyncClientFor(localStackContainer: LocalStackContainer) : CloudFormationAsyncClient {
    val region = Region.of(localStackContainer.region)
    val cloudformationEndpoint =
        localStackContainer.getEndpointOverride(LocalStackContainer.Service.CLOUDFORMATION)
    val credentialsProvider: () -> AwsCredentials = {
        AwsBasicCredentials.create(
            localStackContainer.accessKey,
            localStackContainer.secretKey
        )
    }
    return CloudFormationAsyncClient.builder()
        .region(region)
        .endpointOverride(cloudformationEndpoint)
        .credentialsProvider(credentialsProvider)
        .build()
}

// uses software.amazon.awssdk:cloudformation:2.17.191
fun dynamoDbAsyncClientFor(localStackContainer: LocalStackContainer): DynamoDbAsyncClient {
    val region = Region.of(localStackContainer.region)
    val endpoint = localStackContainer.getEndpointOverride(LocalStackContainer.Service.DYNAMODB)
    val credentialsProvider: () -> AwsCredentials = {
        AwsBasicCredentials.create(
            localStackContainer.accessKey,
            localStackContainer.secretKey
        )
    }
    return DynamoDbAsyncClient.builder()
        .credentialsProvider(credentialsProvider)
        .region(region)
        .endpointOverride(endpoint)
        .build()
}


val credentialsProviderManual: () -> AwsCredentials = {
    AwsBasicCredentials.create(
        "test",
        "test"
    )
}

// Preconfigured clients to run tests against a localstack container started via docker compose (see README.md)

val dynamoDbAsyncClientManual = DynamoDbAsyncClient.builder()
    .credentialsProvider(credentialsProviderManual)
    .region(Region.EU_CENTRAL_1)
    .endpointOverride(URI.create("http://localhost:4566"))
    .build()
val cloudFormationAsyncClientManual = CloudFormationAsyncClient.builder()
    .region(Region.EU_CENTRAL_1)
    .endpointOverride(URI.create("http://localhost:4566"))
    .credentialsProvider(credentialsProviderManual)
    .build()
val dynamoDbEnhancedAsyncClientManual =
    DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(dynamoDbAsyncClientManual).build()
