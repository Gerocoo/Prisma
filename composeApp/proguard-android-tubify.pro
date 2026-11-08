# Android release (R8) rules added by Prisma, on top of proguard-rules.pro.

# Keep class and member names: plugins (Zipline), kotlinx.serialization, Koin and uniffi
# resolve classes by name, so renaming them would break the app at runtime.
-dontobfuscate

# The app itself, its generated Rust bindings and the plugin interfaces stay whole.
-keep class org.spotube.** { *; }
-keep interface org.spotube.** { *; }

# Keep annotations, generics and enclosing info needed by reflection-based libraries.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# Desktop-only and optional dependencies that are never loaded on Android.
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn javax.script.**
-dontwarn java.beans.**
-dontwarn java.lang.management.**
-dontwarn sun.**
-dontwarn org.slf4j.**
-dontwarn org.jetbrains.skiko.**
-dontwarn javafx.**

# QUIC transport excluded on Android in build.gradle.kts (not 16 KB page aligned).
-dontwarn com.ditchoom.socket.quic.**

# Rhino (NewPipe JS engine) optionally links JDK dynalink, absent on Android.
-dontwarn jdk.dynalink.**
