package com.smr.web.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.smr.web.auth.AuthManager
import com.smr.web.auth.AuthSettingsActivity
import com.smr.web.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var authManager: AuthManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Инициализация AuthManager
        authManager = AuthManager(requireContext())
        
        // Настройка обработчиков событий
        setupListeners()
    }
    
    private fun setupListeners() {
        // Кнопка настроек безопасности
        binding.buttonSecuritySettings.setOnClickListener {
            val intent = Intent(requireContext(), AuthSettingsActivity::class.java)
            startActivity(intent)
        }
        
        // Кнопка сброса настроек безопасности
        binding.buttonResetSecurity.setOnClickListener {
            resetSecuritySettings()
        }
    }
    
    /**
     * Сбрасывает настройки безопасности
     */
    private fun resetSecuritySettings() {
        authManager.resetAuth()
        
        // Получаем SharedPreferences и сбрасываем флаг первого запуска
        val prefs = requireContext().getSharedPreferences("app_prefs", 0)
        prefs.edit().putBoolean("first_launch", true).apply()
        
        Toast.makeText(requireContext(), "Настройки безопасности сброшены", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 