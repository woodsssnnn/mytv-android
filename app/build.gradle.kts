import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// 合规架构：私有源/EPG 配置通过 gitignored 的 source-private.properties 在构建期注入。
// 公开构建（无该文件）= 空链 = 应用引导用户自行配置；任何人填好自己的文件即可得到完整版。
val sourcePrivateFile = rootProject.file("source-private.properties")
val sourcePrivate = Properties()
if (sourcePrivateFile.exists()) {
    sourcePrivate.load(FileInputStream(sourcePrivateFile))
}

fun stringArrayBuildConfig(name: String): String {
    val items = sourcePrivate.getProperty(name, "")
        ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    return items.joinToString(separator = ", ", prefix = "new String[]{", postfix = "}") { url ->
        "\"" + url.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }
}

android {
    namespace = "top.yogiczy.mytv"
    compileSdk = 34

    defaultConfig {
        applicationId = "top.yogiczy.mytv"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.4.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86_64"))
        }

        buildConfigField("String[]", "REMOTE_SOURCE_URLS", stringArrayBuildConfig("remote_source_urls"))
        buildConfigField("String[]", "EPG_FALLBACK_URLS", stringArrayBuildConfig("epg_fallback_urls"))
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        create("release") {
            storeFile =
                file(System.getenv("KEYSTORE") ?: keystoreProperties["storeFile"] ?: "keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
                ?: keystoreProperties.getProperty("storePassword")
            keyAlias = System.getenv("KEY_ALIAS") ?: keystoreProperties.getProperty("keyAlias")
            keyPassword =
                System.getenv("KEY_PASSWORD") ?: keystoreProperties.getProperty("keyPassword")
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.androidx.material.icons.extended)

    // TV Compose
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)

    // 播放器
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.rtsp)

    // 序列化
    implementation(libs.kotlinx.serialization)

    // 网络请求
    implementation(libs.okhttp)
    implementation(libs.androidasync)

    // 二维码
    implementation(libs.qrose)

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}