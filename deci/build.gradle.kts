plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "com.github.labai"
version = "2.0.0"

kotlin {
    jvm()
    js(IR) {
        browser()
        nodejs()
        binaries.library()
    }

    iosX64()

    sourceSets {
        commonMain.dependencies {
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jvmTest.dependencies {
            implementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
            implementation("org.junit.jupiter:junit-jupiter-params:5.5.2")

            // expected better performance, but is slower than native jdk...
            implementation("ch.randelshofer:fastdoubleparser:2.0.1")
        }

        jsMain.dependencies {
            implementation(npm("decimal.js", "10.6.0"))
        }

        jsTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
// need to set up rules
// apply(plugin = "org.jlleitschuh.gradle.ktlint")

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "deci", version.toString())

    pom {
        name = "deci"
        description = "Deci - decimals class for kotlin"
        inceptionYear = "2020"
        url = "https://github.com/labai/deci/"

        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
            }
        }

        developers {
            developer {
                id = "labai"
                name = "Augustus"
                email = "augis7@gmail.com"
            }
        }

        scm {
            connection = "scm:git:https://github.com/labai/deci.git"
            developerConnection = "scm:git:ssh://github.com:labai/deci.git"
            url = "https://github.com/labai/deci"
        }
    }
}
