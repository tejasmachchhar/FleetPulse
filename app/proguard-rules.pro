# FleetPulse R8 rules
#
# Room, Jetpack Compose, WorkManager, AndroidX, and Kotlin ship their own
# consumer ProGuard rules. FleetPulse does not reflectively load application
# classes, so no broad keep rules are required.
#
# Keep source file names and line numbers so crash reports from minified builds
# remain diagnosable. Obfuscation of identifiers is still enabled.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
