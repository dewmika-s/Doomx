package com.doomx.optimizer

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.StatFs
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MainActivity : Activity() {
    private val bg = Color.rgb(11,13,11)
    private val panel = Color.rgb(25,29,24)
    private val panel2 = Color.rgb(32,37,29)
    private val lime = Color.rgb(199,255,77)
    private val red = Color.rgb(255,59,53)
    private val white = Color.rgb(245,247,239)
    private val muted = Color.rgb(151,163,139)
    private val prefs by lazy { getSharedPreferences("doomx_local_demo", Context.MODE_PRIVATE) }
    private lateinit var root: LinearLayout
    private var activePage = "Overview"
    private var boostInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        showLogin()
    }

    private fun showLogin() {
        root = baseRoot()
        root.gravity = Gravity.CENTER
        val card = card()
        card.gravity = Gravity.CENTER_HORIZONTAL
        card.addView(logo(84))
        card.addView(label("DOOMX", 31f, lime, true))
        card.addView(label("FF OPTIMIZER  /  ACCESS CONTROL", 11f, muted, true))
        card.addView(space(18))
        card.addView(label("ENTER ACCESS KEY", 16f, white, true))
        card.addView(label("Ask the owner for a valid key to continue.", 12f, muted, false))
        val keyInput = EditText(this).apply {
            hint = "Enter your access key"
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 15f
            singleLine = true
            background = shape(panel2, 10)
            setPadding(dp(14), dp(13), dp(14), dp(13))
        }
        card.addView(keyInput, match().apply { topMargin = dp(14) })
        val enter = button("UNLOCK OPTIMIZER")
        enter.setOnClickListener { validateUserKey(keyInput.text.toString().trim()) }
        card.addView(enter, match().apply { topMargin = dp(12) })
        val contact = button("GET A KEY  •  WHATSAPP")
        contact.setOnClickListener { openUrl("https://whatsapp.com/channel/0029Vb9POf93WHTQdAtoDg3n") }
        card.addView(contact, match().apply { topMargin = dp(8) })
        val email = button("CONTACT: Doomxcheats@gmail.com")
        email.setOnClickListener {
            val i = Intent(Intent.ACTION_SENDTO).apply { data = android.net.Uri.parse("mailto:Doomxcheats@gmail.com") }
            safeStart(i)
        }
        card.addView(email, match().apply { topMargin = dp(8) })
        card.addView(label("DEMO BUILD: LOCAL KEYS ARE NOT SECURE ONLINE LICENSING", 9f, red, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, 0)
        })
        root.addView(card, match().apply { setMargins(dp(20), dp(20), dp(20), dp(20)) })
        setContentView(root)
    }

    private fun validateUserKey(key: String) {
        if (key.isBlank()) { toast("Enter an access key first"); return }
        if (key == "doomadmin") { showOwner(); return }
        val keys = loadKeys()
        val item = keys.optJSONObject(key) ?: run { toast("Invalid key"); return }
        if (!item.optBoolean("active", false)) { toast("This key is deactivated"); return }
        val now = System.currentTimeMillis()
        val expires = item.optLong("expires", Long.MAX_VALUE)
        if (expires < now) { toast("This key has expired"); return }
        val device = deviceFingerprint()
        val bound = item.optString("device", "")
        if (bound.isNotBlank() && bound != device) { toast("This key is already linked to another device"); return }
        item.put("device", device)
        item.put("lastUsed", now)
        keys.put(key, item)
        saveKeys(keys)
        prefs.edit().putString("active_user_key", key).apply()
        showDashboard()
    }

    private fun showOwner() {
        activePage = "Owner"
        root = appRoot("OWNER CONSOLE")
        val p = card()
        p.addView(label("OWNER ACCESS", 22f, lime, true))
        p.addView(label("Create access keys and manage this device's local key list.", 12f, muted, false))
        val nameInput = EditText(this).apply {
            hint = "Key label / customer ID (optional)"
            setHintTextColor(muted); setTextColor(white); textSize = 14f
            background = shape(panel2, 10); setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        p.addView(nameInput, match().apply { topMargin = dp(12) })
        val duration = Spinner(this)
        val options = listOf("24 hours", "7 days", "30 days", "1 year", "Lifetime")
        duration.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        p.addView(duration, match().apply { topMargin = dp(8) })
        val generate = button("GENERATE ACCESS KEY")
        generate.setOnClickListener {
            val code = "DX-${Random.nextInt(1000,9999)}-${Random.nextInt(1000,9999)}-${Random.nextInt(1000,9999)}"
            val millis = when (duration.selectedItemPosition) {
                0 -> 24L*60*60*1000
                1 -> 7L*24*60*60*1000
                2 -> 30L*24*60*60*1000
                3 -> 365L*24*60*60*1000
                else -> Long.MAX_VALUE
            }
            val expires = if (millis == Long.MAX_VALUE) Long.MAX_VALUE else System.currentTimeMillis()+millis
            val obj = JSONObject().put("id", code).put("label", nameInput.text.toString().trim())
                .put("created", System.currentTimeMillis()).put("expires", expires)
                .put("active", true).put("device", "").put("duration", options[duration.selectedItemPosition])
            val keys = loadKeys(); keys.put(code, obj); saveKeys(keys)
            val shown = label("NEW KEY: $code", 14f, lime, true)
            p.addView(shown, 0)
            toast("Key created. Copy it and give it to one user.")
            refreshKeysList(keysBox)
        }
        p.addView(generate, match().apply { topMargin = dp(10) })
        p.addView(label("KEYS • LOCAL DEVICE STORAGE", 14f, white, true).apply { setPadding(0, dp(18), 0, dp(8)) })
        keysBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        p.addView(keysBox, match())
        refreshKeysList(keysBox)
        root.addView(p, match().apply { setMargins(dp(14), dp(10), dp(14), dp(16)) })
        val note = card()
        note.addView(label("SECURITY LIMIT", 13f, red, true))
        note.addView(label("This owner panel is a local prototype. Keys are saved on this phone only and can be bypassed by modifying/reinstalling the app. Secure multi-device licensing requires a server-side database and protected owner authentication.", 12f, muted, false))
        root.addView(note, match().apply { setMargins(dp(14), 0, dp(14), dp(18)) })
        addBottomNav()
    }

    private lateinit var keysBox: LinearLayout
    private fun refreshKeysList(box: LinearLayout) {
        box.removeAllViews()
        val keys = loadKeys()
        val iter = keys.keys()
        if (!iter.hasNext()) {
            box.addView(label("No keys generated yet.", 12f, muted, false))
            return
        }
        while (iter.hasNext()) {
            val k = iter.next()
            val item = keys.optJSONObject(k) ?: continue
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = shape(panel2, 10)
                setPadding(dp(10), dp(10), dp(10), dp(10))
            }
            row.addView(label("$k  •  ${item.optString("duration")}", 12f, white, true))
            val bound = if (item.optString("device").isBlank()) "Not linked" else "Linked to one device"
            val state = if (item.optBoolean("active")) "ACTIVE" else "DEACTIVATED"
            row.addView(label("$state • $bound • ${expiryText(item.optLong("expires"))}", 10f, muted, false))
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val toggle = smallButton(if (item.optBoolean("active")) "DEACTIVATE" else "ACTIVATE")
            toggle.setOnClickListener {
                item.put("active", !item.optBoolean("active"))
                keys.put(k, item); saveKeys(keys); refreshKeysList(box)
            }
            val delete = smallButton("DELETE")
            delete.setOnClickListener {
                keys.remove(k); saveKeys(keys); refreshKeysList(box)
            }
            actions.addView(toggle, LinearLayout.LayoutParams(0, dp(42), 1f))
            actions.addView(delete, LinearLayout.LayoutParams(0, dp(42), 1f).apply { leftMargin = dp(6) })
            row.addView(actions)
            box.addView(row, match().apply { bottomMargin = dp(8) })
        }
        val all = button("DELETE ALL KEYS")
        all.setOnClickListener {
            android.app.AlertDialog.Builder(this).setTitle("Delete all keys?")
                .setMessage("This cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ -> saveKeys(JSONObject()); refreshKeysList(box) }.show()
        }
        box.addView(all, match())
    }

    private fun showDashboard() = showPage("Overview")

    private fun showPage(page: String) {
        activePage = page
        root = appRoot(page.uppercase(Locale.US))
        when (page) {
            "Overview" -> overview()
            "Sensi Builder" -> sensiPage()
            "Performance" -> performancePage()
            "Network" -> networkPage()
            "Storage" -> storagePage()
            "Support" -> supportPage()
            "Owner" -> { showOwner(); return }
        }
        addBottomNav()
    }

    private fun overview() {
        val hero = card()
        hero.addView(label("DOOMX  /  GAMING SYSTEM", 11f, lime, true))
        hero.addView(label("PREPARE FOR BATTLE", 26f, white, true).apply { setPadding(0, dp(5), 0, dp(4)) })
        hero.addView(label("Device-aware diagnostics and safe gaming preparation.", 12f, muted, false))
        val boost = button(if (boostInProgress) "PREPARING..." else "⚡  BOOST & PLAY FREE FIRE")
        boost.setOnClickListener { boostAndLaunch(boost) }
        hero.addView(boost, match().apply { topMargin = dp(16) })
        root.addView(hero, match().apply { setMargins(dp(14), dp(10), dp(14), dp(12)) })
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        grid.addView(metricRow("DEVICE", "${Build.MANUFACTURER} ${Build.MODEL}", "ANDROID ${Build.VERSION.RELEASE}"))
        grid.addView(metricRow("RAM", ramText(), "AVAILABLE MEMORY"))
        grid.addView(metricRow("BATTERY", batteryText(), "SYSTEM BATTERY LEVEL"))
        grid.addView(metricRow("DISPLAY", refreshText(), "DISPLAY REFRESH RATE"))
        root.addView(grid, match().apply { setMargins(dp(14), 0, dp(14), dp(12)) })
        val note = card()
        note.addView(label("OPTIMIZATION STATUS", 14f, lime, true))
        note.addView(label("Safe mode: settings shortcuts and diagnostics only. Android controls CPU/GPU clocks and background process limits.", 12f, muted, false))
        root.addView(note, match().apply { setMargins(dp(14), 0, dp(14), dp(16)) })
    }

    private fun sensiPage() {
        val p = card()
        p.addView(label("SENSITIVITY CONSOLE", 20f, white, true))
        p.addView(label("Build a starting profile. These values are suggestions, not game telemetry or guaranteed headshots.", 12f, muted, false))
        val brands = Spinner(this)
        brands.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf("Samsung", "Xiaomi / Redmi / POCO", "OPPO", "vivo", "realme", "Infinix", "Tecno", "iPhone", "Other"))
        p.addView(label("PHONE BRAND", 11f, lime, true).apply { setPadding(0, dp(12), 0, dp(4)) })
        p.addView(brands, match())
        val ram = Spinner(this)
        ram.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("2 GB", "3–4 GB", "6–8 GB", "12 GB+"))
        p.addView(label("RAM CLASS", 11f, lime, true).apply { setPadding(0, dp(12), 0, dp(4)) })
        p.addView(ram, match())
        val dpi = SeekBar(this).apply { max = 480; progress = 290 }
        val dpiLabel = label("DISPLAY DPI (OPTIONAL): 610", 12f, white, true)
        dpi.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) { dpiLabel.text = "DISPLAY DPI (OPTIONAL): ${320+progress}" }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        p.addView(dpiLabel); p.addView(dpi)
        val result = label("Generate a profile to see suggested values.", 14f, lime, true)
        val generate = button("GENERATE SENSITIVITY")
        generate.setOnClickListener {
            val low = ram.selectedItemPosition == 0 || ram.selectedItemPosition == 1
            val baseVal = if (low) 188 else 178
            result.text = "GENERAL  ${baseVal+8}\nRED DOT  ${baseVal+5}\n2× SCOPE  ${baseVal-2}\n4× SCOPE  ${baseVal-13}\nSNIPER  ${if (low) 112 else 105}\nFREE LOOK  ${baseVal-18}\n\nBrand: ${brands.selectedItem} • DPI: ${320+dpi.progress}"
        }
        p.addView(generate, match().apply { topMargin = dp(12) })
        p.addView(result.apply { setPadding(0, dp(14), 0, dp(8)) })
        val copy = button("COPY SETTINGS")
        copy.setOnClickListener {
            val cb = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cb.setPrimaryClip(android.content.ClipData.newPlainText("DOOMX Sensi", result.text))
            toast("Settings copied")
        }
        p.addView(copy, match())
        root.addView(p, match().apply { setMargins(dp(14), dp(10), dp(14), dp(14)) })
    }

    private fun performancePage() {
        val p = card()
        p.addView(label("LIVE PERFORMANCE", 21f, white, true))
        p.addView(label("Refresh the supported device readings. Actual in-game FPS may not be available to third-party apps.", 12f, muted, false))
        p.addView(metricRow("AVAILABLE RAM", ramText(), "ANDROID MEMORY INFO"))
        p.addView(metricRow("BATTERY", batteryText(), "BATTERY LEVEL"))
        p.addView(metricRow("DISPLAY", refreshText(), "NOT ACTUAL GAME FPS"))
        p.addView(metricRow("THERMAL", "Not exposed by this basic build", "CHECK PHONE SETTINGS"))
        val refresh = button("REFRESH METRICS")
        refresh.setOnClickListener { showPage("Performance") }
        p.addView(refresh, match().apply { topMargin = dp(10) })
        root.addView(p, match().apply { setMargins(dp(14), dp(10), dp(14), dp(14)) })
    }

    private fun networkPage() {
        val p = card()
        p.addView(label("NETWORK ANALYSER", 21f, white, true))
        p.addView(label(networkText(), 16f, lime, true))
        p.addView(label("This starter app checks connection type. Accurate upload/download speed and ping testing need a test endpoint; a network optimizer cannot guarantee lower game ping.", 12f, muted, false))
        val refresh = button("REFRESH CONNECTION")
        refresh.setOnClickListener { showPage("Network") }
        p.addView(refresh, match().apply { topMargin = dp(12) })
        val settings = button("OPEN NETWORK SETTINGS")
        settings.setOnClickListener { safeStart(Intent(Settings.ACTION_WIRELESS_SETTINGS)) }
        p.addView(settings, match().apply { topMargin = dp(8) })
        root.addView(p, match().apply { setMargins(dp(14), dp(10), dp(14), dp(14)) })
    }

    private fun storagePage() {
        val p = card()
        val stat = StatFs(filesDir.absolutePath)
        p.addView(label("STORAGE ANALYSER", 21f, white, true))
        p.addView(metricRow("FREE STORAGE", bytes(stat.availableBytes), "AVAILABLE ON FILESYSTEM"))
        p.addView(metricRow("TOTAL STORAGE", bytes(stat.totalBytes), "FILESYSTEM TOTAL"))
        p.addView(label("Android does not permit a normal app to clear every other app's private cache. Use the system settings to review storage and clear individual app caches yourself.", 12f, muted, false))
        val clear = button("OPEN APP STORAGE SETTINGS")
        clear.setOnClickListener {
            val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = android.net.Uri.parse("package:$packageName") }
            safeStart(i)
        }
        p.addView(clear, match().apply { topMargin = dp(12) })
        val files = button("OPEN DEVICE STORAGE SETTINGS")
        files.setOnClickListener { safeStart(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)) }
        p.addView(files, match().apply { topMargin = dp(8) })
        root.addView(p, match().apply { setMargins(dp(14), dp(10), dp(14), dp(14)) })
    }

    private fun supportPage() {
        val p = card()
        p.addView(label("DOOMX SUPPORT", 22f, lime, true))
        p.addView(label("Email: Doomxcheats@gmail.com", 14f, white, true))
        val mail = button("EMAIL SUPPORT")
        mail.setOnClickListener {
            val i = Intent(Intent.ACTION_SENDTO).apply { data = android.net.Uri.parse("mailto:Doomxcheats@gmail.com") }
            safeStart(i)
        }
        p.addView(mail, match().apply { topMargin = dp(10) })
        val wa = button("OPEN DOOMX WHATSAPP CHANNEL")
        wa.setOnClickListener { openUrl("https://whatsapp.com/channel/0029Vb9POf93WHTQdAtoDg3n") }
        p.addView(wa, match().apply { topMargin = dp(8) })
        val owner = button("OWNER CONSOLE • OWNER CODE REQUIRED")
        owner.setOnClickListener {
            val input = EditText(this).apply {
                hint = "Owner code"
                setHintTextColor(muted)
                setTextColor(white)
                singleLine = true
            }
            android.app.AlertDialog.Builder(this)
                .setTitle("Owner verification")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Continue") { _, _ ->
                    if (input.text.toString() == "doomadmin") showOwner()
                    else toast("Incorrect owner code")
                }.show()
        }
        p.addView(owner, match().apply { topMargin = dp(8) })
        root.addView(p, match().apply { setMargins(dp(14), dp(10), dp(14), dp(14)) })
    }

    private fun boostAndLaunch(button: Button) {
        if (boostInProgress) return
        boostInProgress = true
        button.isEnabled = false
        val anim = AlphaAnimation(0.25f, 1f).apply { duration = 450; repeatCount = 5; repeatMode = AlphaAnimation.REVERSE }
        button.startAnimation(anim)
        button.text = "SCANNING DEVICE • 0%"
        val handler = android.os.Handler(mainLooper)
        val stages = listOf("CHECKING MEMORY • 25%", "CHECKING NETWORK • 50%", "PREPARING SESSION • 75%", "READY • 100%")
        for (i in stages.indices) {
            handler.postDelayed({
                button.text = stages[i]
                if (i == stages.lastIndex) {
                    handler.postDelayed({
                        boostInProgress = false
                        val launch = packageManager.getLaunchIntentForPackage("com.dts.freefireth")
                            ?: packageManager.getLaunchIntentForPackage("com.dts.freefiremax")
                        if (launch != null) safeStart(launch) else {
                            toast("Free Fire not found. Install the official game first.")
                            button.text = "⚡ BOOST & PLAY FREE FIRE"; button.isEnabled = true
                        }
                    }, 600)
                }
            }, (i+1)*650L)
        }
    }

    private fun appRoot(title: String): LinearLayout {
        val r = baseRoot()
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        header.addView(logo(42))
        val names = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10),0,0,0) }
        names.addView(label("DOOMX", 22f, lime, true))
        names.addView(label(title, 9f, muted, true))
        header.addView(names, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val badge = label("● LIVE", 10f, lime, true)
        header.addView(badge)
        r.addView(header)
        val divider = View(this).apply { setBackgroundColor(Color.rgb(55,64,45)) }
        r.addView(divider, LinearLayout.LayoutParams(-1, dp(1)))
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body)
        r.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root = r
        contentBody = body
        setContentView(r)
        return body
    }

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
