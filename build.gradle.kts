group = "com.joutak"
version = System.getProperty("version")
val commitHash = System.getProperty("commitHash")
if (commitHash.isNotBlank()) {
    version = "$version-$commitHash"
}

val targetJavaVersion = 17
plugins {
    kotlin("jvm") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://repo.onarandombox.com/content/groups/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("com.onarandombox.multiversecore:multiverse-core:4.3.14")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib")
}


kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.shadowJar {
    archiveClassifier = ""
    archiveFileName.set("${project.name}.jar")
    if (System.getenv("TESTING") != null) {
        destinationDirectory.set(file("C:\\Users\\Lenovo\\Desktop\\server\\plugins"))
    }
}

//tasks.shadowJar {
//    archiveClassifier = ""
//    archiveFileName.set("${project.name}.jar")
//
//    val serverPath = System.getenv("SERVER_PATH")
//    if (System.getenv("TESTING") != null) {
//        if (serverPath != null) destinationDirectory.set(file("$serverPath\\plugins"))
//        else logger.warn("SERVER_PATH property is not set!")
//    }
//}
// idk why not working for me but it should tho

tasks.jar {
    finalizedBy("shadowJar")
    enabled = false
}

tasks.processResources {
    val props = mapOf("version" to version, "pluginName" to project.name)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}