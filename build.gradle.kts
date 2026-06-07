import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `maven-publish`
    `java-library`
    alias(libs.plugins.kotlin)
    alias(libs.plugins.grgit)
    alias(libs.plugins.shadow)
}

val devMode = grgit.branch.current().name != "master" && grgit.branch.current().name != "HEAD"

allprojects {
    group = "info.preva1l.trashcan"
    version = "1.2.3"
    repositories {
        mavenCentral()
        maven("https://repo.sunnyinfra.cloud/public")
        configureSlneReleasesRepository(credentials = false)
    }
}

val parentProjects = arrayOf("fabric")

subprojects {
    if (parentProjects.contains(project.name)) return@subprojects

    apply(plugin = "maven-publish")
    apply(plugin = "java-library")
    apply(plugin = rootProject.libs.plugins.shadow.get().pluginId)

    if (parentProjects.contains(project.parent?.name)) {
        version = version as String + "-${project.name}"

        if (project.parent?.name?.equals("fabric") == true) {
            //apply(plugin = "fabric-loom")
        }
    }

    tasks {
        withType<ShadowJar> {
            exclude("META-INF/maven/**", "org/**", "paper-plugin.yml")
            archiveClassifier.set("")
        }

        withType<JavaCompile> {
            options.compilerArgs.add("-parameters")
            options.isFork = true
            options.encoding = "UTF-8"
        }

        register<Jar>("sourcesJar") {
            archiveClassifier.set("sources")
            from(sourceSets.main.get().allSource)
        }

        withType<Javadoc> {
            (options as StandardJavadocDocletOptions).tags(
                "apiNote:a:API Note:",
                "implSpec:a:Implementation Requirements:",
                "implNote:a:Implementation Note:"
            )
        }

        register<Jar>("javadocJar") {
            dependsOn("javadoc")
            archiveClassifier.set("javadoc")
            from(named<Javadoc>("javadoc").get().destinationDir)
        }

        named("build") {
            dependsOn("shadowJar")
        }
    }

    publishing {
        repositories {
            configureSlneReleasesRepository(credentials = true)
        }
        publications {
            register<MavenPublication>("mavenJava") {
                from(components["java"])
                artifact(tasks.named("sourcesJar"))
                artifact(tasks.named("javadocJar"))
            }
        }
    }
}

fun RepositoryHandler.configureSlneReleasesRepository(credentials: Boolean = true)
{
    maven("https://reposilite.slne.dev/releases/") {
        name = "slne-repository-releases"

        if (credentials) {
            val user: String? = properties["slne_releases_repo_username"]?.toString()
                ?: System.getenv("SLNE_RELEASES_REPO_USERNAME")
            val pass: String? = properties["slne_releases_repo_password"]?.toString()
                ?: System.getenv("SLNE_RELEASES_REPO_PASSWORD")

            credentials {
                username = user
                password = pass
            }
        }
    }
}

logger.lifecycle("Building Trashcan $version")
