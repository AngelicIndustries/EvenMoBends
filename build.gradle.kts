plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
    testRuntimeOnly("org.lwjgl.lwjgl:lwjgl_util:2.9.4-nightly-20150209")
}

tasks.test {
    useJUnitPlatform()
}

apply(from = "https://raw.githubusercontent.com/AngelicIndustries/67minecraft-gradle-publish/${property("publish67ScriptTag")}/67minecraft-publish.gradle.kts")

tasks.withType<Jar>().configureEach {
    from("LICENSE") {
        into("META-INF")
        rename { "EVENMOBENDS-LICENSE.txt" }
    }
    from("LICENSES") {
        into("META-INF/LICENSES")
    }
    from("UPSTREAM.md") {
        into("META-INF")
        rename { "EVENMOBENDS-UPSTREAM.md" }
    }
}
