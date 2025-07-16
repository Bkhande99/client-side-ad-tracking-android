import com.mobitv.client.versioning.VersionInfo

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10"
}

@Suppress("UnstableApiUsage")
android {
    namespace = "com.github.harmonicinc.clientsideadtracking"
    compileSdk = Constants.compileSdkVersion

    defaultConfig {
        minSdk = Constants.minSdkVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        //OMSDK Config
        buildConfigField("String", "PARTNER_NAME", "\"com.harmonicinc.omsdkdemo\"")
        buildConfigField("String", "VENDOR_KEY", "\"harmonic\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            enableUnitTestCoverage = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests.all {
            it.jvmArgs("-noverify")
        }
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    val coroutinesVersion = "1.7.3"
    val okhttpVersion = "4.12.0"

    implementation(project(":lib:lib"))

    // 3rd party libs
    implementation("com.google.android.gms:play-services-pal:20.1.1") {}
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.google.android.tv:tv-ads:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.2.1")

    // Android / Kotlin stdlibs
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.leanback:leanback:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.robolectric:robolectric:4.10.3")
    testImplementation("androidx.test:core-ktx:1.5.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("com.squareup.okhttp3:mockwebserver:$okhttpVersion")
    testImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

// Exclude OMSDK local aar (named "lib")
val excludedArtifact = setOf("lib")

val versionPropertyFile = "versions.properties"
val oldVersionPropertyFile = "oldversions.properties"

fun createVersionInfo(): VersionInfo {
    val versionPropertiesFile = file("${rootProject.projectDir}/$versionPropertyFile")
    return VersionInfo(versionPropertiesFile)
}


fun shouldIncrementVersion(): Boolean {
    try {
        if (project.hasProperty("IncreaseVersion")) {
            val value = project.properties["IncreaseVersion"]
            return value.toString().toBoolean()
        }
        return false
    } catch (e: Exception) {
        return false
    }
}

fun createOldVersionInfo(): VersionInfo {
    return try {
        val versionPropertiesFile = file("${rootProject.projectDir}/$oldVersionPropertyFile")
        VersionInfo(versionPropertiesFile)
    } catch (e: Exception) {
        val versionPropertiesFile = file("${rootProject.projectDir}/$versionPropertyFile")
        VersionInfo(versionPropertiesFile)
    }
}

/**
 * Get the property from project. This method also ensures to remove double quotation marks on both end-sides
 * and trim the value after that.
 * If the property name is not found from Project, search from environment names. If still not found, return default value.
 *
 * @param proj gradle project instance
 * @param propertyName property or environment variable name to read
 * @param defaultValue default value in case property name is not found.
 * @return
 */
fun getPropertyEx(proj: Project, propertyName: String, defaultValue: Any): Any {
    var value: String = defaultValue.toString()

    when {
        proj.hasProperty(propertyName) -> {
            value = proj.property(propertyName).toString()
        }
        System.getenv().containsKey(propertyName) -> {
            value = System.getenv(propertyName) ?: defaultValue.toString()
        }
        else -> {
            return defaultValue
        }
    }

    // Remove double quotation marks if used and exist on both ends.
    var index = 0
    val last = value.length - 1
    val mid = last / 2

    while ((index <= mid) && (value[index] == '\"') && (value[last - index] == '\"')) {
        index++
    }

    // Substring property value to remove those marks and trim the value.
    value = value.substring(index, value.length - index).trim()

    // Return typed value based on default value type
    return when (defaultValue) {
        is Boolean -> value.toBoolean()
        is Int -> value.toInt()
        else -> value
    }
}


// Task to commit and tag the current version in git
tasks.register("commitAndTagVersion") {
    if (shouldIncrementVersion()) {
        project.copy {
            from("${getRootDir()}/$versionPropertyFile")
            into("${getRootDir()}/")
            rename {
                oldVersionPropertyFile
            }

        }
        val versionInfo = createVersionInfo()
        val currentVersion = versionInfo.getVersion()
        versionInfo.increaseVersionProperties()
        val updatedVersionNumber = versionInfo.getUpdatedVersion()
        val commitMessage = "Published, tagged build version $currentVersion and " +
                "incremented version to $updatedVersionNumber for next build"
        project.exec {
            commandLine("git", "add", "../versions.properties")
        }
        project.exec {
            commandLine("git", "commit", "--author='Jenkins <jenkins@jenkins.mobitv.corp>'", "-m", "$commitMessage")
        }
        project.exec {
            commandLine("git", "tag", currentVersion)
        }
    }
}

publishing {
    val versionInfo = createOldVersionInfo()
    val versionNumber = versionInfo.getVersion()
    publications {
        // Create a Maven publication for the library
        create<MavenPublication>("TivoHarmonic") {
            // Set the coordinates for the publication
            groupId = "com.harmonicinc.clientsideadtracking"
            artifactId = "harmonic-tivo"
            version = versionNumber
            // Add aar to the artifacts
            artifact(file("build/outputs/aar/lib-release.aar"))
            // Add sources to the artifacts
            artifact(file("build/libs/lib-sources.jar")) {
                classifier = "sources"
            }

            pom.withXml {
                val dependenciesNode = asNode().appendNode("dependencies")
                val configurationNames = arrayOf("implementation", "api")
                configurationNames.forEach { configurationName ->
                    configurations[configurationName].allDependencies.forEach {
                        // Exclude OMSDK AAR in POM as it is private
                        if (it.group != null && it.name !in excludedArtifact) {
                            val dependencyNode = dependenciesNode.appendNode("dependency")
                            dependencyNode.appendNode("groupId", it.group)
                            dependencyNode.appendNode("artifactId", it.name)
                            dependencyNode.appendNode("version", it.version)
                        }
                    }
                }
            }
        }
    }

    // Configure the repositories for publishing
    repositories {
        maven {
            // Maven repository URL (replace with your repository URL)
            val mavenPath: String = getPropertyEx(project, "MAVEN_REPO_URL", "").toString()
            val mavenUserName: String = getPropertyEx(project, "MAVEN_REPO_USERNAME", "").toString()
            val mavenPassword: String = getPropertyEx(project, "MAVEN_REPO_PASSWORD", "").toString()
            url = uri(mavenPath)
            isAllowInsecureProtocol = true
            credentials {
                username = mavenUserName
                password = mavenPassword
            }
        }
    }
}

//Tasks to create local Android Release build and publish it to Local Maven
tasks.register("buildLocalRelease") {
    dependsOn("assembleRelease", "releaseSourcesJar")
}
tasks.getByName("publishTivoHarmonicPublicationToMavenLocal").dependsOn("buildLocalRelease")
tasks.register("buildReleaseAndPublishLocal") {
    dependsOn("publishTivoHarmonicPublicationToMavenLocal")
}