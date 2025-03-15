package com.smr.web.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.smr.web.api.AuthResponse

/**
 * Класс для управления сессией пользователя
 */
class SessionManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()
    private val TAG = "SessionManager"
    
    companion object {
        private const val PREF_NAME = "WebSmrPrefs"
        private const val KEY_TOKEN = "user_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    /**
     * Сохраняет данные авторизации
     * @param authResponse Ответ от сервера с данными авторизации
     */
    fun saveAuthData(authResponse: AuthResponse) {
        // Не сохраняем данные, если есть ошибка
        if (!authResponse.error.isNullOrEmpty()) {
            Log.d(TAG, "Не сохраняем данные авторизации, так как есть ошибка: ${authResponse.error}")
            return
        }
        
        editor.putString(KEY_TOKEN, authResponse.token)
        editor.putString(KEY_USER_ID, authResponse.user_id)
        editor.putBoolean(KEY_IS_LOGGED_IN, authResponse.success)
        editor.apply()
        
        Log.d(TAG, "Сохранены данные авторизации: token=${authResponse.token?.take(10)}, user_id=${authResponse.user_id}")
    }
    
    /**
     * Проверяет, авторизован ли пользователь
     * @return true, если пользователь авторизован
     */
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    /**
     * Возвращает токен авторизации
     * @return Токен авторизации или null, если пользователь не авторизован
     */
    fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }
    
    /**
     * Возвращает идентификатор пользователя
     * @return Идентификатор пользователя или null, если пользователь не авторизован
     */
    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }
    
    /**
     * Выход из системы (удаление данных авторизации)
     */
    fun logout() {
        editor.clear()
        editor.apply()
        Log.d(TAG, "Пользователь вышел из системы")
    }
} 