package com.smr.web.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.*

/**
 * Утилитарный класс для генерации QR-кодов
 */
object QrCodeGenerator {
    
    /**
     * Генерирует QR-код с заданным содержимым
     * @param content Содержимое QR-кода
     * @param width Ширина QR-кода
     * @param height Высота QR-кода
     * @return Bitmap с QR-кодом
     */
    fun generateQrCode(content: String, width: Int = 500, height: Int = 500): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
            put(EncodeHintType.MARGIN, 1)
        }
        
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints)
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        
        return bitmap
    }
    
    /**
     * Генерирует тестовый QR-код для авторизации
     * @return Строка с данными для QR-кода
     */
    fun generateTestAuthQrCode(): String {
        val userId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        
        return "AUTH:$userId:$timestamp:$random"
    }
} 