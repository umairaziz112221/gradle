version = "1.0"

configurations {
    create("wars") {
        attributes.attribute(Attribute.of("type", String::class.java), "war")
    }
}

dependencies {
    "wars"(project("date"))
    "wars"(project("hello"))
}

tasks.register<Copy>("explodedDist") {
    from(configurations["wars"])
    into("$buildDir/explodedDist")
}
