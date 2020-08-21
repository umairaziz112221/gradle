plugins {
    `java`
    `war`
}

group = "org.gradle.sample"
version = rootProject.version

repositories {
    mavenCentral()
}

configurations {
    register("wars") {
        attributes.attribute(Attribute.of("type", String::class.java), "war")
        outgoing.artifact(tasks.war)
        isCanBeResolved = false
        isCanBeConsumed = true
    }
}

dependencies {
    implementation("javax.servlet:servlet-api:2.5")
}
