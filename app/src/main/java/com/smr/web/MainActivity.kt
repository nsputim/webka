package com.smr.web

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.smr.web.auth.AuthManager
import com.smr.web.auth.PinAuthActivity
import com.smr.web.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var authManager: AuthManager
    
    private val TAG = "MainActivity"
    
    companion object {
        private const val PREF_FIRST_LAUNCH = "first_launch"
        private const val PREF_NAME = "app_prefs"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: запуск MainActivity")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Инициализация AuthManager
        authManager = AuthManager(this)
        
        // Получение SharedPreferences
        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        
        // Проверяем, не запущены ли мы из PinAuthActivity
        if (intent.hasExtra("from_pin_auth")) {
            Log.d(TAG, "onCreate: запущены из PinAuthActivity, настраиваем навигацию")
            setupNavigation()
            return
        }
        
        // Проверка первого запуска
        val firstLaunch = isFirstLaunch()
        Log.d(TAG, "onCreate: первый запуск = $firstLaunch")
        
        if (firstLaunch) {
            // Запуск инициализации защиты
            Log.d(TAG, "onCreate: запуск инициализации защиты")
            startSecuritySetup()
            return
        }
        
        // Проверка, включена ли защита и установлен ли PIN-код
        val authEnabled = authManager.isAuthEnabled()
        val hasPin = authManager.hasPin()
        Log.d(TAG, "onCreate: защита включена = $authEnabled, PIN-код установлен = $hasPin")
        
        if (authEnabled && hasPin) {
            // Если защита включена и PIN-код установлен, запускаем экран ввода PIN-кода
            Log.d(TAG, "onCreate: запуск экрана ввода PIN-кода")
            startPinAuthActivity(false)
            return
        } else if (authEnabled && !hasPin) {
            // Если защита включена, но PIN-код не установлен, запускаем установку PIN-кода
            Log.d(TAG, "onCreate: защита включена, но PIN-код не установлен, запуск установки PIN-кода")
            startSecuritySetup()
            return
        }

        Log.d(TAG, "onCreate: настройка навигации")
        setupNavigation()
    }
    
    /**
     * Настраивает навигацию в приложении
     */
    private fun setupNavigation() {
        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications,
                R.id.navigation_settings
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
    
    /**
     * Проверяет, является ли текущий запуск первым
     */
    private fun isFirstLaunch(): Boolean {
        val isFirstLaunch = prefs.getBoolean(PREF_FIRST_LAUNCH, true)
        Log.d(TAG, "isFirstLaunch: проверка первого запуска, результат = $isFirstLaunch")
        
        // Проверяем, есть ли уже установленный PIN-код
        val hasPin = authManager.hasPin()
        Log.d(TAG, "isFirstLaunch: проверка наличия PIN-кода, результат = $hasPin")
        
        // Если это первый запуск, сохраняем это значение
        if (isFirstLaunch) {
            prefs.edit().putBoolean(PREF_FIRST_LAUNCH, false).commit()
            Log.d(TAG, "isFirstLaunch: установлен флаг первого запуска в false")
        }
        
        // Если это первый запуск или нет PIN-кода, считаем это первым запуском
        return isFirstLaunch || !hasPin
    }
    
    /**
     * Запускает настройку безопасности
     */
    private fun startSecuritySetup() {
        Log.d(TAG, "startSecuritySetup: запуск настройки безопасности")
        startPinAuthActivity(true)
    }
    
    /**
     * Запускает активность аутентификации по PIN-коду
     */
    private fun startPinAuthActivity(isSettingPin: Boolean) {
        Log.d(TAG, "startPinAuthActivity: запуск активности PinAuthActivity, режим установки PIN = $isSettingPin")
        val intent = Intent(this, PinAuthActivity::class.java)
        intent.putExtra(PinAuthActivity.EXTRA_SETTING_PIN, isSettingPin)
        startActivity(intent)
        finish()
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: MainActivity возобновлена")
    }
    
    /**
     * Сбрасывает настройки безопасности (для отладки)
     */
    private fun resetSecuritySettings() {
        Log.d(TAG, "resetSecuritySettings: сброс настроек безопасности")
        authManager.resetAuth()
        prefs.edit().putBoolean(PREF_FIRST_LAUNCH, true).commit()
        Log.d(TAG, "resetSecuritySettings: настройки сброшены, первый запуск = ${prefs.getBoolean(PREF_FIRST_LAUNCH, true)}")
    }
}