plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "com.github.labai"
version = "0.0.2.dev1"

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}


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
