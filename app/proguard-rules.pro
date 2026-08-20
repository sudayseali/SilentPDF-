# Preserve line numbers and source file attributes for stack trace symbolication
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room Database Entities and DAOs
-keep class com.silentpdf.app.data.db.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Tom Roush PDFBox for Android
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.commons.logging.**
-dontwarn com.gemalto.jp2.**

# Google ML Kit Text Recognition
-keep class com.google.mlkit.vision.** { *; }
-keep class com.google.android.gms.vision.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontwarn kotlinx.serialization.**
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Android Framework reflection (PdfRenderer LoadParams on Android 35+)
-dontwarn android.graphics.pdf.PdfRenderer$LoadParams*

