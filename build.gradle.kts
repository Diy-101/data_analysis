plugins {
    `java-library`
    `maven-publish`
    java
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    api(libs.org.springframework.boot.spring.boot.starter.webmvc)
    api(libs.com.fasterxml.jackson.core.jackson.databind)
    api(libs.com.google.genai.google.genai)
    api(libs.org.springframework.boot.spring.boot.starter.actuator)
    api(libs.org.ta4j.ta4j.core)
    runtimeOnly(libs.org.springframework.boot.spring.boot.devtools)
    testImplementation(libs.org.springframework.boot.spring.boot.starter.webmvc.test)
}

group = "com.java"
version = "0.0.1-SNAPSHOT"
description = "tradingBot"

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(26))
    }
}
