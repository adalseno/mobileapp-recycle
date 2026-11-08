# R8 rules for the release build.
# Libraries used here (Compose, Ktor, kotlinx.serialization, Play services) ship their own
# consumer rules; add app-specific rules below only if a release build misbehaves.

# Google Code Scanner (ML Kit): its internals break under R8 full mode (NullPointerException
# inside com.google.android.gms.internal.mlkit_code_scanner when the scanner client is created).
-keep class com.google.mlkit.vision.codescanner.** { *; }
-keep class com.google.android.gms.internal.mlkit_code_scanner.** { *; }
-keep class com.google.mlkit.common.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }
