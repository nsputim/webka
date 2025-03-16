package com.smr.web.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.smr.web.MainActivity
import com.smr.web.R
import com.smr.web.databinding.ActivityPinAuthBinding

/**
 * Активность для аутентификации по PIN-коду
 */
class PinAuthActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPinAuthBinding
    private lateinit var authManager: AuthManager
    private val TAG = "PinAuthActivity"
    
    private val pinDots = mutableListOf<ImageView>()
    private val enteredPin = StringBuilder()
    private var isSettingPin = false
    private var confirmPin = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authManager = AuthManager(this)
        
        // Проверяем, установлен ли уже PIN-код
        isSettingPin = intent.getBooleanExtra(EXTRA_SETTING_PIN, false)
        Log.d(TAG, "onCreate: режим установки PIN = $isSettingPin")
        
        // Инициализируем точки PIN-кода
        pinDots.add(binding.pinDot1)
        pinDots.add(binding.pinDot2)
        pinDots.add(binding.pinDot3)
        pinDots.add(binding.pinDot4)
        
        // Настраиваем заголовок
        if (isSettingPin) {
            binding.textTitle.text = "Установите PIN-код"
        } else {
            binding.textTitle.text = "Введите PIN-код"
        }
        
        // Настраиваем кнопки клавиатуры
        setupKeypadButtons()
        
        // Настраиваем кнопку биометрической аутентификации
        setupBiometricButton()
    }
    
    /**
     * Настраивает кнопки клавиатуры
     */
    private fun setupKeypadButtons() {
        // Цифровые кнопки
        val digitButtons = listOf(
            binding.button0, binding.button1, binding.button2, binding.button3,
            binding.button4, binding.button5, binding.button6, binding.button7,
            binding.button8, binding.button9
        )
        
        // Устанавливаем обработчики нажатий для цифровых кнопок
        for (button in digitButtons) {
            button.setOnClickListener {
                val digit = button.text.toString()
                onDigitPressed(digit)
            }
        }
        
        // Кнопка удаления
        binding.buttonBackspace.setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin.deleteCharAt(enteredPin.length - 1)
                updatePinDots()
            }
        }
    }
    
    /**
     * Обрабатывает нажатие на цифровую кнопку
     */
    private fun onDigitPressed(digit: String) {
        if (enteredPin.length < PIN_LENGTH) {
            enteredPin.append(digit)
            updatePinDots()
            
            // Если введены все цифры PIN-кода
            if (enteredPin.length == PIN_LENGTH) {
                Handler(Looper.getMainLooper()).postDelayed({
                    validatePin()
                }, 200)
            }
        }
    }
    
    /**
     * Обновляет отображение точек PIN-кода
     */
    private fun updatePinDots() {
        for (i in pinDots.indices) {
            if (i < enteredPin.length) {
                pinDots[i].background = ContextCompat.getDrawable(this, R.drawable.pin_dot_filled)
            } else {
                pinDots[i].background = ContextCompat.getDrawable(this, R.drawable.pin_dot_empty)
            }
        }
    }
    
    /**
     * Проверяет введенный PIN-код
     */
    private fun validatePin() {
        val pin = enteredPin.toString()
        Log.d(TAG, "validatePin: проверка PIN-кода, режим установки = $isSettingPin")
        
        if (isSettingPin) {
            // Режим установки PIN-кода
            if (confirmPin.isEmpty()) {
                // Первый ввод PIN-кода
                confirmPin = pin
                enteredPin.clear()
                updatePinDots()
                binding.textTitle.text = "Повторите PIN-код"
                binding.textError.visibility = View.INVISIBLE
                Log.d(TAG, "validatePin: первый ввод PIN-кода сохранен, ожидание подтверждения")
            } else {
                // Подтверждение PIN-кода
                Log.d(TAG, "validatePin: проверка совпадения PIN-кодов: введено = $pin, сохранено = $confirmPin")
                if (pin == confirmPin) {
                    // PIN-коды совпадают
                    Log.d(TAG, "validatePin: PIN-коды совпадают, сохранение PIN-кода")
                    authManager.savePin(pin)
                    authManager.setAuthEnabled(true)
                    Log.d(TAG, "validatePin: PIN-код успешно установлен, защита включена: ${authManager.isAuthEnabled()}, PIN-код установлен: ${authManager.hasPin()}")
                    Toast.makeText(this, "PIN-код успешно установлен", Toast.LENGTH_SHORT).show()
                    
                    // Если доступна биометрическая аутентификация, предлагаем включить ее
                    if (authManager.isBiometricAvailable()) {
                        showBiometricEnrollDialog()
                    } else {
                        finishSetup()
                    }
                } else {
                    // PIN-коды не совпадают
                    Log.d(TAG, "validatePin: PIN-коды не совпадают, сброс ввода")
                    binding.textError.text = "PIN-коды не совпадают. Попробуйте снова."
                    binding.textError.visibility = View.VISIBLE
                    confirmPin = ""
                    enteredPin.clear()
                    updatePinDots()
                    binding.textTitle.text = "Установите PIN-код"
                }
            }
        } else {
            // Режим проверки PIN-кода
            Log.d(TAG, "validatePin: проверка PIN-кода для входа")
            if (authManager.validatePin(pin)) {
                // PIN-код верный
                Log.d(TAG, "validatePin: PIN-код верный, переход к основной активности")
                binding.textError.visibility = View.INVISIBLE
                proceedToMainActivity()
            } else {
                // PIN-код неверный
                Log.d(TAG, "validatePin: PIN-код неверный, сброс ввода")
                binding.textError.text = "Неверный PIN-код. Попробуйте снова."
                binding.textError.visibility = View.VISIBLE
                enteredPin.clear()
                updatePinDots()
            }
        }
    }
    
    /**
     * Настраивает кнопку биометрической аутентификации
     */
    private fun setupBiometricButton() {
        if (authManager.isBiometricAvailable() && authManager.isBiometricEnabled()) {
            binding.buttonBiometric.visibility = View.VISIBLE
            binding.buttonBiometric.setOnClickListener {
                showBiometricPrompt()
            }
        } else {
            binding.buttonBiometric.visibility = View.GONE
        }
    }
    
    /**
     * Показывает диалог биометрической аутентификации
     */
    private fun showBiometricPrompt() {
        authManager.showBiometricPrompt(
            this,
            "Вход в приложение",
            "Приложите палец к сканеру отпечатков",
            "Используйте биометрические данные для входа в приложение",
            "Использовать PIN-код",
            { result ->
                when (result) {
                    AuthManager.AUTH_SUCCESS -> proceedToMainActivity()
                    AuthManager.AUTH_FAILED -> Toast.makeText(this, "Биометрическая аутентификация не удалась", Toast.LENGTH_SHORT).show()
                    AuthManager.AUTH_ERROR -> Toast.makeText(this, "Ошибка биометрической аутентификации", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    
    /**
     * Показывает диалог для включения биометрической аутентификации
     */
    private fun showBiometricEnrollDialog() {
        // В реальном приложении здесь должен быть диалог с вопросом о включении биометрической аутентификации
        // Для простоты просто включаем ее
        authManager.setBiometricEnabled(true)
        Toast.makeText(this, "Биометрическая аутентификация включена", Toast.LENGTH_SHORT).show()
        finishSetup()
    }
    
    /**
     * Завершает настройку и переходит к основной активности
     */
    private fun finishSetup() {
        Log.d(TAG, "finishSetup: переход к основной активности")
        proceedToMainActivity()
    }
    
    /**
     * Переходит к основной активности
     */
    private fun proceedToMainActivity() {
        Log.d(TAG, "proceedToMainActivity: создание Intent для MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        // Очищаем стек активностей и создаем новую задачу
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        // Добавляем флаг, чтобы MainActivity знала, что запущена из PinAuthActivity
        intent.putExtra("from_pin_auth", true)
        Log.d(TAG, "proceedToMainActivity: активность запускается")
        startActivity(intent)
        Log.d(TAG, "proceedToMainActivity: завершение текущей активности")
        finish()
    }
    
    companion object {
        const val EXTRA_SETTING_PIN = "extra_setting_pin"
        const val PIN_LENGTH = 4
    }
}