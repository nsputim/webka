package com.smr.web.auth

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import java.util.concurrent.Executor
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Класс для управления аутентификацией в приложении
 */
class AuthManager(private val context: Context) {
    
    private val TAG = "AuthManager"
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = prefs.edit()
    private lateinit var executor: Executor
    
    companion object {
        private const val PREF_NAME = "app_prefs"
        private const val KEY_AUTH_ENABLED = "auth_enabled"
        private const val KEY_PIN_CODE = "pin_code"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_ALIAS = "auth_key"
        
        // Константы для статусов аутентификации
        const val AUTH_SUCCESS = 0
        const val AUTH_FAILED = 1
        const val AUTH_ERROR = 2
    }
    
    /**
     * Проверяет, включена ли защита приложения
     */
    fun isAuthEnabled(): Boolean {
        val enabled = prefs.getBoolean(KEY_AUTH_ENABLED, false)
        Log.d(TAG, "isAuthEnabled: проверка включения защиты, результат = $enabled")
        return enabled
    }
    
    /**
     * Включает или отключает защиту приложения
     */
    fun setAuthEnabled(enabled: Boolean) {
        Log.d(TAG, "setAuthEnabled: установка защиты в $enabled")
        editor.putBoolean(KEY_AUTH_ENABLED, enabled)
        val result = editor.commit() // Используем commit() вместо apply() для немедленного сохранения
        Log.d(TAG, "setAuthEnabled: результат сохранения = $result, проверка значения: ${prefs.getBoolean(KEY_AUTH_ENABLED, false)}")
    }
    
    /**
     * Сбрасывает все настройки аутентификации
     */
    fun resetAuth() {
        Log.d(TAG, "resetAuth: сброс всех настроек аутентификации")
        editor.remove(KEY_AUTH_ENABLED)
        editor.remove(KEY_PIN_CODE)
        editor.remove(KEY_BIOMETRIC_ENABLED)
        val result = editor.commit()
        Log.d(TAG, "resetAuth: результат сброса = $result")
    }
    
    /**
     * Проверяет, включена ли биометрическая аутентификация
     */
    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }
    
    /**
     * Включает или отключает биометрическую аутентификацию
     */
    fun setBiometricEnabled(enabled: Boolean) {
        editor.putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
        editor.commit()
        Log.d(TAG, "Биометрическая аутентификация ${if (enabled) "включена" else "выключена"}")
    }
    
    /**
     * Сохраняет PIN-код
     */
    fun savePin(pin: String) {
        editor.putString(KEY_PIN_CODE, pin)
        val result = editor.commit()
        val savedPin = prefs.getString(KEY_PIN_CODE, "")
        Log.d(TAG, "PIN-код сохранен, результат = $result, проверка сохранения: ${savedPin != null && savedPin.isNotEmpty()}")
    }
    
    /**
     * Проверяет PIN-код
     */
    fun validatePin(pin: String): Boolean {
        val savedPin = prefs.getString(KEY_PIN_CODE, "") ?: ""
        val isValid = savedPin == pin
        Log.d(TAG, "Проверка PIN-кода: введено $pin, сохранено $savedPin, результат: $isValid")
        return isValid
    }
    
    /**
     * Проверяет, установлен ли PIN-код
     */
    fun hasPin(): Boolean {
        val pin = prefs.getString(KEY_PIN_CODE, "")
        val hasPin = !pin.isNullOrEmpty()
        Log.d(TAG, "hasPin: проверка наличия PIN-кода, результат = $hasPin")
        return hasPin
    }
    
    /**
     * Проверяет, доступна ли биометрическая аутентификация на устройстве
     */
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == 
                BiometricManager.BIOMETRIC_SUCCESS
    }
    
    /**
     * Создает и показывает биометрический диалог
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        description: String,
        negativeButtonText: String,
        callback: (Int) -> Unit
    ) {
        executor = ContextCompat.getMainExecutor(context)
        
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    callback(AUTH_SUCCESS)
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    callback(AUTH_FAILED)
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    callback(AUTH_ERROR)
                }
            })
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    /**
     * Создает ключ для шифрования в Android Keystore
     */
    private fun createKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
        )
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setUserAuthenticationParameters(
                        0,
                        KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
                    )
                }
            }
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
    
    /**
     * Получает ключ из Android Keystore
     */
    private fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        
        val key = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        return key ?: createKey()
    }
    
    /**
     * Получает шифр для биометрической аутентификации
     */
    fun getCipher(): Cipher? {
        return try {
            val cipher = Cipher.getInstance(
                "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"
            )
            cipher.init(Cipher.ENCRYPT_MODE, getKey())
            cipher
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении шифра", e)
            null
        }
    }
}