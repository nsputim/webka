package com.smr.web.repository

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.smr.web.api.AuthResponse
import com.smr.web.api.QrCodeRequest
import com.smr.web.api.RetrofitClient
import com.smr.web.data.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

/**
 * Репозиторий для работы с авторизацией
 */
class AuthRepository(private val context: Context) {
    
    private val sessionManager = SessionManager(context)
    private val TAG = "AuthRepository"
    private val gson = Gson()
    
    /**
     * Получает уникальный идентификатор устройства
     */
    private fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
    
    /**
     * Получает версию приложения
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0"
        }
    }
    
    /**
     * Отправляет данные QR-кода на сервер
     * @param qrData Данные из QR-кода
     * @return Результат авторизации
     */
    suspend fun sendQrCodeData(qrData: String): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = QrCodeRequest(
                    code = qrData,
                    device_id = getDeviceId(),
                    app_version = getAppVersion(),
                    platform = "android"
                )
                
                Log.d(TAG, "Отправка запроса: $request")
                
                // Сначала пробуем получить ответ в виде строки
                try {
                    val stringResponse = RetrofitClient.apiService.sendQrCodeDataString(request)
                    Log.d(TAG, "Получен ответ в виде строки: ${stringResponse.body()}")
                    
                    if (stringResponse.isSuccessful) {
                        val responseString = stringResponse.body()
                        
                        if (responseString != null) {
                            try {
                                // Пытаемся распарсить ответ как JSON
                                val authResponse = gson.fromJson(responseString, AuthResponse::class.java)
                                Log.d(TAG, "Успешно распарсили JSON: $authResponse")
                                
                                // Сохраняем данные авторизации, если успешно и нет ошибки
                                if (authResponse.success && authResponse.error.isNullOrEmpty()) {
                                    sessionManager.saveAuthData(authResponse)
                                }
                                
                                return@withContext Result.success(authResponse)
                            } catch (e: JsonSyntaxException) {
                                Log.e(TAG, "Ошибка парсинга JSON из строки: $responseString", e)
                                
                                // Пытаемся вручную извлечь данные из строки
                                val success = responseString.contains("\"success\":true")
                                val error = extractValue(responseString, "error")
                                val message = extractValue(responseString, "message")
                                
                                val authResponse = AuthResponse(
                                    success = success,
                                    error = error,
                                    message = message
                                )
                                
                                return@withContext Result.success(authResponse)
                            }
                        } else {
                            Log.e(TAG, "Пустой ответ от сервера")
                            return@withContext Result.success(
                                AuthResponse(
                                    success = false,
                                    message = "Пустой ответ от сервера",
                                    error = "Сервер вернул пустой ответ"
                                )
                            )
                        }
                    } else {
                        Log.e(TAG, "Ошибка сервера: ${stringResponse.code()} ${stringResponse.message()}")
                        return@withContext Result.success(
                            AuthResponse(
                                success = false,
                                message = "Ошибка сервера: ${stringResponse.code()} ${stringResponse.message()}",
                                error = "HTTP Error: ${stringResponse.code()}"
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при получении строкового ответа, пробуем обычный метод", e)
                    
                    // Если не удалось получить ответ в виде строки, пробуем обычный метод
                    try {
                        val response = RetrofitClient.apiService.sendQrCodeData(request)
                        
                        if (response.isSuccessful) {
                            response.body()?.let { authResponse ->
                                Log.d(TAG, "Получен успешный ответ: $authResponse")
                                // Сохраняем данные авторизации, если успешно и нет ошибки
                                if (authResponse.success && authResponse.error.isNullOrEmpty()) {
                                    sessionManager.saveAuthData(authResponse)
                                }
                                Result.success(authResponse)
                            } ?: run {
                                Log.e(TAG, "Пустой ответ от сервера")
                                // Создаем объект ответа с ошибкой
                                Result.success(AuthResponse(
                                    success = false,
                                    message = "Пустой ответ от сервера",
                                    error = "Сервер вернул пустой ответ"
                                ))
                            }
                        } else {
                            // Пытаемся получить сообщение об ошибке из тела ответа
                            val errorBody = response.errorBody()?.string()
                            Log.e(TAG, "Ошибка сервера: ${response.code()} ${response.message()} $errorBody")
                            
                            Result.success(AuthResponse(
                                success = false,
                                message = "Ошибка сервера: ${response.code()} ${response.message()}",
                                error = errorBody ?: "Неизвестная ошибка"
                            ))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка при получении ответа через обычный метод, пробуем RAW", e)
                        
                        // Если и это не сработало, пробуем получить RAW ответ
                        try {
                            val rawResponse = RetrofitClient.apiService.sendQrCodeDataRaw(request)
                            
                            if (rawResponse.isSuccessful) {
                                val responseString = rawResponse.body()?.string()
                                Log.d(TAG, "Получен RAW ответ: $responseString")
                                
                                if (responseString != null) {
                                    // Пытаемся вручную извлечь данные из строки
                                    val success = responseString.contains("\"success\":true")
                                    val error = extractValue(responseString, "error")
                                    val message = extractValue(responseString, "message")
                                    
                                    Result.success(AuthResponse(
                                        success = success,
                                        error = error,
                                        message = message
                                    ))
                                } else {
                                    Result.success(AuthResponse(
                                        success = false,
                                        message = "Пустой RAW ответ от сервера",
                                        error = "Сервер вернул пустой RAW ответ"
                                    ))
                                }
                            } else {
                                val errorBody = rawResponse.errorBody()?.string()
                                Log.e(TAG, "Ошибка сервера при RAW запросе: ${rawResponse.code()} ${rawResponse.message()} $errorBody")
                                
                                Result.success(AuthResponse(
                                    success = false,
                                    message = "Ошибка сервера при RAW запросе: ${rawResponse.code()} ${rawResponse.message()}",
                                    error = errorBody ?: "Неизвестная ошибка"
                                ))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Все методы запроса не удались", e)
                            Result.success(AuthResponse(
                                success = false,
                                message = "Все методы запроса не удались: ${e.message}",
                                error = "Exception: ${e.javaClass.simpleName}"
                            ))
                        }
                    }
                }
            } catch (e: HttpException) {
                Log.e(TAG, "HttpException: ${e.message()}", e)
                Result.success(AuthResponse(
                    success = false,
                    message = "Ошибка сети: ${e.message()}",
                    error = "HttpException: ${e.code()}"
                ))
            } catch (e: IOException) {
                Log.e(TAG, "IOException: ${e.message}", e)
                Result.success(AuthResponse(
                    success = false,
                    message = "Ошибка сети: Проверьте подключение к интернету",
                    error = "IOException: ${e.message}"
                ))
            } catch (e: Exception) {
                Log.e(TAG, "Exception: ${e.message}", e)
                Result.success(AuthResponse(
                    success = false,
                    message = "Неизвестная ошибка: ${e.message}",
                    error = "Exception: ${e.javaClass.simpleName}"
                ))
            }
        }
    }
    
    /**
     * Извлекает значение по ключу из строки JSON
     */
    private fun extractValue(jsonString: String, key: String): String? {
        // Пробуем несколько вариантов регулярных выражений для извлечения значения
        val patterns = listOf(
            "\"$key\":\"(.*?)\"".toRegex(),  // Для строковых значений в кавычках
            "\"$key\":(true|false)".toRegex(),  // Для булевых значений
            "\"$key\":([0-9]+)".toRegex()  // Для числовых значений
        )
        
        for (pattern in patterns) {
            val matchResult = pattern.find(jsonString)
            val value = matchResult?.groupValues?.getOrNull(1)
            if (value != null) {
                return value
            }
        }
        
        return null
    }
    
    /**
     * Проверяет, авторизован ли пользователь
     * @return true, если пользователь авторизован
     */
    fun isLoggedIn(): Boolean {
        return sessionManager.isLoggedIn()
    }
    
    /**
     * Возвращает токен авторизации
     * @return Токен авторизации или null, если пользователь не авторизован
     */
    fun getToken(): String? {
        return sessionManager.getToken()
    }
    
    /**
     * Выход из системы (удаление данных авторизации)
     */
    fun logout() {
        sessionManager.logout()
    }
} 