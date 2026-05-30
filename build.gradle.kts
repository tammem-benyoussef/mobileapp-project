// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
}

val localAppData = System.getenv("LOCALAPPDATA")
    ?: "${System.getProperty("user.home")}/AppData/Local"

allprojects {
    buildDir = file("$localAppData/HamhamaBuild/${project.name}")
}