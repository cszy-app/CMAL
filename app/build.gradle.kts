import java.io.FileInputStream
import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// 签名信息读取：优先环境变量（GitHub Secrets），其次 keystore.properties 文件
val env = System.getenv()
val keyStoreBase64 = env["KEYSTORE_BASE64"]
val keyStorePassword = env["KEYSTORE_PASSWORD"]
val keyAlias = env["KEY_ALIAS"]
val keyPassword = env["KEY_PASSWORD"]

// Xbox 登录 Client ID：从 local.properties 读取（本机密不提交），缺失时用占位符保证可编译
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(FileInputStream(f))
}
val xboxClientId: String = localProps.getProperty("XBOX_CLIENT_ID", "YOUR_AZURE_CLIENT_ID")

var releaseStoreFile: File? = null
var releaseStorePassword: String? = null
var releaseKeyAlias: String? = null
var releaseKeyPassword: String? = null

if (!keyStoreBase64.isNullOrBlank() && !keyStorePassword.isNullOrBlank()
    && !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()
) {
    val decoded = Base64.getDecoder().decode(keyStoreBase64)
    val f = file("${buildDir}/cmal-release.jks")
    f.parentFile.mkdirs()
    f.writeBytes(decoded)
    releaseStoreFile = f
    releaseStorePassword = keyStorePassword
    releaseKeyAlias = keyAlias
    releaseKeyPassword = keyPassword
} else {
    val propsFile = rootProject.file("keystore.properties")
    if (propsFile.exists()) {
        val props = Properties()
        props.load(FileInputStream(propsFile))
        releaseStoreFile = rootProject.file(props.getProperty("storeFile"))
        releaseStorePassword = props.getProperty("storePassword")
        releaseKeyAlias = props.getProperty("keyAlias")
        releaseKeyPassword = props.getProperty("keyPassword")
    }
}

android {
    namespace = "com.cszyapp.cmal"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cszyapp.cmal"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "0.1-beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "XBOX_CLIENT_ID", "\"$xboxClientId\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            if (releaseStoreFile != null) {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // 无正式签名信息时退化为 debug 签名，便于本地联调
                signingConfig = signingConfigs.getByName("debug")
            }
        }
        debug {
            isMinifyEnabled = false
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
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
