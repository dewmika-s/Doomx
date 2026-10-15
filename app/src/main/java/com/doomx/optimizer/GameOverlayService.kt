package com.doomx.optimizer

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class GameOverlayService : Service() {
    private lateinit var wm: WindowManager
    private var panelView: LinearLayout? = null
    private var expanded = false
    private var handleParams: WindowManager.LayoutParams? = null
    private val uiPrefs by lazy { getSharedPreferences("doomx_ui", Context.MODE_PRIVATE) }
    private val monitorHandler = Handler(Looper.getMainLooper())
    private var monitorText: TextView? = null
    private val monitorRunnable = object : Runnable {
        override fun run() {
            val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val tempTenths = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            val temp = if (tempTenths >= 0) String.format(java.util.Locale.US, "%.1f °C", tempTenths / 10.0) else "N/A"
            val mode = getSharedPreferences("doomx_modes", Context.MODE_PRIVATE).getInt("mode_index", 1)
            val modeName = listOf("LOW", "BALANCED", "HIGH")[mode.coerceIn(0, 2)]
            monitorText?.text = "LIVE MONITOR  •  $modeName\nDISPLAY ${displayRefreshRate()} Hz  •  BATTERY $temp\nGAME FPS: Not exposed by Android on this device"
            if (expanded) monitorHandler.postDelayed(this, 1000L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startOverlayForegroundNotification()
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showHandle()
    }

    private fun params(width: Int, height: Int, centered: Boolean = false, focusable: Boolean = false): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE
        val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            (if (focusable) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        return WindowManager.LayoutParams(width, height, type, flags, PixelFormat.TRANSLUCENT).apply {
            gravity = if (centered) Gravity.CENTER else Gravity.TOP or Gravity.END
            x = if (centered) 0 else dp(6)
            y = if (centered) 0 else dp(180)
            if (Build.VERSION.SDK_INT >= 28) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun startOverlayForegroundNotification() {
        val channelId = "doomx_overlay"
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(channelId, "DOOMX floating panel", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, channelId)
                .setContentTitle("DOOMX floating panel is active")
                .setContentText("Tap the small DX button to open game tools")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true).build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this).setContentTitle("DOOMX floating panel is active")
                .setContentText("Tap the small DX button to open game tools")
                .setSmallIcon(android.R.drawable.ic_menu_view).setOngoing(true).build()
        }
        startForeground(2201, notification)
    }

    private fun styledButton(text: String, action: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            textSize = 11f
            isAllCaps = false
            setTextColor(Color.rgb(35, 105, 230))
            setBackgroundColor(Color.rgb(235, 242, 255))
            setOnClickListener { action() }
        }

    @Suppress("DEPRECATION")
    private fun displayRefreshRate(): Int = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display?.refreshRate?.toInt() ?: 60
        else wm.defaultDisplay.refreshRate.toInt()
    } catch (_: Exception) { 60 }

    private fun showHandle() {
        monitorHandler.removeCallbacks(monitorRunnable)
        monitorText = null
        try {
            panelView?.let { runCatching { wm.removeView(it) } }
            val dark = uiPrefs.getBoolean("dark_mode", false)
            val handle = LinearLayout(this).apply {
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (dark) Color.rgb(18, 28, 48) else Color.WHITE)
                    setStroke(dp(2), Color.rgb(35, 105, 230))
                }
                elevation = dp(8).toFloat()
                addView(TextView(this@GameOverlayService).apply {
                    text = "DX"
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setTextColor(if (dark) Color.WHITE else Color.rgb(35, 105, 230))
                }, LinearLayout.LayoutParams(-1, -1))
            }
            val lp = WindowManager.LayoutParams(dp(54), dp(54),
                if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT).apply {
                gravity = Gravity.TOP or Gravity.START
                x = uiPrefs.getInt("handle_x", resources.displayMetrics.widthPixels - dp(68)).coerceAtLeast(0)
                y = uiPrefs.getInt("handle_y", dp(180)).coerceAtLeast(0)
                if (Build.VERSION.SDK_INT >= 28) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            var downX = 0f
            var downY = 0f
            var startX = 0
            var startY = 0
            var moved = false
            handle.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX; downY = event.rawY
                        startX = lp.x; startY = lp.y; moved = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - downX).toInt()
                        val dy = (event.rawY - downY).toInt()
                        if (kotlin.math.abs(dx) > dp(5) || kotlin.math.abs(dy) > dp(5)) moved = true
                        if (moved) {
                            lp.x = (startX + dx).coerceIn(0, (resources.displayMetrics.widthPixels - dp(54)).coerceAtLeast(0))
                            lp.y = (startY + dy).coerceIn(0, (resources.displayMetrics.heightPixels - dp(54)).coerceAtLeast(0))
                            runCatching { wm.updateViewLayout(handle, lp) }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (moved) {
                            uiPrefs.edit().putInt("handle_x", lp.x).putInt("handle_y", lp.y).apply()
                        } else if (expanded) {
                            showHandle()
                        } else {
                            showPanel()
                        }
                        true
                    }
                    else -> true
                }
            }
            panelView = handle
            handleParams = lp
            wm.addView(handle, lp)
            expanded = false
        } catch (_: Exception) {
            Toast.makeText(this, "DOOMX could not display the floating button. Check overlay permission and device restrictions.", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun showPanel() {
        val prefs = getSharedPreferences("doomx_modes", Context.MODE_PRIVATE)
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val popupWidth = (screenWidth * 0.52f).toInt().coerceAtMost(screenWidth - dp(24)).coerceAtLeast(dp(220))
        val popupHeight = (screenHeight * 0.55f).toInt().coerceAtMost(screenHeight - dp(48)).coerceAtLeast(dp(300))

        val dark = uiPrefs.getBoolean("dark_mode", false)
        val surface = if (dark) Color.rgb(18, 22, 32) else Color.rgb(255, 255, 255)
        val cardSurface = if (dark) Color.rgb(29, 36, 51) else Color.rgb(239, 245, 255)
        val primaryText = if (dark) Color.rgb(245, 248, 255) else Color.rgb(25, 35, 55)
        val mutedText = if (dark) Color.rgb(170, 184, 207) else Color.rgb(92, 104, 125)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(10))
            setBackgroundColor(surface)
        }
        val title = TextView(this).apply {
            text = "DOOMX  •  GAME PANEL"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(35, 105, 230))
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        content.addView(title)
        content.addView(TextView(this).apply {
            text = "QUICK TOOLS  /  PROFILE  /  TOUCH"
            textSize = 9f
            setTextColor(mutedText)
            setPadding(dp(8), 0, dp(8), dp(8))
        })

        val scroll = ScrollView(this).apply { isFillViewport = true }
        val tools = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cardSurface)
        }
        val modeLabel = TextView(this).apply {
            text = "PERFORMANCE PROFILE"
            textSize = 12f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(primaryText); setPadding(dp(8), dp(12), dp(8), dp(4))
        }
        tools.addView(modeLabel)
        val modeNames = listOf("LOW", "BALANCED", "HIGH")
        val modeDescriptions = listOf("Low resource profile", "Balanced profile", "High performance profile")
        val selectedMode = prefs.getInt("mode_index", 1).coerceIn(0, 2)
        val cycleMode = styledButton("${modeNames[selectedMode]}  •  TAP TO CHANGE  ↻") {
            val next = (prefs.getInt("mode_index", 1) + 1) % 3
            setMode(modeDescriptions[next], next)
            showPanel()
        }.apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(when (selectedMode) {
                0 -> Color.rgb(35, 105, 230)
                1 -> Color.rgb(225, 45, 65)
                else -> Color.rgb(20, 150, 100)
            })
        }
        tools.addView(cycleMode, LinearLayout.LayoutParams(-1, dp(48)).apply { setMargins(dp(6), dp(3), dp(6), dp(8)) })
        tools.addView(styledButton("CLEAR RAM  •  SAFE CHECK") { clearOwnMemory() })
        tools.addView(styledButton("NETWORK OPTIMIZE") { openNetworkSettings() })
        tools.addView(styledButton("FPS / PERFORMANCE INFO") { Toast.makeText(this, "Selected profile changes DOOMX settings only; Android controls actual game FPS and CPU/GPU power.", Toast.LENGTH_LONG).show() })
        tools.addView(styledButton("TEMPERATURE MONITOR") { showTemperature() })
        monitorText = TextView(this).apply {
            textSize = 11f; setTextColor(primaryText); setPadding(dp(8), dp(10), dp(8), dp(10))
            background = android.graphics.drawable.GradientDrawable().apply { setColor(cardSurface); cornerRadius = dp(8).toFloat() }
        }
        tools.addView(monitorText)
        monitorHandler.removeCallbacks(monitorRunnable)
        monitorHandler.post(monitorRunnable)

        val touchTitle = TextView(this).apply {
            text = "X / Y TOUCH SENSITIVITY EDITOR"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(35, 105, 230))
            setPadding(dp(8), dp(12), dp(8), dp(4))
        }
        tools.addView(touchTitle)
        val xValue = TextView(this).apply { setTextColor(primaryText); textSize = 11f }
        val xSeek = SeekBar(this).apply {
            max = 200; progress = prefs.getInt("touch_x", 100)
            progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(35, 105, 230))
            thumbTintList = android.content.res.ColorStateList.valueOf(Color.rgb(225, 45, 65))
        }
        xValue.text = "X AXIS: ${xSeek.progress}"
        xSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                xValue.text = "X AXIS: $progress"
                if (fromUser) prefs.edit().putInt("touch_x", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Toast.makeText(this@GameOverlayService, "X-axis profile saved: ${seekBar?.progress}", Toast.LENGTH_SHORT).show()
            }
        })
        tools.addView(xValue); tools.addView(xSeek)

        val yValue = TextView(this).apply { setTextColor(primaryText); textSize = 11f }
        val ySeek = SeekBar(this).apply {
            max = 200; progress = prefs.getInt("touch_y", 100)
            progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(35, 105, 230))
            thumbTintList = android.content.res.ColorStateList.valueOf(Color.rgb(225, 45, 65))
        }
        yValue.text = "Y AXIS: ${ySeek.progress}"
        ySeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                yValue.text = "Y AXIS: $progress"
                if (fromUser) prefs.edit().putInt("touch_y", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Toast.makeText(this@GameOverlayService, "Y-axis profile saved: ${seekBar?.progress}", Toast.LENGTH_SHORT).show()
            }
        })
        tools.addView(yValue); tools.addView(ySeek)
        tools.addView(TextView(this).apply {
            text = "Note: X/Y values are saved in DOOMX only. Android apps cannot rewrite Free Fire's private touch settings."
            textSize = 10f
            setTextColor(mutedText)
            setPadding(dp(8), dp(4), dp(8), dp(10))
        })
        scroll.addView(tools)
        content.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        content.addView(styledButton("CLOSE PANEL  ×") { showHandle() })

        try {
            panelView?.let { runCatching { wm.removeView(it) } }
            panelView = content
            wm.addView(content, params(popupWidth, popupHeight, centered = true, focusable = true))
            expanded = true
            monitorHandler.removeCallbacks(monitorRunnable)
            monitorHandler.post(monitorRunnable)
            Toast.makeText(this, "DOOMX game panel opened", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "DOOMX could not open the popup on this device. Try a smaller display size or check overlay permission.", Toast.LENGTH_LONG).show()
            runCatching { panelView?.let { wm.removeView(it) } }
            panelView = null
            expanded = false
        }
    }

    private fun clearOwnMemory() {
        // A normal Android app cannot clear other apps' private memory.
        Toast.makeText(this, "RAM check complete. Android does not let DOOMX clear other apps' RAM; use Android Recents to close apps you do not need.", Toast.LENGTH_LONG).show()
    }

    private fun openNetworkSettings() {
        val i = Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(i) }
        Toast.makeText(this, "Network settings opened. Check Wi-Fi/mobile data; DOOMX cannot directly lower ping.", Toast.LENGTH_LONG).show()
    }

    private fun setMode(mode: String, index: Int) {
        getSharedPreferences("doomx_modes", Context.MODE_PRIVATE).edit()
            .putInt("mode_index", index).putString("mode", mode).apply()
        Toast.makeText(this, "$mode profile selected successfully. This saves the DOOMX profile; Android controls actual CPU/GPU and game FPS.", Toast.LENGTH_LONG).show()
    }

    private fun showTemperature() {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val tenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val msg = if (tenths >= 0) "Battery temperature: ${tenths / 10.0} °C" else "Battery temperature unavailable"
        Toast.makeText(this, "Temperature check complete. $msg", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        panelView?.let { runCatching { wm.removeView(it) } }
        panelView = null
        monitorHandler.removeCallbacks(monitorRunnable)
        monitorText = null
        super.onDestroy()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
