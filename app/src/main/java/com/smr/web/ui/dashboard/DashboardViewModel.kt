package com.smr.web.ui.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.smr.web.api.AuthResponse
import com.smr.web.repository.AuthRepository
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application.applicationContext)
    private val TAG = "DashboardViewModel"
    
    private val _text = MutableLiveData<String>()
    val text: LiveData<String> = _text
    
    // Состояние загрузки
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Результат авторизации
    private val _authResult = MutableLiveData<Result<AuthResponse>>()
    val authResult: LiveData<Result<AuthResponse>> = _authResult
    
    // Состояние авторизации
    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn
    
    init {
        // Проверяем состояние авторизации при создании ViewModel
        checkAuthState()
    }
    
    /**
     * Проверяет состояние авторизации
     */
    private fun checkAuthState() {
        val isUserLoggedIn = repository.isLoggedIn()
        _isLoggedIn.value = isUserLoggedIn
        
        if (isUserLoggedIn) {
            _text.value = "Вы уже авторизованы. Токен: ${repository.getToken()?.take(10)}..."
        } else {
            _text.value = "Отсканируйте QR-код для авторизации"
        }
    }
    
    /**
     * Отправляет данные QR-кода на сервер
     * @param qrData Данные из QR-кода
     */
    fun sendQrCodeData(qrData: String) {
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "Отправка QR-кода: $qrData")
                val result = repository.sendQrCodeData(qrData)
                _authResult.value = result
                
                // Обновляем состояние авторизации
                checkAuthState()
                
                // Обновляем текст в зависимости от результата
                result.fold(
                    onSuccess = { response ->
                        Log.d(TAG, "Получен ответ: $response")
                        
                        // Проверяем наличие ошибки, даже если success=true
                        if (response.success && response.error.isNullOrEmpty()) {
                            _text.value = "Авторизация успешна: ${response.message ?: "Пользователь авторизован"}"
                        } else {
                            // Если success=true, но есть error, считаем это ошибкой
                            _text.value = "Ошибка авторизации: ${response.error ?: response.message ?: "Неизвестная ошибка"}"
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Ошибка при обработке ответа", error)
                        _text.value = "Ошибка: ${error.message ?: "Неизвестная ошибка"}"
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при отправке QR-кода", e)
                _authResult.value = Result.failure(e)
                _text.value = "Ошибка: ${e.message ?: "Неизвестная ошибка"}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Выход из системы
     */
    fun logout() {
        repository.logout()
        checkAuthState()
    }
}