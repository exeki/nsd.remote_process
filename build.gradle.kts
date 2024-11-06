import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22"
    id("groovy")
    application
}

group = "ru.kazantsev.nsd"
version = "1.0.0"

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/exeki/*")
        credentials {
            username = System.getenv("GITHUB_USERNAME")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
    mavenCentral()
    mavenLocal()
}

dependencies {
    api("ru.kazantsev.nsd:basic_api_connector:1.0.4")
    api("ru.kazantsev.nsd:json_rpc_connector:1.1.1")
    implementation("org.slf4j:slf4j-api:2.0.9")
    api("org.apache.poi:poi-ooxml:5.3.0")
    testImplementation(kotlin("test"))
    testImplementation("org.codehaus.groovy:groovy-all:3.0.19")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}
