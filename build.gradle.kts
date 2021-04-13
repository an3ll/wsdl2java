import org.gradle.kotlin.dsl.accessors.runtime.conventionOf
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    groovy
    kotlin("jvm") version "1.5.0-M2"
    `maven-publish`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.14.0"
}

group = "com.github.an3ll"
version = "0.14"

repositories {
    mavenCentral()
}

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of("11"))
    }
}

dependencies {
    implementation(localGroovy())
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.0-M2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.5.2")
    testImplementation(gradleTestKit())

    runtimeOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.61")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")
}

gradlePlugin {
    plugins {
        create("wsdl2java") {
            id = "com.github.an3ll.wsdl2java"
            displayName = "Fork of https://github.com/nilsmagnus/wsdl2java Plugin for gradle 7.0"
            description = "Generate java classes using wsdl2java and cxf"
            implementationClass = "com.github.an3ll.wsdl2java.Wsdl2JavaPlugin"
        }
    }
}

pluginBundle {
    vcsUrl = "https://github.com/an3ll/wsdl2java"
    website = "https://github.com/an3ll/wsdl2java"
    tags = listOf("wsdl2java", "cxf")
}

tasks {
    test {
        useJUnitPlatform()
    }

    named<GroovyCompile>("compileGroovy") {
        classpath = sourceSets.main.get().compileClasspath
    }

    named<KotlinCompile>("compileKotlin") {
        classpath += files(
            conventionOf(sourceSets.main.get())
                .getPlugin(GroovySourceSet::class.java).groovy.classesDirectory
        )
    }
}
