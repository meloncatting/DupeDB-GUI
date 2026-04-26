pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        mavenCentral()
        gradlePluginPortal()
    }

       plugins {
        id("net.fabricmc.fabric-loom-remap") version("1.16-SNAPSHOT")
    }
}





// Should match your modid
    rootProject.name = "dupedbgui"

