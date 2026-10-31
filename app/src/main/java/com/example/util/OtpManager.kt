package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.data.AccountRepository
import java.util.regex.Pattern

object OtpManager {

    private val OTP_PATTERN = Pattern.compile("\\b(\\d{4,8})\\b")

    fun extractAndProcessOtp(context: Context, text: String) {
        if (text.isBlank()) return
        
        // Only process if text contains key terms like OTP, code, pin, verify, Facebook, FB
        val lower = text.lowercase()
        val isOtpContext = lower.contains("otp") || lower.contains("code") || 
                lower.contains("pin") || lower.contains("verification") || 
                lower.contains("facebook") || lower.contains("fb") ||
                lower.contains("পাসওয়ার্ড") || lower.contains("কোড")

        if (!isOtpContext) return

        val matcher = OTP_PATTERN.matcher(text)
        if (matcher.find()) {
            val code = matcher.group(1) ?: return
            
            // 1. Copy to Clipboard
            copyToClipboard(context, code)

            // 2. Update Repository State
            AccountRepository.getInstance(context).setLatestOtp(code)

            // 3. Vibrate device
            vibrate(context)

            // 4. Show Notification
            showOtpNotification(context, code)
        }
    }

    fun copyToClipboard(context: Context, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("OTP Code", text)
            clipboard.setPrimaryClip(clip)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vibrate(context: Context) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(250)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showOtpNotification(context: Context, code: String) {
        val channelId = "otp_channel_id"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "OTP Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for auto-detected OTP codes"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("OTP Code Detected: $code")
            .setContentText("Auto-copied: $code to Clipboard")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1002, notification)
    }
}
