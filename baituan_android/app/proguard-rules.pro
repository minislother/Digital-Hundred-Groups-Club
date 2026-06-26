# Gson 会按字段名解析接口数据，因此保留接口模型字段，避免发布包混淆后反序列化失败。
-keep class com.minislother.baituan.domain.model.** { *; }
-keep class com.minislother.baituan.core.network.** { *; }

# Retrofit 和 Gson 依赖泛型签名及运行期注解完成接口适配和 JSON 字段映射。
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 第三方库在 Android 环境中可能引用可选的 JVM 注解或 Kotlin 扩展类型，忽略这些非运行期必需告警。
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
