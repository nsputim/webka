package com.smr.web.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Интерфейс для работы с API сервера
 */
interface ApiService {
    
    /**
     * Отправляет данные QR-кода на сервер для авторизации
     * @param request Данные для авторизации
     * @return Ответ сервера
     */
    @POST("ipauth")
    suspend fun sendQrCodeData(@Body request: QrCodeRequest): Response<AuthResponse>
    
    /**
     * Отправляет данные QR-кода на сервер для авторизации и получает ответ в виде строки
     * @param request Данные для авторизации
     * @return Ответ сервера в виде строки
     */
    @POST("ipauth")
    suspend fun sendQrCodeDataRaw(@Body request: QrCodeRequest): Response<ResponseBody>
    
    /**
     * Отправляет данные QR-кода на сервер для авторизации и получает ответ в виде строки
     * @param request Данные для авторизации
     * @return Ответ сервера в виде строки
     */
    @POST("ipauth")
    suspend fun sendQrCodeDataString(@Body request: QrCodeRequest): Response<String>
}

/**
 * Класс запроса с данными QR-кода
 */
data class QrCodeRequest(
    val code: String,  // QR-код
    val device_id: String,  // Идентификатор устройства
    val app_version: String = "1.0",  // Версия приложения
    val platform: String = "android"  // Платформа
)

/**
 * Класс ответа от сервера
 */
data class AuthResponse(
    val success: Boolean,
    val message: String? = null,
    val token: String? = null,
    val user_id: String? = null,
    val error: String? = null
) 