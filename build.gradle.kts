plugins {
    java
    id ("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "de.blablubbabc"
version = "2.3.3"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.helpch.at/releases/")
    maven("https://repo.rosewooddev.io/repository/public/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("net.milkbowl.vault:VaultAPI:1.7")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.dmulloy2:ProtocolLib:5.2.0")
    compileOnly("org.jetbrains:annotations:24.0.1")

    implementation("commons-lang:commons-lang:2.6")
    implementation("com.github.MrXiaoM:holoeasy:3.4.3-1")
    implementation("com.github.technicallycoded:FoliaLib:0.4.4")
}
tasks {
    shadowJar {
        archiveClassifier.set("")
        mapOf(
            "org.apache.commons" to "commons",
            "org.holoeasy" to "holoeasy",
            "com.tcoded.folialib" to "folialib",
        ).forEach { (original, target) ->
            relocate(original, "de.blablubbabc.billboards.util.$target")
        }
    }
    build {
        dependsOn(shadowJar)
    }
    withType<JavaCompile>().configureEach {
        options.encoding = "utf-8"
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(sourceSets.main.get().resources.srcDirs) {
            expand(mapOf("version" to version))
            include("plugin.yml")
        }
    }
}
