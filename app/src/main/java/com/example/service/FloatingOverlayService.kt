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
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
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
import com.example.util.SecurityGuard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: FrameLayout
    private var loginWindowView: FrameLayout? = null
    private var historyWindowView: FrameLayout? = null
    private var historyCollectJob: Job? = null

    private lateinit var accountRepo: AccountRepository
    private lateinit var loginRepo: WBloksLoginRepository
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
        SecurityGuard.verifySecurityOrExit(applicationContext)
        accountRepo = AccountRepository.getInstance(applicationContext)
        loginRepo = WBloksLoginRepository(applicationContext)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        startForegroundServiceNotification()
        createFloatingButton()
        startRealtimeRemoteSwitchMonitor()
    }

    private fun startRealtimeRemoteSwitchMonitor() {
        serviceScope.launch {
            while (isActive) {
                SecurityGuard.verifySecurityOrExit(applicationContext)
                val isOn = SecurityGuard.checkRemoteSwitchIsOn(applicationContext)
                if (!isOn) {
                    Toast.makeText(applicationContext, "App is currently OFF", Toast.LENGTH_SHORT).show()
                    stopSelf()
                    break
                }
                delay(4000)
            }
        }
    }

    private fun startForegroundServiceNotification() {
        val channelId = "floating_overlay_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "FB Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("FB Overlay Active")
            .setContentText("Tap FB button to login · Hold 1s for history")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1001, notification)
    }

    private fun getOverlayLayoutType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingButton() {
        floatingView = FrameLayout(this)

        val btnDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#1D4ED8"))
            setStroke(3, Color.parseColor("#60A5FA"))
        }

        val btnText = TextView(this).apply {
            text = "FB"
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = btnDrawable
        }

        val sizePx = (52 * resources.displayMetrics.density).toInt()
        val btnParams = FrameLayout.LayoutParams(sizePx, sizePx).apply {
            gravity = Gravity.CENTER
        }
        floatingView.addView(btnText, btnParams)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayLayoutType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 280
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
                    if (abs(diffX) > 12 || abs(diffY) > 12) {
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
                        val diffX = abs(event.rawX - initialTouchX)
                        val diffY = abs(event.rawY - initialTouchY)
                        if (diffX < 15 && diffY < 15) {
                            showLoginDialogOverlay()
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

    @SuppressLint("SetTextI18n")
    private fun showLoginDialogOverlay() {
        dismissLoginDialogOverlay()
        dismissHistoryDialogOverlay()

        val density = resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density).toInt()

        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            getOverlayLayoutType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        }

        loginWindowView = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#99000000"))
            setOnClickListener { dismissLoginDialogOverlay() }
        }

        val cardBg = GradientDrawable().apply {
            setColor(Color.parseColor("#0F172A"))
            cornerRadius = dp(12).toFloat()
            setStroke(dp(1), Color.parseColor("#334155"))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg
            setPadding(dp(16), dp(14), dp(16), dp(16))
            setOnClickListener { }
        }

        // Header Row
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
        }

        val titleTv = TextView(this).apply {
            text = "FB Login"
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeBtnBg = GradientDrawable().apply {
            setColor(Color.parseColor("#DC2626"))
            cornerRadius = dp(6).toFloat()
        }

        val closeBtn = Button(this).apply {
            text = "X"
            textSize = 12f
            setTextColor(Color.WHITE)
            background = closeBtnBg
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener { dismissLoginDialogOverlay() }
        }

        headerRow.addView(titleTv)
        headerRow.addView(closeBtn)
        container.addView(headerRow)

        val inputBg1 = GradientDrawable().apply {
            setColor(Color.parseColor("#1E293B"))
            cornerRadius = dp(8).toFloat()
            setStroke(dp(1), Color.parseColor("#475569"))
        }

        val numberInput = EditText(this).apply {
            hint = "Enter Number"
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.WHITE)
            textSize = 14f
            setSingleLine(true)
            background = inputBg1
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setText(accountRepo.savedNumber.value)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dp(8))
            }
        }
        container.addView(numberInput)

        val inputBg2 = GradientDrawable().apply {
            setColor(Color.parseColor("#1E293B"))
            cornerRadius = dp(8).toFloat()
            setStroke(dp(1), Color.parseColor("#475569"))
        }

        val passwordInput = EditText(this).apply {
            hint = "Enter Password (Auto-Saved)"
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.WHITE)
            textSize = 14f
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            background = inputBg2
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setText(accountRepo.savedPassword.value)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }
        container.addView(passwordInput)

        val statusTv = TextView(this).apply {
            text = ""
            setTextColor(Color.parseColor("#FACC15"))
            textSize = 12f
            visibility = View.GONE
            setPadding(0, 0, 0, dp(8))
        }
        container.addView(statusTv)

        val loginBtnBg = GradientDrawable().apply {
            setColor(Color.parseColor("#16A34A"))
            cornerRadius = dp(8).toFloat()
        }

        val loginBtn = Button(this).apply {
            text = "LOGIN"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = loginBtnBg
            minHeight = 0
            minimumHeight = 0
            setPadding(0, dp(10), 0, dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                if (isLoggingIn) return@setOnClickListener

                val phone = numberInput.text.toString().trim()
                val pass = passwordInput.text.toString().trim()

                if (phone.isEmpty()) {
                    statusTv.visibility = View.VISIBLE
                    statusTv.setTextColor(Color.parseColor("#F87171"))
                    statusTv.text = "Enter phone number!"
                    return@setOnClickListener
                }
                if (pass.isEmpty()) {
                    statusTv.visibility = View.VISIBLE
                    statusTv.setTextColor(Color.parseColor("#F87171"))
                    statusTv.text = "Enter password!"
                    return@setOnClickListener
                }

                accountRepo.saveNumber(phone)
                accountRepo.savePassword(pass)

                isLoggingIn = true
                isEnabled = false
                text = "LOGGING IN..."
                statusTv.visibility = View.VISIBLE
                statusTv.setTextColor(Color.parseColor("#FACC15"))
                statusTv.text = "Logging in as $phone..."

                serviceScope.launch {
                    val result = loginRepo.executeWBloksLogin(phone, pass)
                    isLoggingIn = false
                    withContext(Dispatchers.Main) {
                        isEnabled = true
                        text = "LOGIN"
                        when (result) {
                            is LoginResult.Success -> {
                                accountRepo.saveAccount(result.account)
                                statusTv.setTextColor(Color.parseColor("#4ADE80"))
                                statusTv.text = "✔ LOGIN SUCCESS! UID: ${result.account.uid}"
                                Toast.makeText(applicationContext, "Login Successful!", Toast.LENGTH_SHORT).show()
                            }
                            is LoginResult.Failure -> {
                                statusTv.setTextColor(Color.parseColor("#F87171"))
                                statusTv.text = "${result.status}: ${result.message}"
                            }
                        }
                    }
                }
            }
        }

        container.addView(loginBtn)

        val frameParams = FrameLayout.LayoutParams(
            dp(290),
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }

        loginWindowView?.addView(container, frameParams)

        try {
            windowManager.addView(loginWindowView, overlayParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dismissLoginDialogOverlay() {
        loginWindowView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            loginWindowView = null
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showHistoryDialogOverlay() {
        dismissLoginDialogOverlay()
        dismissHistoryDialogOverlay()

        val density = resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density).toInt()

        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            getOverlayLayoutType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        historyWindowView = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#99000000"))
            setOnClickListener { dismissHistoryDialogOverlay() }
        }

        val cardBg = GradientDrawable().apply {
            setColor(Color.parseColor("#0F172A"))
            cornerRadius = dp(12).toFloat()
            setStroke(dp(1), Color.parseColor("#334155"))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setOnClickListener { }
        }

        // Top close row
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, 0, 0, dp(8))
        }

        val closeBtnBg = GradientDrawable().apply {
            setColor(Color.parseColor("#DC2626"))
            cornerRadius = dp(6).toFloat()
        }

        val closeBtn = Button(this).apply {
            text = "X"
            textSize = 12f
            setTextColor(Color.WHITE)
            background = closeBtnBg
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener { dismissHistoryDialogOverlay() }
        }

        headerLayout.addView(closeBtn)
        container.addView(headerLayout)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(340)
            )
        }

        val itemsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scrollView.addView(itemsLayout)
        container.addView(scrollView)

        val frameParams = FrameLayout.LayoutParams(
            dp(310),
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }

        historyWindowView?.addView(container, frameParams)

        historyCollectJob = serviceScope.launch {
            accountRepo.accounts.collectLatest { list ->
                itemsLayout.removeAllViews()
                for (acc in list) {
                    itemsLayout.addView(createAccountRowView(acc))
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
        val density = resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density).toInt()

        val rowBg = GradientDrawable().apply {
            setColor(Color.parseColor("#1E293B"))
            cornerRadius = dp(8).toFloat()
            setStroke(dp(1), Color.parseColor("#334155"))
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rowBg
            setPadding(dp(10), dp(10), dp(10), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dp(8))
            }
        }

        // Top info: ONLY Number + Delete button
        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val numTv = TextView(this).apply {
            text = acc.number
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val delBtnBg = GradientDrawable().apply {
            setColor(Color.parseColor("#7F1D1D"))
            cornerRadius = dp(4).toFloat()
        }

        val delBtn = Button(this).apply {
            text = "X"
            textSize = 11f
            setTextColor(Color.WHITE)
            background = delBtnBg
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setOnClickListener {
                accountRepo.removeAccount(acc.id)
            }
        }

        infoLayout.addView(numTv)
        infoLayout.addView(delBtn)
        row.addView(infoLayout)

        // 3 Copy Buttons Row: NUMBER, COOKIE, UID
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }

        fun makeCopyButton(label: String, hexColor: String, valueToCopy: String, marginLeft: Int, marginRight: Int): Button {
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor(hexColor))
                cornerRadius = dp(6).toFloat()
            }
            return Button(this).apply {
                text = label
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = bg
                minHeight = 0
                minimumHeight = 0
                minWidth = 0
                minimumWidth = 0
                setPadding(0, dp(8), 0, dp(8))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(marginLeft, 0, marginRight, 0)
                }
                setOnClickListener {
                    OtpManager.copyToClipboard(applicationContext, valueToCopy)
                    Toast.makeText(applicationContext, label, Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnRow.addView(makeCopyButton("NUMBER", "#2563EB", acc.number, 0, dp(4)))
        btnRow.addView(makeCopyButton("COOKIE", "#059669", acc.cookie, dp(2), dp(2)))
        btnRow.addView(makeCopyButton("UID", "#D97706", acc.uid, dp(4), 0))

        row.addView(btnRow)
        return row
    }

    private fun dismissHistoryDialogOverlay() {
        historyCollectJob?.cancel()
        historyCollectJob = null
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
        dismissLoginDialogOverlay()
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
