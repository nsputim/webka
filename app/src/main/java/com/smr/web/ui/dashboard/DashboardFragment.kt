package com.smr.web.ui.dashboard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.smr.web.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    
    private lateinit var dashboardViewModel: DashboardViewModel
    
    // Запрос разрешения на использование камеры
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startQrCodeScanner()
        } else {
            Toast.makeText(
                requireContext(),
                "Для сканирования QR-кода необходимо разрешение на использование камеры",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    // Обработка результата сканирования QR-кода
    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { qrContent ->
            // Отправляем данные QR-кода на сервер
            dashboardViewModel.sendQrCodeData(qrContent)
            
            // Отображаем результат сканирования
            binding.textResult.apply {
                text = "Отсканированный QR-код: $qrContent"
                visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Настраиваем наблюдателей для LiveData
        setupObservers()
        
        // Настраиваем кнопку сканирования QR-кода
        binding.btnScanQr.setOnClickListener {
            checkCameraPermissionAndStartScanner()
        }
        
        // Настраиваем кнопку выхода из системы
        binding.btnLogout.setOnClickListener {
            dashboardViewModel.logout()
            Toast.makeText(requireContext(), "Вы вышли из системы", Toast.LENGTH_SHORT).show()
            binding.textResult.visibility = View.GONE
        }
        
        return root
    }
    
    private fun setupObservers() {
        // Наблюдаем за текстом
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            binding.textDashboard.text = it
        }
        
        // Наблюдаем за состоянием загрузки
        dashboardViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnScanQr.isEnabled = !isLoading
            binding.btnLogout.isEnabled = !isLoading
        }
        
        // Наблюдаем за состоянием авторизации
        dashboardViewModel.isLoggedIn.observe(viewLifecycleOwner) { isLoggedIn ->
            binding.btnLogout.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
            binding.btnScanQr.text = if (isLoggedIn) "Пересканировать QR-код" else "Сканировать QR-код"
        }
        
        // Наблюдаем за результатом авторизации
        dashboardViewModel.authResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = { response ->
                    if (response.success && response.error.isNullOrEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "Авторизация успешна: ${response.message ?: "Пользователь авторизован"}",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Ошибка авторизации: ${response.error ?: response.message ?: "Неизвестная ошибка"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onFailure = { error ->
                    Toast.makeText(
                        requireContext(),
                        "Ошибка: ${error.message ?: "Неизвестная ошибка"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
    
    private fun checkCameraPermissionAndStartScanner() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Разрешение уже предоставлено, запускаем сканер
                startQrCodeScanner()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Показываем объяснение, почему нужно разрешение
                Toast.makeText(
                    requireContext(),
                    "Для сканирования QR-кода необходимо разрешение на использование камеры",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                // Запрашиваем разрешение
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun startQrCodeScanner() {
        // Настраиваем параметры сканирования
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Наведите камеру на QR-код")
            setCameraId(0) // Используем заднюю камеру
            setBeepEnabled(true) // Звуковой сигнал при успешном сканировании
            setOrientationLocked(false) // Разрешаем поворот экрана
        }
        
        // Запускаем сканер
        scanLauncher.launch(options)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}