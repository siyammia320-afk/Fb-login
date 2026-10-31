package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.data.AccountItem
import com.example.data.AccountRepository
import com.example.network.LoginResult
import com.example.network.WBloksLoginRepository
import com.example.util.OtpManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: FrameLayout
    private var historyWindowView: FrameLayout? = null

    private lateinit var accountRepo: AccountRepository
    private val loginRepo = WBloksLoginRepository()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private val handler = Handler(Looper.getMainLooper())
    private var isLongPress = false
    private val longPressRunnable = Runnable {
        isLongPress = true
        showHistoryDialogOverlay()
    }

    private var isLoggingIn = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        accountRepo = AccountRepository.getInstance(applicationContext)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        startForegroundServiceNotification()
        createFloatingButton()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "floating_overlay_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "FB Login Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("FB Login Overlay Active")
            .setContentText("Tap floating button to login, hold 1s for history")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1001, notification)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingButton() {
        floatingView = FrameLayout(this)

        val btnDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#1E3A8A"))
            setStroke(3, Color.parseColor("#60A5FA"))
        }

        val btnText = TextView(this).apply {
            text = "FB"
            setTextColor(Color.WHITE)
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            background = btnDrawable
        }

        val btnParams = FrameLayout.LayoutParams(140, 140).apply {
            gravity = Gravity.CENTER
        }
        floatingView.addView(btnText, btnParams)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }

        floatingView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isLongPress = false
                    handler.postDelayed(longPressRunnable, 1000)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val diffX = (event.rawX - initialTouchX).toInt()
                    val diffY = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(diffX) > 10 || Math.abs(diffY) > 10) {
                        handler.removeCallbacks(longPressRunnable)
                    }
                    params.x = initialX + diffX
                    params.y = initialY + diffY
                    try {
                        windowManager.updateViewLayout(floatingView, params)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(longPressRunnable)
                    if (!isLongPress) {
                        val diffX = Math.abs(event.rawX - initialTouchX)
                        val diffY = Math.abs(event.rawY - initialTouchY)
                        if (diffX < 15 && diffY < 15) {
                            onFloatingSingleTap()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPressRunnable)
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(floatingView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onFloatingSingleTap() {
        if (isLoggingIn) {
            Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show()
            return
        }

        val phone = accountRepo.savedNumber.value.trim()
        val pass = accountRepo.savedPassword.value.trim()
        val customUrl = accountRepo.savedApiUrl.value.trim()

        if (phone.isBlank()) {
            Toast.makeText(this, "Enter Phone Number in App first!", Toast.LENGTH_LONG).show()
            return
        }
        if (pass.isBlank()) {
            Toast.makeText(this, "Enter Password in App first!", Toast.LENGTH_LONG).show()
            return
        }

        isLoggingIn = true
        Toast.makeText(this, "Logging in as $phone...", Toast.LENGTH_SHORT).show()

        serviceScope.launch {
            val result = loginRepo.executeWBloksLogin(phone, pass, customUrl)
            isLoggingIn = false
            when (result) {
                is LoginResult.Success -> {
                    accountRepo.saveAccount(result.account)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "SUCCESS! UID: ${result.account.uid}", Toast.LENGTH_LONG).show()
                    }
                }
                is LoginResult.Failure -> {
                    val failAcc = AccountItem(
                        number = phone,
                        password = pass,
                        cookie = "",
                        uid = "",
                        status = result.status
                    )
                    accountRepo.saveAccount(failAcc)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "${result.status}: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showHistoryDialogOverlay() {
        dismissHistoryDialogOverlay()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        historyWindowView = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#B0000000"))
            setOnClickListener { dismissHistoryDialogOverlay() }
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(32, 32, 32, 32)
            setOnClickListener { }
        }

        val cardBg = GradientDrawable().apply {
            setColor(Color.parseColor("#1E293B"))
            setCornerRadius(16f)
            setStroke(2, Color.parseColor("#334155"))
        }
        container.background = cardBg

        // Header
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }

        val titleTv = TextView(this).apply {
            text = "Account History"
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeBtn = Button(this).apply {
            text = "X"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#DC2626"))
            setOnClickListener { dismissHistoryDialogOverlay() }
        }

        headerLayout.addView(titleTv)
        headerLayout.addView(closeBtn)
        container.addView(headerLayout)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                800
            )
        }

        val itemsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scrollView.addView(itemsLayout)
        container.addView(scrollView)

        val frameParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
            setMargins(40, 40, 40, 40)
        }

        historyWindowView?.addView(container, frameParams)

        // Populate items
        serviceScope.launch {
            accountRepo.accounts.collectLatest { list ->
                itemsLayout.removeAllViews()
                if (list.isEmpty()) {
                    val emptyTv = TextView(this@FloatingOverlayService).apply {
                        text = "No accounts saved yet."
                        setTextColor(Color.LTGRAY)
                        setPadding(16, 32, 16, 32)
                        gravity = Gravity.CENTER
                    }
                    itemsLayout.addView(emptyTv)
                } else {
                    for (acc in list) {
                        itemsLayout.addView(createAccountRowView(acc))
                    }
                }
            }
        }

        try {
            windowManager.addView(historyWindowView, overlayParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createAccountRowView(acc: AccountItem): View {
        val rowBg = GradientDrawable().apply {
            setColor(Color.parseColor("#0F172A"))
            setCornerRadius(8f)
            setStroke(1, Color.parseColor("#334155"))
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rowBg
            setPadding(20, 20, 20, 20)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, 0, 16)
            layoutParams = lp
        }

        // Top info: Number + Delete
        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val numTv = TextView(this).apply {
            text = acc.number
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val statusTv = TextView(this).apply {
            text = acc.status
            setTextColor(if (acc.status == "SUCCESS") Color.parseColor("#4ADE80") else Color.parseColor("#F87171"))
            textSize = 12f
            setPadding(0, 0, 16, 0)
        }

        val delBtn = Button(this).apply {
            text = "Remove"
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#7F1D1D"))
            setOnClickListener {
                accountRepo.removeAccount(acc.id)
            }
        }

        infoLayout.addView(numTv)
        infoLayout.addView(statusTv)
        infoLayout.addView(delBtn)
        row.addView(infoLayout)

        // 3 Copy Buttons Row: NUMBER, COOKIE, UID
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 0)
        }

        val copyNumBtn = Button(this).apply {
            text = "NUMBER"
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2563EB"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 6, 0)
            }
            setOnClickListener {
                OtpManager.copyToClipboard(applicationContext, acc.number)
                Toast.makeText(applicationContext, "NUMBER Copied!", Toast.LENGTH_SHORT).show()
            }
        }

        val copyCookieBtn = Button(this).apply {
            text = "COOKIE"
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#059669"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(3, 0, 3, 0)
            }
            setOnClickListener {
                OtpManager.copyToClipboard(applicationContext, acc.cookie)
                Toast.makeText(applicationContext, "COOKIE Copied!", Toast.LENGTH_SHORT).show()
            }
        }

        val copyUidBtn = Button(this).apply {
            text = "UID"
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#D97706"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(6, 0, 0, 0)
            }
            setOnClickListener {
                OtpManager.copyToClipboard(applicationContext, acc.uid)
                Toast.makeText(applicationContext, "UID Copied!", Toast.LENGTH_SHORT).show()
            }
        }

        btnRow.addView(copyNumBtn)
        btnRow.addView(copyCookieBtn)
        btnRow.addView(copyUidBtn)

        row.addView(btnRow)

        return row
    }

    private fun dismissHistoryDialogOverlay() {
        historyWindowView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            historyWindowView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        dismissHistoryDialogOverlay()
        if (::floatingView.isInitialized) {
            try {
                windowManager.removeView(floatingView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
