# Stack trace va Line numberlar (Crashlytics xatolik qatorini to'g'ri ko'rsatishi uchun)
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature

# Gson model maydonlari (agar data klasslarda @SerializedName ishlatilgan bo'lsa)
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# R8 warninglarni e'tiborsiz qoldirish
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**