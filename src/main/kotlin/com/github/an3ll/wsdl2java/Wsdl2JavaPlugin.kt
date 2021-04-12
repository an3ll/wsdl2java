package com.github.an3ll.wsdl2java

import no.nils.wsdl2java.Wsdl2JavaPlugin
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import java.io.File

class Wsdl2JavaPlugin : Wsdl2JavaPlugin() {

    private val wsdl2java = "wsdl2java"
    private val java9Dependencies = listOf(
        "javax.xml.bind:jaxb-api:2.3.1",
        "javax.xml.ws:jaxws-api:2.3.1",
        "org.glassfish.jaxb:jaxb-runtime:2.3.2",
        "org.glassfish.main.javaee-api:javax.jws:3.1.2.2",
        "com.sun.xml.messaging.saaj:saaj-impl:1.5.1"
    )

    private val destinationDir = "build/generated/wsdl"

    override fun apply(project: Project) {
        project.plugins.apply("java")

        val extension = project.extensions.create(wsdl2java, no.nils.wsdl2java.Wsdl2JavaPluginExtension::class.java)
        val cxfVersion = project.provider { extension.cxfVersion }

        // Add new configuration for our plugin and add required dependencies to it later.
        val wsdl2javaConfiguration = project.configurations.maybeCreate(wsdl2java)

        // Get compile configuration and add Java 9+ dependencies if required.
        project.configurations.named("implementation").configure { configuration ->
            configuration.withDependencies { dependencySet ->
                if (JavaVersion.current().isJava9Compatible) {
                    java9Dependencies.map { dependency ->
                        dependencySet.add(project.dependencies.create(dependency))
                    }
                }
            }
        }

        val wsdl2JavaTask = project.tasks.register(wsdl2java, no.nils.wsdl2java.Wsdl2JavaTask::class.java) { task ->
            wsdl2javaConfiguration.withDependencies { dependencySet ->
                dependencySet.add(project.dependencies.create("org.apache.cxf:cxf-tools-wsdlto-databinding-jaxb:${cxfVersion.get()}"))
                dependencySet.add(project.dependencies.create("org.apache.cxf:cxf-tools-wsdlto-frontend-jaxws:${cxfVersion.get()}"))
                dependencySet.add(project.dependencies.create("org.apache.cxf.xjcplugins:cxf-xjc-ts:${cxfVersion.get()}"))
                dependencySet.add(project.dependencies.create("org.apache.cxf.xjcplugins:cxf-xjc-boolean:${cxfVersion.get()}"))

                if (JavaVersion.current().isJava9Compatible) {
                    java9Dependencies.map { dependency -> dependencySet.add(project.dependencies.create(dependency)) }
                }
            }

            task.group = "Wsdl2Java"
            task.description = "Generate java source code from WSDL files."
            task.classpath = wsdl2javaConfiguration
        }

        project.tasks.named("compileJava").configure { task ->
            task.dependsOn(wsdl2JavaTask)
        }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            project.tasks.withType(getTaskClass("org.jetbrains.kotlin.gradle.tasks.KotlinCompile")).configureEach { task ->
                task.dependsOn(wsdl2JavaTask)
            }
        }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.kapt") {
            project.tasks.withType(getTaskClass("org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask"))
                .configureEach { task ->
                    task.dependsOn(wsdl2JavaTask)
                }
        }

        val sourceSetContainer = project.extensions.getByType(SourceSetContainer::class.java)
        sourceSetContainer.named("main").get().java.srcDirs.add(File(destinationDir))
    }
}
