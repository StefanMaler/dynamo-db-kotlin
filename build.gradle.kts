import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.6.7"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
}

group = "hse"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.17.1"))
    testImplementation("org.testcontainers:localstack")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("com.amazonaws:aws-java-sdk-core:1.12.213") // is not managed although localstack is; so versions might be incompatible
    // without aws-java-sdk-core dependency, during local stack test container startup:
    //   java.lang.NoClassDefFoundError: com/amazonaws/auth/AWSCredentials; add dependency by trial and error:
    //    https://github.com/testcontainers/testcontainers-java/blob/master/modules/localstack/build.gradle shows only compile and test dependencies, including com.amazonaws:aws-java-sdk-s3:1.12.191
    // testImplementation("aws.sdk.kotlin:aws-core:0.15.0") CHECKED (FAIL problem persists); added via alt-insert
    // testImplementation("com.amazonaws:aws-java-sdk-core:1.12.213") via alt-insert "com.amazonaws:aws-java-sdk-core" CHECKED works
    implementation(platform("software.amazon.awssdk:bom:2.17.191"))
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:cloudformation")
    testImplementation("software.amazon.awssdk:dynamodb-enhanced") // intellij shows (alt-insert): 2.17.181, mvn central shows software.amazon.awssdk:dynamodb-enhanced:2.17.191
    implementation("aws.sdk.kotlin:dynamodb:0.15.0") // not managed by software.amazon.awssdk:bom
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.1")
    // TODO: consider spring-data-dynamodb https://github.com/boostchicken/spring-data-dynamodb, https://www.baeldung.com/spring-data-dynamodb
    //  note: last release from Jun 17 2020; open issues include missing reactive support: https://github.com/boostchicken/spring-data-dynamodb/issues/404
    // testImplementation("io.github.boostchicken:spring-data-dynamodb:5.2.5")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
