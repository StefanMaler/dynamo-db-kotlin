package hse.dynamodbexperiment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DynamoDbExperimentApplication

fun main(args: Array<String>) {
    runApplication<DynamoDbExperimentApplication>(*args)
}
