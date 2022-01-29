import com.modrinth.minotaur.TaskModrinthUpload
import com.modrinth.minotaur.request.VersionType
import org.jetbrains.changelog.date
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("fabric-loom")

    val kotlinVersion: String by System.getProperties()
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("org.jetbrains.changelog") version "1.3.1"
    id("com.modrinth.minotaur") version "1.2.1"
}

base {
    val archivesBaseName: String by project
    archivesName.set(archivesBaseName)
}

val env = System.getenv()
val minecraftVersion: String by project

val baseVersion = env["MOD_VERSION"] ?: "unreleased"
version = "$baseVersion+mc$minecraftVersion"

val mavenGroup: String by project
group = mavenGroup

changelog {
    // cf. https://github.com/JetBrains/gradle-changelog-plugin
    version.set(baseVersion)
    header.set(provider { "[${version.get()}] - ${date()}" })
    itemPrefix.set("*")
}

val versionChangelog by lazy { changelog.getOrNull(baseVersion) ?: changelog.getLatest() }

minecraft {}

repositories {
    maven {
        name = "CottonMC"
        url = uri("https://server.bbkr.space/artifactory/libs-release")
    }

    dependencies {
        minecraft("com.mojang:minecraft:${minecraftVersion}")

        val yarnMappings: String by project
        mappings("net.fabricmc:yarn:$yarnMappings:v2")

        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

        val loaderVersion: String by project
        modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

        val fabricVersion: String by project
        modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")

        val fabricKotlinVersion: String by project
        modImplementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")

        val libGuiVersion: String by project
        modImplementation("io.github.cottonmc:LibGui:$libGuiVersion")
        include("io.github.cottonmc:LibGui:$libGuiVersion")
    }
}

loom {
    // cf. https://github.com/FabricMC/fabric-loom

    runs {
        getByName("client").apply {
            property("fabric.log.level", "info")
            property("fabric.log.debug.level", "debug")
        }
    }
}

task<TaskModrinthUpload>("modrinth") {
    // cf. https://github.com/modrinth/minotaur

    group = "publishing"
    onlyIf { "MODRINTH_TOKEN" in env.keys }
    dependsOn("build")

    val modrinthProjectId: String by project
    projectId = modrinthProjectId

    token = env["MODRINTH_TOKEN"]
    uploadFile = tasks["remapJar"]
    changelog = versionChangelog.toText()

    versionNumber = version.toString()
    versionName = baseVersion

    val releaseType by lazy {
        when {
            "-alpha" in baseVersion -> VersionType.ALPHA
            "-beta" in baseVersion  -> VersionType.BETA
            else                    -> VersionType.RELEASE
        }
    }
    versionType = releaseType

    addGameVersion(minecraftVersion)
    addLoader("fabric")
}

tasks {
    val javaVersion = JavaVersion.VERSION_17

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        options.release.set(javaVersion.toString().toInt())
    }

    withType<KotlinCompile> {
        kotlinOptions { jvmTarget = javaVersion.toString() }
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
    }

    jar {
        val archivesBaseName: String by project
        from("LICENSE") { rename { "${it}_${archivesBaseName}" } }
        from("LICENSE.md") { rename { "${it}_${archivesBaseName}.md" } }
    }

    processResources {
        inputs.property("version", project.version)
        fun prop(name: String) = name to project.property(name)
        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "version" to project.version,
                    prop("minecraftVersion"),
                    prop("loaderVersion"),
                    prop("fabricVersion"),
                    prop("fabricKotlinVersion"),
                    prop("libGuiVersion")
                )
            )
        }
    }

    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toString())) }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        withSourcesJar()
    }
}
