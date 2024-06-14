import gg.essential.gradle.multiversion.excludeKotlinDefaultImpls
import gg.essential.gradle.multiversion.mergePlatformSpecifics
import gg.essential.gradle.util.RelocationTransform.Companion.registerRelocationAttribute
import gg.essential.gradle.util.noServerRunConfigs
import gg.essential.gradle.util.prebundle

val mavenGroup: String by rootProject
val isMainProject = project.name == file("../mainProject").readText().trim()

plugins {
    kotlin("jvm")
    id("gg.essential.multi-version")
    id("gg.essential.defaults")
    id("su.plo.crowdin.plugin")
    id("su.plo.voice.relocate")
}

group = "$mavenGroup.client"
base.archivesName.set("plasmovoice-${platform.loaderStr}-${platform.mcVersionStr}")

loom.noServerRunConfigs()

if (platform.isForge) {
    loom.forge.apply {
        mixinConfig(
            "plasmovoice.mixins.json",
            "plasmovoice-forge.mixins.json"
        )
    }
}

loom.runs {
    getByName("client") {
        programArgs("--username", "GNOME__")
        property("plasmovoice.alpha.disableversioncheck", "true")
        property("universalcraft.shader.legacy.debug", "true")
    }
}

plasmoCrowdin {
    projectId = "plasmo-voice"
    sourceFileName = "client.json"
    resourceDir = "assets/plasmovoice/lang"
}

val shadowCommon by configurations.creating

repositories {
    maven("https://repo.essential.gg/repository/maven-public")
}

dependencies {
    compileOnly(rootProject.libs.netty)
    implementation(rootProject.libs.rnnoise)

    if (platform.isFabric) {
        val fabricApiVersion = when (platform.mcVersion) {
            11902 -> "0.73.2+1.19.2"
            11903 -> "0.73.2+1.19.3"
            11904 -> "0.76.0+1.19.4"
            12001 -> "0.84.0+1.20.1"
            12002 -> "0.89.1+1.20.2"
            12004 -> "0.97.0+1.20.4"
            12006 -> "0.97.8+1.20.6"
            12100 -> "0.100.1+1.21"
            else -> throw GradleException("Unsupported platform $platform")
        }

        modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricApiVersion}")
        "include"(modImplementation("me.lucko:fabric-permissions-api:0.2-SNAPSHOT")!!)
    }

    rootProject.libs.versions.universalcraft.map {
        "gg.essential:universalcraft-$platform:$it"
    }.also {
        modApi(it) {
            isTransitive = false
        }
        shadowCommon(it) {
            isTransitive = false
        }
    }

    val includedProjects = listOf(
        ":api:common",
        ":api:client",
        ":api:server-common",
        ":api:server",
        ":server:common",
        ":server-common",
        ":common",
        ":protocol"
    )

    includedProjects.forEach {
        implementation(project(it))
        shadowCommon(project(it)) {
            isTransitive = false
        }
    }

    // kotlin
    shadowCommon(kotlin("stdlib-jdk8"))
    shadowCommon(rootProject.libs.kotlinx.coroutines)
    shadowCommon(rootProject.libs.kotlinx.coroutines.jdk8)
    shadowCommon(rootProject.libs.kotlinx.json)

    shadowCommon(rootProject.libs.opus)
    shadowCommon(rootProject.libs.config)
    shadowCommon(rootProject.libs.rnnoise)
    shadowCommon(rootProject.libs.crowdin.lib) {
        isTransitive = false
    }

    "include"(rootProject.libs.guice)
    "include"(rootProject.libs.aopalliance)
    "include"(rootProject.libs.javax.inject)
}

tasks {
    processResources {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(plasmoCrowdinDownload)
    }

    jar {
        mergePlatformSpecifics()

        if (platform.mcVersion >= 11400) {
            excludeKotlinDefaultImpls()
        }
    }

    shadowJar {
        configurations = listOf(shadowCommon)

        relocate("su.plo.crowdin", "su.plo.voice.libs.crowdin")
        relocate("gg.essential.universal", "su.plo.voice.universal")

        dependencies {
            exclude(dependency("net.java.dev.jna:jna"))
            exclude(dependency("org.slf4j:slf4j-api"))
            exclude(dependency("org.jetbrains:annotations"))
            exclude(dependency("com.google.guava:.*"))

            exclude("README.md")

            if (platform.isForge) {
                exclude("fabric.mod.json")
            } else {
                exclude("plasmovoice-forge.mixins.json")
                exclude("pack.mcmeta")
                exclude("META-INF/mods.toml")
                exclude("DebugProbesKt.bin")
            }
        }
    }

    remapJar {
        dependsOn(shadowJar)
        input.set(shadowJar.get().archiveFile)
    }

    build {
        doLast {
            remapJar.get().archiveFile.get().asFile
                .copyTo(rootProject.buildDir.resolve("libs/${remapJar.get().archiveFile.get().asFile.name}"), true)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "${platform.loaderStr}-${platform.mcVersionStr}"
        }
    }
}
//dependencies {
//    implementation(project(":api:common"))
//    implementation(project(":api:client"))
//
//    implementation(project(":common"))
//    implementation(project(":protocol"))
//
//    compileOnly(rootProject.libs.netty)
//    implementation(rootProject.libs.config)
//    implementation(rootProject.libs.rnnoise)
//}

//apiValidation {
//    ignoredProjects.add(":client-common")
//}
