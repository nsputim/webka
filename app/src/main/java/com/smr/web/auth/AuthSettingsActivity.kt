package com.smr.web.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smr.web.databinding.ActivityAuthSettingsBinding

/**
 * Активность для настройки параметров аутентификации
 */
class AuthSettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAuthSettingsBinding
    private lateinit var authManager: AuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authManager = AuthManager(this)
        
        // Инициализируем UI
        initUI()
        
        // Настраиваем обработчики событий
        setupListeners()
    }
    
    /**
     * Инициализирует UI элементы
     */
    private fun initUI() {
        // Устанавливаем состояние переключателей
        binding.switchAuthEnabled.isChecked = authManager.isAuthEnabled()
        binding.switchBiometricEnabled.isChecked = authManager.isBiometricEnabled()
        
        // Проверяем доступность биометрической аутентификации
        val isBiometricAvailable = authManager.isBiometricAvailable()
        binding.switchBiometricEnabled.isEnabled = isBiometricAvailable
        
        if (isBiometricAvailable) {
            binding.textBiometricStatus.text = "Биометрическая аутентификация доступна на этом устройстве."
        } else {
            binding.textBiometricStatus.text = "Биометрическая аутентификация недоступна на этом устройстве."
        }
        
        // Обновляем состояние UI в зависимости от настроек
        updateUI()
    }
    
    /**
     * Настраивает обработчики событий
     */
    private fun setupListeners() {
        // Переключатель защиты приложения
        binding.switchAuthEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !authManager.isAuthEnabled()) {
                // Если включаем защиту, но PIN-код еще не установлен
                startPinSetupActivity()
            } else {
                authManager.setAuthEnabled(isChecked)
                updateUI()
            }
        }
        
        // Переключатель биометрической аутентификации
        binding.switchBiometricEnabled.setOnCheckedChangeListener { _, isChecked ->
            authManager.setBiometricEnabled(isChecked)
        }
        
        // Кнопка изменения PIN-кода
        binding.buttonChangePin.setOnClickListener {
            startPinSetupActivity()
        }
        
        // Кнопка сохранения
        binding.buttonSave.setOnClickListener {
            saveSettings()
        }
    }
    
    /**
     * Обновляет состояние UI в зависимости от настроек
     */
    private fun updateUI() {
        val isAuthEnabled = authManager.isAuthEnabled()
        
        // Обновляем доступность элементов
        binding.cardPin.alpha = if (isAuthEnabled) 1.0f else 0.5f
        binding.buttonChangePin.isEnabled = isAuthEnabled
        
        binding.cardBiometric.alpha = if (isAuthEnabled) 1.0f else 0.5f
        binding.switchBiometricEnabled.isEnabled = isAuthEnabled && authManager.isBiometricAvailable()
    }
    
    /**
     * Запускает активность для установки PIN-кода
     */
    private fun startPinSetupActivity() {
        val intent = Intent(this, PinAuthActivity::class.java)
        intent.putExtra(PinAuthActivity.EXTRA_SETTING_PIN, true)
        startActivity(intent)
    }
    
    /**
     * Сохраняет настройки и закрывает активность
     */
    private fun saveSettings() {
        Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    override fun onResume() {
        super.onResume()
        // Обновляем UI при возвращении к активности
        initUI()
    }
} 