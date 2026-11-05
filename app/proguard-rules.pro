# Stack trace va Line numberlar (Crashlytics xatolik qatorini to'g'ri ko'rsatishi uchun)
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature

# Fragment default constructorlarini R8/ProGuard o'chirmasligi uchun saqlash
-keep public class * extends androidx.fragment.app.Fragment {
    public <init>();
}

# Gson model maydonlari (agar data klasslarda @SerializedName ishlatilgan bo'lsa)
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# R8 warninglarni e'tiborsiz qoldirish
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Ktor missing classes warnings (auto-generated from missing_rules.txt)
-dontwarn io.ktor.client.engine.mock.MockEngine$Companion
-dontwarn io.ktor.client.engine.mock.MockEngine
-dontwarn io.ktor.client.engine.mock.MockRequestHandleScope
-dontwarn io.ktor.client.engine.mock.MockUtilsKt
-dontwarn io.ktor.client.plugins.HttpTimeout$HttpTimeoutCapabilityConfiguration
-dontwarn io.ktor.client.plugins.HttpTimeout$Plugin
-dontwarn io.ktor.client.plugins.HttpTimeout
-dontwarn io.ktor.client.plugins.contentnegotiation.ContentNegotiation$Config
-dontwarn io.ktor.client.plugins.contentnegotiation.ContentNegotiation$Plugin
-dontwarn io.ktor.client.plugins.contentnegotiation.ContentNegotiation