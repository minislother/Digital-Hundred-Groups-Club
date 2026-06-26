package com.minislother.baituan.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.minislother.baituan.core.auth.TokenStore
import com.minislother.baituan.core.config.AppConfig
import com.minislother.baituan.core.network.BaituanApi
import com.minislother.baituan.core.network.BooleanJsonAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    /**
     * 提供 Gson，用于把后端 JSON 转成 Kotlin 数据对象。
     *
     * 后端部分审核字段可能以 0/1 返回，所以这里注册 BooleanJsonAdapter，
     * 避免数字布尔值导致 Gson 解析异常。
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(Boolean::class.java, BooleanJsonAdapter())
            .registerTypeAdapter(Boolean::class.javaObjectType, BooleanJsonAdapter())
            .create()
    }

    /**
     * 提供 OkHttpClient。
     *
     * 这里统一给每个请求自动加 token 请求头，避免每个接口都手动写鉴权逻辑。
     * 网络日志只在 debug 环境打开，release 环境关闭，防止生产日志暴露敏感信息。
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(tokenStore: TokenStore, appConfig: AppConfig): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (appConfig.enableHttpLogging) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val token = runBlocking { tokenStore.token.first() }
                val requestBuilder = chain.request().newBuilder()
                if (!token.isNullOrBlank()) {
                    requestBuilder.header("X-Token", token)
                    requestBuilder.header("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(logging)
            .build()
    }

    /**
     * 提供 Retrofit。
     *
     * Retrofit 会根据 BaituanApi 中的注解，把函数调用转换成真实 HTTP 请求。
     */
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson, appConfig: AppConfig): Retrofit {
        return Retrofit.Builder()
            .baseUrl(appConfig.baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * 提供后端 API 接口对象，Repository 层通过它调用 Spring Boot 后端。
     */
    @Provides
    @Singleton
    fun provideBaituanApi(retrofit: Retrofit): BaituanApi {
        return retrofit.create(BaituanApi::class.java)
    }
}
