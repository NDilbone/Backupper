plugins {
    id("java")
    id("application")
}

group = "com.github"
version = "1.0.0"

// Configure Java compatibility
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.0")
    implementation("ch.qos.logback:logback-classic:1.4.12")
    implementation("org.slf4j:slf4j-api:2.0.7")
}

// Ensure the correct main class is used
application {
    mainClass.set("backupper.StartBackup")
}

// Fat JAR (Uber JAR) Task
tasks.register<Jar>("fatJar") {
    archiveBaseName.set("Backupper")
    archiveVersion.set("") // Remove a version from JAR name
    manifest {
        attributes["Main-Class"] = "backupper.StartBackup"
    }
    from(sourceSets.main.get().output) {
        // Exclude config files from being packaged in the JAR
        exclude("config.json", "example.config.json")
    }

    dependsOn(configurations.runtimeClasspath)

    // Handle duplicate files properly
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Include dependencies inside the JAR
    from({
        configurations.runtimeClasspath.get().filter { it.exists() }.map {
            if (it.isDirectory) it else zipTree(it)
        }
    })
}

// Compile options
tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

// Make the `build` task create a Fat JAR
tasks.build {
    dependsOn(tasks.named("fatJar"))
}