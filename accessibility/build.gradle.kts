plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android { namespace="dev.alita.androidagent.accessibilityarchive"; compileSdk=35
 defaultConfig { minSdk=31 }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget="17" }
}
