package com.smr.web.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

/**
 * Класс для настройки и получения экземпляра Retrofit
 */
object RetrofitClient {
    
    // Базовый URL сервера
    private const val BASE_URL = "https://web.s-m-r.ru/api/"
    
    // Создаем OkHttpClient с логированием и таймаутами
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .method(original.method, original.body)
            
            val request = requestBuilder.build()
            chain.proceed(request)
        }
        .followRedirects(false)  // Не следовать за перенаправлениями
        .followSslRedirects(false)  // Не следовать за SSL перенаправлениями
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // Создаем Gson с настройкой lenient для обработки некорректного JSON
    private val gson = GsonBuilder()
        .setLenient()
        .create()
    
    // Создаем экземпляр Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(ScalarsConverterFactory.create())  // Добавляем конвертер для строк
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    // Создаем экземпляр ApiService
    val apiService: ApiService = retrofit.create(ApiService::class.java)
} 