# Keep kotlinx.serialization generated serializers for our model classes.
-keepclassmembers class org.stilllauncher.data.** {
    *** Companion;
}
-keepclasseswithmembers class org.stilllauncher.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
