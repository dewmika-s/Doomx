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
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
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
            setTextColor(Color.rgb(35, 105, 230))
            setBackgroundColor(Color.rgb(235, 242, 255))
            setOnClickListener { action() }
        }

    private fun showHandle() {
        val handle = styledButton(if (expanded) "DX  ×" else "DX  +") {
            if (expanded) showHandle() else showPanel()
        }.apply { textSize = 12f; minHeight = dp(44); minWidth = dp(48) }
        try {
            panelView?.let { runCatching { wm.removeView(it) } }
            panelView = handleAsLayout(handle)
            wm.addView(panelView, params(dp(58), dp(48)))
            expanded = false
        } catch (_: Exception) {
            Toast.makeText(this, "DOOMX could not display the floating button. Check overlay permission and device restrictions.", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun handleAsLayout(button: Button): LinearLayout =
        LinearLayout(this).apply {
            setBackgroundColor(Color.rgb(255, 255, 255))
            setPadding(dp(2), dp(2), dp(2), dp(2))
            addView(button, LinearLayout.LayoutParams(-1, -1))
        }

    private fun showPanel() {
        val prefs = getSharedPreferences("doomx_modes", Context.MODE_PRIVATE)
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val popupWidth = (screenWidth * 0.52f).toInt().coerceAtMost(screenWidth - dp(24)).coerceAtLeast(dp(220))
        val popupHeight = (screenHeight * 0.55f).toInt().coerceAtMost(screenHeight - dp(48)).coerceAtLeast(dp(300))

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(10))
            setBackgroundColor(Color.rgb(255, 255, 255))
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
            setTextColor(Color.rgb(92, 104, 125))
            setPadding(dp(8), 0, dp(8), dp(8))
        })

        val scroll = ScrollView(this).apply { isFillViewport = true }
        val tools = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(239, 245, 255))
        }
        tools.addView(styledButton("CLEAR RAM  •  SAFE CHECK") { clearOwnMemory() })
        tools.addView(styledButton("NETWORK OPTIMIZE") { openNetworkSettings() })
        tools.addView(styledButton("FPS BOOSTER  •  APPLY PROFILE") {
            setMode("Performance", 2)
            Toast.makeText(this, "FPS booster profile applied. Android does not allow this app to force game FPS or CPU/GPU clocks.", Toast.LENGTH_LONG).show()
        })
        tools.addView(styledButton("TEMPERATURE MONITOR") { showTemperature() })
        tools.addView(styledButton("LOW PERFORMANCE MODE") { setMode("Low Performance", 0) })
        tools.addView(styledButton("BALANCED MODE") { setMode("Balanced", 1) })
        tools.addView(styledButton("PERFORMANCE MODE") { setMode("Performance", 2) })

        val touchTitle = TextView(this).apply {
            text = "X / Y TOUCH SENSITIVITY EDITOR"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(35, 105, 230))
            setPadding(dp(8), dp(12), dp(8), dp(4))
        }
        tools.addView(touchTitle)
        val xValue = TextView(this).apply { setTextColor(Color.rgb(25, 35, 55)); textSize = 11f }
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

        val yValue = TextView(this).apply { setTextColor(Color.rgb(25, 35, 55)); textSize = 11f }
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
            setTextColor(Color.rgb(92, 104, 125))
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
        super.onDestroy()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
