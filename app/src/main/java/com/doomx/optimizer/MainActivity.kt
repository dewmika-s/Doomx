
    private lateinit var contentBody: LinearLayout
    private fun addBottomNav() {
        if (!::contentBody.isInitialized) return
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(16,19,15))
            setPadding(dp(3), dp(5), dp(3), dp(5))
        }
        listOf("Overview", "Sensi Builder", "Performance", "Network", "Storage", "Support").forEach { page ->
            val b = TextView(this).apply {
                text = when(page) { "Overview" -> "HOME"; "Sensi Builder" -> "SENSI"; "Performance" -> "DEVICE"; "Network" -> "NET"; "Storage" -> "FILES"; else -> "HELP" }
                textSize = 8f; gravity = Gravity.CENTER; setTextColor(if (activePage == page) lime else muted)
                setPadding(dp(2), dp(12), dp(2), dp(12)); typeface = Typeface.DEFAULT_BOLD
                setOnClickListener { showPage(page) }
            }
            nav.addView(b, LinearLayout.LayoutParams(0, dp(44), 1f))
        }
        root.addView(nav)
    }

    private fun baseRoot() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(bg)
        setPadding(0, 0, 0, 0)
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = shape(panel, 14)
    }

    private fun metricRow(title: String, value: String, sub: String): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(panel, 12)
            setPadding(dp(13), dp(12), dp(13), dp(12))
        }
        row.addView(label(title, 10f, muted, true))
        row.addView(label(value, 17f, lime, true).apply { setPadding(0, dp(3), 0, dp(2)) })
        row.addView(label(sub, 9f, muted, false))
        return row.apply { layoutParams = match().apply { bottomMargin = dp(7) } }
    }

    private fun logo(size: Int) = ImageView(this).apply {
        setImageResource(R.drawable.doomx_mark)
        layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
    }

    private fun label(t: String, s: Float, c: Int, bold: Boolean) = TextView(this).apply {
        text = t; textSize = s; setTextColor(c)
        if (bold) typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        setLineSpacing(dp(2).toFloat(), 1f)
    }

    private fun button(t: String) = Button(this).apply {
        text = t; textSize = 12f; isAllCaps = false; setTextColor(bg)
        typeface = Typeface.DEFAULT_BOLD; background = shape(lime, 10)
        setPadding(dp(10), dp(8), dp(10), dp(8)); minHeight = dp(46); stateListAnimator = null
    }

    private fun smallButton(t: String) = Button(this).apply {
        text = t; textSize = 10f; isAllCaps = false; setTextColor(if (t == "DELETE") red else lime)
        background = shape(bg, 8); minHeight = dp(40); stateListAnimator = null
    }

    private fun shape(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), if (color == lime) lime else Color.rgb(55,64,45))
    }

    private fun space(h: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(h)) }
    private fun match() = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_LONG).show()
    private fun openUrl(url: String) = safeStart(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    private fun safeStart(i: Intent) { try { startActivity(i) } catch (_: Exception) { toast("This action is not available on this device") } }
    private fun deviceFingerprint(): String {
        val raw = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: Build.MODEL
        return MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }.take(16)
    }
    private fun loadKeys(): JSONObject = try { JSONObject(prefs.getString("keys", "{}") ?: "{}") } catch (_: Exception) { JSONObject() }
    private fun saveKeys(keys: JSONObject) { prefs.edit().putString("keys", keys.toString()).apply() }
    private fun expiryText(ms: Long): String = if (ms == Long.MAX_VALUE) "Lifetime" else SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ms))
    private fun ramText(): String {
        val m = ActivityManager.MemoryInfo()
        (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(m)
        return "${bytes(m.availMem)} free"
    }
    private fun batteryText(): String {
        val i = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra("level", -1) ?: -1
        val scale = i?.getIntExtra("scale", 100) ?: 100
        return if (level >= 0) "${level * 100 / scale}%" else "Unavailable"
    }
    private fun refreshText(): String = try { "${windowManager.defaultDisplay.refreshRate.toInt()} Hz" } catch (_: Exception) { "Unavailable" }
    private fun networkText(): String {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork
        val c = if (net != null) cm.getNetworkCapabilities(net) else null
        return when {
            c == null -> "OFFLINE"
            c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "CONNECTED • WI-FI"
            c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CONNECTED • MOBILE DATA"
            else -> "CONNECTED"
        }
    }
    private fun bytes(b: Long): String {
        if (b < 1024) return "$b B"
        var n = b.toDouble(); val u = arrayOf("KB","MB","GB","TB"); var i = -1
        do { n /= 1024; i++ } while (n >= 1024 && i < u.lastIndex)
        return String.format(Locale.US, "%.1f %s", n, u[i.coerceAtMost(u.lastIndex)])
    }
}
