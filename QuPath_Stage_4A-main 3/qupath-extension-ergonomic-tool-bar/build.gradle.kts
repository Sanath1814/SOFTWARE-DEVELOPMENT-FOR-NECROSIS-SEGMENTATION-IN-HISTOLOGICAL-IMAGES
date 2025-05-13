plugins {
    id("com.gradleup.shadow") version "8.3.5"
    id("qupath-conventions")
}

qupathExtension {
    name = "qupath-ergonomic-tool-bar"
    group = "io.github.qupath"
    version = "0.1.0-SNAPSHOT"
    description = "An ergonomic toolbar for QuPath that makes it easier to access common tools"
    automaticModule = "io.github.qupath.extension.template"
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.scijava.org/content/repositories/releases/") }
    maven { url = uri("https://mvnrepository.com/artifact/io.github.qupath/qupath-core") }
    maven { url = uri("https://mvnrepository.com/artifact/io.github.qupath/") }
    maven { url = uri("https://mvnrepository.com/artifact/org.slf4j/") }
    maven { url = uri("https://mvnrepository.com/artifact/org.locationtech.jts/") }
    maven { url = uri("https://mvnrepository.com/artifact/org.openjfx/") }
    maven { url = uri("https://repo.maven.apache.org/maven2") }
}
dependencies {
    implementation("org.locationtech.jts:jts-core:1.20.0") // JTS Topology Suite
    implementation("org.slf4j:slf4j-api:2.0.16") // SLF4J API
    implementation("io.github.qupath:qupath-core:0.6.0-rc1")
    implementation("io.github.qupath:qupath-extension-processing:0.6.0-rc1")

    shadow(libs.bundles.qupath)
    shadow(libs.bundles.logging)
    shadow(libs.qupath.fxtras)

    testImplementation(libs.bundles.qupath)
    testImplementation(libs.junit)
}

buildscript {
    repositories {
        mavenCentral()
        maven { url = uri("https://maven.scijava.org/content/repositories/releases/") }
        maven { url = uri("https://mvnrepository.com/artifact/io.github.qupath/qupath-core") }
        maven { url = uri("https://mvnrepository.com/artifact/io.github.qupath/") }
        maven { url = uri("https://mvnrepository.com/artifact/org.slf4j/") }
        maven { url = uri("https://mvnrepository.com/artifact/org.locationtech.jts/") }
        maven { url = uri("https://mvnrepository.com/artifact/org.openjfx/") }
    }
    dependencies {
        classpath("com.gradleup.shadow:shadow-gradle-plugin:9.0.0-beta8")
        classpath("io.github.qupath:qupath-core:0.6.0-rc1")
        classpath("org.locationtech.jts:jts-core:1.20.0") // JTS Topology Suite
        classpath("org.slf4j:slf4j-api:2.0.16") // SLF4J API
        classpath("io.github.qupath:qupath-extension-processing:0.6.0-rc1")
    }
}

apply(plugin = "java")
apply(plugin = "com.gradleup.shadow")