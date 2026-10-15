package com.doomx.optimizer

import android.app.Service
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
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showHandle()
    }

    private fun params(width: Int, height: Int, centered: Boolean = false): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE
        return WindowManager.LayoutParams(
            width, height, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (centered) Gravity.CENTER or Gravity.END else Gravity.TOP or Gravity.END
            x = 12
            y = if (centered) 0 else 180
        }
    }

    private fun styledButton(text: String, action: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            textSize = 11f
            setTextColor(Color.rgb(199, 255, 77))
            setBackgroundColor(Color.rgb(28, 33, 25))
            setOnClickListener { action() }
        }

    private fun showHandle() {
        val handle = styledButton(if (expanded) "DOOMX  ×" else "DX  +") {
            if (expanded) showHandle() else showPanel()
        }
        try {
            panelView?.let { runCatching { wm.removeView(it) } }
            panelView = handleAsLayout(handle)
            wm.addView(panelView, params(dp(76), dp(48)))
            expanded = false
        } catch (_: Exception) { stopSelf() }
    }

    private fun handleAsLayout(button: Button): LinearLayout =
        LinearLayout(this).apply {
            setBackgroundColor(Color.rgb(11, 13, 11))
            setPadding(dp(2), dp(2), dp(2), dp(2))
            addView(button, LinearLayout.LayoutParams(-1, -1))
        }

    private fun showPanel() {
        val prefs = getSharedPreferences("doomx_modes", Context.MODE_PRIVATE)
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val popupWidth = (screenWidth * 0.56f).toInt().coerceAtLeast(dp(220))
        val popupHeight = (screenHeight * 0.58f).toInt().coerceAtLeast(dp(330))

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(10))
            setBackgroundColor(Color.rgb(11, 13, 11))
        }
        val title = TextView(this).apply {
            text = "DOOMX  •  GAME PANEL"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(199, 255, 77))
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        content.addView(title)
        content.addView(TextView(this).apply {
            text = "QUICK TOOLS  /  PROFILE  /  TOUCH"
            textSize = 9f
            setTextColor(Color.LTGRAY)
            setPadding(dp(8), 0, dp(8), dp(8))
        })

        val scroll = ScrollView(this).apply { isFillViewport = true }
        val tools = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(17, 20, 16))
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
            setTextColor(Color.rgb(199, 255, 77))
            setPadding(dp(8), dp(12), dp(8), dp(4))
        }
        tools.addView(touchTitle)
        val xValue = TextView(this).apply { setTextColor(Color.WHITE); textSize = 11f }
        val xSeek = SeekBar(this).apply { max = 200; progress = prefs.getInt("touch_x", 100) }
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

        val yValue = TextView(this).apply { setTextColor(Color.WHITE); textSize = 11f }
        val ySeek = SeekBar(this).apply { max = 200; progress = prefs.getInt("touch_y", 100) }
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
            setTextColor(Color.LTGRAY)
            setPadding(dp(8), dp(4), dp(8), dp(10))
        })
        scroll.addView(tools)
        content.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        content.addView(styledButton("CLOSE PANEL  ×") { showHandle() })

        try {
            panelView?.let { runCatching { wm.removeView(it) } }
            panelView = content
            wm.addView(content, params(popupWidth, popupHeight, centered = true))
            expanded = true
            Toast.makeText(this, "DOOMX game panel opened", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) { stopSelf() }
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
