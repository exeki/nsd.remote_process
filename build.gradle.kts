import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("maven-publish")
    kotlin("jvm") version "1.9.22"
    id("groovy")
    id("java-library")
    id("org.jetbrains.dokka") version "1.9.10"

}

group = "ru.kazantsev.nsd"
version = "1.0.2"

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

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/exeki/nsd.remote_process")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

dependencies {
    api("ru.kazantsev.nsd:basic_api_connector:1.0.4")
    api("ru.kazantsev.nsd:json_rpc_connector:1.1.1")
    implementation("org.slf4j:slf4j-api:2.0.9")
    api("org.apache.poi:poi-ooxml:5.3.0")
    testImplementation(kotlin("test"))
    testImplementation("org.codehaus.groovy:groovy-all:3.0.19")
}

tasks {

    test {
        useJUnitPlatform()
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }

    javadoc {
        dependsOn(dokkaJavadoc)
    }

    dokkaJavadoc {
        outputDirectory.set(buildDir.resolve("docs\\javadoc"))
    }

    compileJava {
        targetCompatibility = "11"
    }

    register<Jar>("javadocJar") {
        from(getByName("javadoc").outputs.files)
        archiveClassifier.set("javadoc")
    }

    register<Jar>("sourcesJar") {
        from(sourceSets.main.get().allSource)
        archiveClassifier.set("sources")
    }
}
