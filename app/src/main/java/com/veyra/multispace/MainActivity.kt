package com.veyra.multispace

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date
import kotlin.math.max

data class AppTarget(
    val packageName: String,
    val label: String,
    val userSerial: Long,
    val profileLabel: String,
    val user: UserHandle,
    val icon: android.graphics.drawable.Drawable?,
    val isSystemApp: Boolean
)

data class SpaceItem(
    val id: String,
    val packageName: String,
    val appLabel: String,
    val customName: String,
    val userSerial: Long,
    val profileLabel: String,
    val createdAt: Long
)

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("multispace_local", MODE_PRIVATE) }
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private var currentTab = "home"
    private var darkMode = false
    private var language = "fa"
    private val spaces = mutableListOf<SpaceItem>()
    private val purple = Color.rgb(109, 74, 255)
    private val ink = Color.rgb(34, 29, 51)
    private val muted = Color.rgb(119, 112, 137)
    private val bg get() = if (darkMode) Color.rgb(20, 18, 31) else Color.rgb(246, 244, 250)
    private val cardColor get() = if (darkMode) Color.rgb(39, 35, 55) else Color.WHITE
    private val textColor get() = if (darkMode) Color.rgb(246, 243, 255) else ink

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        darkMode = prefs.getBoolean("dark", false)
        language = prefs.getString("language", "fa") ?: "fa"
        loadSpaces()
        render()
    }

    private fun isFa() = language == "fa"
    private fun tr(fa: String, en: String) = if (isFa()) fa else en

    private fun loadSpaces() {
        spaces.clear()
        val arr = try { JSONArray(prefs.getString("spaces", "[]")) } catch (_: Exception) { JSONArray() }
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            spaces.add(SpaceItem(
                o.optString("id"), o.optString("package"), o.optString("label"),
                o.optString("custom", o.optString("label")), o.optLong("serial"),
                o.optString("profile", "Personal"), o.optLong("created")
            ))
        }
    }

    private fun saveSpaces() {
        val arr = JSONArray()
        spaces.forEach {
            arr.put(JSONObject().put("id", it.id).put("package", it.packageName)
                .put("label", it.appLabel).put("custom", it.customName)
                .put("serial", it.userSerial).put("profile", it.profileLabel)
                .put("created", it.createdAt))
        }
        prefs.edit().putString("spaces", arr.toString()).apply()
    }

    private fun render() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            layoutDirection = if (isFa()) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(20))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(bottomNav(), LinearLayout.LayoutParams(-1, dp(68)))
        setContentView(root)
        when (currentTab) {
            "spaces" -> showSpaces()
            "settings" -> showSettings()
            else -> showHome()
        }
    }

    private fun bottomNav(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(cardColor)
            elevation = dp(10).toFloat()
        }
        val items = listOf(
            Triple("home", "⌂", tr("خانه", "Home")),
            Triple("spaces", "▦", tr("فضاها", "Spaces")),
            Triple("settings", "⚙", tr("تنظیمات", "Settings"))
        )
        items.forEach { (id, icon, label) ->
            val selected = currentTab == id
            val b = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(7), dp(12), dp(5))
                setOnClickListener { currentTab = id; render() }
            }
            b.addView(TextView(this).apply {
                text = icon; textSize = 22f
                setTextColor(if (selected) purple else muted)
                gravity = Gravity.CENTER
            })
            b.addView(TextView(this).apply {
                text = label; textSize = 11f
                setTextColor(if (selected) purple else muted)
                setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            })
            bar.addView(b, LinearLayout.LayoutParams(0, -1, 1f))
        }
        return bar
    }

    private fun showHome() {
        content.addView(TextView(this).apply {
            text = "MultiSpace"; textSize = 13f; setTextColor(purple)
            setTypeface(null, Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = tr("فضاهای دیجیتال، مرتب و مستقل‌تر", "Your apps. Better organized.")
            textSize = 25f; setTextColor(textColor); setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(5), 0, dp(16))
        })
        val summary = card()
        summary.addView(TextView(this).apply {
            text = tr("فضاهای من", "MY SPACES"); textSize = 12f; setTextColor(muted)
        })
        summary.addView(TextView(this).apply {
            text = spaces.size.toString(); textSize = 38f; setTextColor(textColor)
            setTypeface(null, Typeface.BOLD); setPadding(0, dp(3), 0, dp(4))
        })
        summary.addView(TextView(this).apply {
            text = tr("میانبرهای برنامه ذخیره‌شده", "Saved app entries")
            textSize = 13f; setTextColor(muted)
        })
        content.addView(summary)
        content.addView(space(dp(14)))
        val action = button(tr("＋  افزودن برنامه", "＋  Add an app"), true) {
            showAppPicker()
        }
        content.addView(action)
        content.addView(space(dp(18)))
        sectionTitle(tr("دسترسی سریع", "QUICK ACCESS"))
        if (spaces.isEmpty()) {
            infoCard(
                tr("هنوز فضایی نساخته‌ای", "No spaces yet"),
                tr("با افزودن برنامه، یک ورودی دسترسی سریع برای آن ایجاد می‌شود.", "Add an installed app to create a quick-access entry.")
            )
        } else {
            spaces.take(4).forEach { content.addView(spaceRow(it)) }
        }
        content.addView(space(dp(12)))
        sectionTitle(tr("وضعیت جداسازی", "ISOLATION STATUS"))
        infoCard(
            tr("وابسته به پروفایل اندروید", "Android profile dependent"),
            tr("این برنامه خودش اپ‌ها را شبیه‌سازی یا کلون نمی‌کند. برای جداسازی واقعی، برنامه باید در پروفایل کاری اندروید نیز نصب شده باشد.", "This app does not fake app cloning. Real separation requires the app to be installed in a separate Android profile, such as a supported Work Profile.")
        )
        content.addView(space(dp(12)))
        content.addView(button(tr("بررسی اطلاعات دستگاه", "Check device information"), false) {
            showDeviceInfo()
        })
    }

    private fun showSpaces() {
        pageHeader(tr("فضاهای من", "My Spaces"),
            tr("برنامه‌ها و پروفایل‌های قابل دسترسی را مدیریت کن.", "Manage app entries and accessible profiles."))
        content.addView(button(tr("＋  افزودن برنامه", "＋  Add app"), true) { showAppPicker() })
        content.addView(space(dp(12)))
        if (spaces.isEmpty()) {
            infoCard(tr("لیست خالی است", "Your list is empty"),
                tr("برای شروع، یک برنامه نصب‌شده را اضافه کن.", "Add an installed app to get started."))
        } else {
            spaces.forEach { content.addView(spaceRow(it, true)) }
        }
    }

    private fun showSettings() {
        pageHeader(tr("تنظیمات", "Settings"),
            tr("شخصی‌سازی و مدیریت MultiSpace Ultimate", "Personalize and manage MultiSpace Ultimate"))
        settingRow("🌐", tr("زبان", "Language"), if (isFa()) "فارسی" else "English") {
            AlertDialog.Builder(this)
                .setTitle(tr("انتخاب زبان", "Choose language"))
                .setSingleChoiceItems(arrayOf("فارسی", "English"), if (isFa()) 0 else 1) { d, which ->
                    language = if (which == 0) "fa" else "en"
                    prefs.edit().putString("language", language).apply()
                    d.dismiss(); render()
                }.setNegativeButton(tr("بستن", "Close"), null).show()
        }
        settingRow("◐", tr("ظاهر برنامه", "Appearance"),
            if (darkMode) tr("حالت تیره", "Dark mode") else tr("حالت روشن", "Light mode")) {
            darkMode = !darkMode
            prefs.edit().putBoolean("dark", darkMode).apply()
            render()
        }
        settingRow("▣", tr("اطلاعات دستگاه", "Device information"), tr("مشاهده مشخصات", "View details")) {
            showDeviceInfo()
        }
        settingRow("🔒", tr("امنیت", "Security"), tr("قفل صفحه دستگاه", "Device screen lock")) {
            try { startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) } catch (_: Exception) {
                toast(tr("تنظیمات امنیتی در دسترس نیست.", "Security settings unavailable."))
            }
        }
        settingRow("ⓘ", tr("درباره برنامه", "About"), "MultiSpace Ultimate 0.3.0") {
            AlertDialog.Builder(this).setTitle("MultiSpace Ultimate")
                .setMessage(tr(
                    "نسخه 0.3.0\nمدیر محلی فضاها و میانبرهای برنامه.\nبدون حساب کاربری و بدون تبلیغات.\n\nاین نسخه موتور کلون‌سازی عمومی ندارد؛ جداسازی واقعی وابسته به قابلیت پروفایل اندروید است.",
                    "Version 0.3.0\nLocal space and app-entry manager.\nNo account and no ads.\n\nThis release does not include a universal cloning engine. Real isolation depends on Android profile support."
                )).setPositiveButton("OK", null).show()
        }
        content.addView(space(dp(12)))
        infoCard(tr("حریم خصوصی", "Privacy"),
            tr("اطلاعات فضاها فقط به‌صورت محلی در حافظه برنامه ذخیره می‌شود. این نسخه هیچ حساب آنلاین یا سرویس ثبت‌نامی ندارد.",
                "Space entries are stored locally on this device. This version has no online account or registration service."))
    }

    private fun showAppPicker() {
        val targets = getLaunchableApps()
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        val search = EditText(this).apply {
            hint = tr("جست‌وجوی برنامه...", "Search apps...")
            singleLine = true
        }
        dialogLayout.addView(search)
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroller = ScrollView(this).apply { addView(list) }
        dialogLayout.addView(tabs)
        dialogLayout.addView(scroller, LinearLayout.LayoutParams(-1, dp(390)))
        val dialog = AlertDialog.Builder(this)
            .setTitle(tr("افزودن برنامه", "Add an app"))
            .setView(dialogLayout)
            .setNegativeButton(tr("بستن", "Close"), null)
            .create()

        var showSystemApps = false
        fun populate(filter: String = "") {
            list.removeAllViews()
            val q = filter.trim().lowercase()
            val filtered = targets.filter {
                it.isSystemApp == showSystemApps &&
                    (it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q))
            }
            if (filtered.isEmpty()) {
                list.addView(TextView(this).apply {
                    text = tr("برنامه‌ای پیدا نشد.", "No apps found.")
                    setTextColor(muted); setPadding(0, dp(20), 0, dp(20))
                })
            }
            filtered.forEach { target ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(5), dp(9), dp(5), dp(9))
                    setOnClickListener {
                        addSpace(target)
                        dialog.dismiss()
                        currentTab = "spaces"; render()
                    }
                }
                val icon = ImageView(this).apply {
                    setImageDrawable(target.icon)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
                row.addView(icon, LinearLayout.LayoutParams(dp(42), dp(42)))
                val labels = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(10), 0, dp(8), 0)
                }
                labels.addView(TextView(this).apply {
                    text = target.label; textSize = 14f; setTextColor(textColor)
                    setTypeface(null, Typeface.BOLD)
                })
                labels.addView(TextView(this).apply {
                    text = "${target.profileLabel} · ${target.packageName}"
                    textSize = 10f; setTextColor(muted)
                })
                row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(TextView(this).apply {
                    text = "＋"; textSize = 22f; setTextColor(purple)
                })
                list.addView(row)
                list.addView(View(this).apply { setBackgroundColor(if (darkMode) 0x33FFFFFF else 0x11000000) },
                    LinearLayout.LayoutParams(-1, dp(1)))
            }
        }
        val installed = button(tr("برنامه‌های نصب‌شده", "Installed apps"), false) {
            showSystemApps = false
            populate(search.text.toString())
        }
        val system = button(tr("برنامه‌های سیستمی", "System apps"), false) {
            showSystemApps = true
            populate(search.text.toString())
        }
        val discover = button(tr("یافتن در Play Store", "Discover in Play Store"), false) {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=apps"))) }
            catch (_: Exception) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=apps"))) }
        }
        tabs.addView(installed, LinearLayout.LayoutParams(0, dp(42), 1f))
        tabs.addView(system, LinearLayout.LayoutParams(0, dp(42), 1f))
        tabs.addView(discover, LinearLayout.LayoutParams(0, dp(42), 1f))
        search.addTextChangedListener(SimpleTextWatcher { populate(it) })
        populate()
        dialog.show()
    }

    private fun getLaunchableApps(): List<AppTarget> {
        val result = mutableListOf<AppTarget>()
        val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val userManager = getSystemService(Context.USER_SERVICE) as UserManager
        val profiles = try { launcherApps.profiles } catch (_: Exception) { listOf(Process.myUserHandle()) }
        profiles.forEach { user ->
            val serial = try { userManager.getSerialNumberForUser(user) } catch (_: Exception) { 0L }
            val profile = if (user == Process.myUserHandle()) tr("شخصی", "Personal") else tr("پروفایل دیگر", "Other profile")
            try {
                launcherApps.getActivityList(null, user).forEach { info ->
                    val appInfo = info.applicationInfo
                    val label = info.label?.toString() ?: appInfo.loadLabel(packageManager).toString()
                    val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    result.add(AppTarget(appInfo.packageName, label, serial, profile, user, info.getBadgedIcon(0), isSystem))
                }
            } catch (_: SecurityException) { }
        }
        return result.distinctBy { "${it.packageName}:${it.userSerial}" }
            .sortedWith(compareBy({ it.label.lowercase() }, { it.profileLabel }))
    }

    private fun addSpace(target: AppTarget) {
        val entry = SpaceItem(
            System.currentTimeMillis().toString(), target.packageName, target.label,
            target.label, target.userSerial, target.profileLabel, System.currentTimeMillis()
        )
        spaces.add(0, entry)
        saveSpaces()
        toast(tr("برنامه به فضاها اضافه شد.", "App entry added."))
    }

    private fun spaceRow(item: SpaceItem, actions: Boolean = false): View {
        val row = card()
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val icon = ImageView(this)
        try {
            val userManager = getSystemService(Context.USER_SERVICE) as UserManager
            val user = userManager.getUserForSerialNumber(item.userSerial)
            val launcher = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val activity = launcher.getActivityList(item.packageName, user).firstOrNull()
            icon.setImageDrawable(activity?.getBadgedIcon(0) ?: packageManager.getApplicationIcon(item.packageName))
        } catch (_: Exception) { icon.setImageResource(android.R.drawable.sym_def_app_icon) }
        row.addView(icon, LinearLayout.LayoutParams(dp(42), dp(42)))
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), 0, dp(6), 0)
        }
        texts.addView(TextView(this).apply {
            text = item.customName; textSize = 15f; setTextColor(textColor); setTypeface(null, Typeface.BOLD)
        })
        texts.addView(TextView(this).apply {
            text = "${item.profileLabel} · ${item.packageName}"
            textSize = 10f; setTextColor(muted)
        })
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        val launch = TextView(this).apply {
            text = "↗"; textSize = 23f; setTextColor(purple); gravity = Gravity.CENTER
            setPadding(dp(9), dp(5), dp(9), dp(5))
            setOnClickListener { launchEntry(item) }
        }
        row.addView(launch)
        if (actions) {
            val more = TextView(this).apply {
                text = "⋮"; textSize = 24f; setTextColor(muted)
                setPadding(dp(8), dp(5), dp(5), dp(5))
                setOnClickListener { showSpaceActions(item) }
            }
            row.addView(more)
        }
        row.setOnClickListener { launchEntry(item) }
        return row
    }

    private fun launchEntry(item: SpaceItem) {
        try {
            val um = getSystemService(Context.USER_SERVICE) as UserManager
            val user = um.getUserForSerialNumber(item.userSerial)
                ?: throw IllegalStateException("Profile is no longer available")
            val launcher = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val activity = launcher.getActivityList(item.packageName, user).firstOrNull()
                ?: throw IllegalStateException("App is not installed in this profile")
            launcher.startMainActivity(activity.componentName, user, null, null)
        } catch (_: Exception) {
            toast(tr("این برنامه در پروفایل انتخاب‌شده نصب یا قابل اجرا نیست.", "This app is not installed or cannot be launched in the selected profile."))
        }
    }

    private fun showSpaceActions(item: SpaceItem) {
        val options = arrayOf(tr("تغییر نام", "Rename"), tr("حذف از فضاها", "Remove from spaces"), tr("اطلاعات", "Details"))
        AlertDialog.Builder(this).setTitle(item.customName).setItems(options) { _, which ->
            when (which) {
                0 -> {
                    val input = EditText(this).apply { setText(item.customName) }
                    AlertDialog.Builder(this).setTitle(tr("نام جدید", "New name")).setView(input)
                        .setNegativeButton(tr("لغو", "Cancel"), null)
                        .setPositiveButton(tr("ذخیره", "Save")) { _, _ ->
                            val index = spaces.indexOfFirst { it.id == item.id }
                            if (index >= 0) spaces[index] = item.copy(customName = input.text.toString().ifBlank { item.appLabel })
                            saveSpaces(); render()
                        }.show()
                }
                1 -> {
                    spaces.removeAll { it.id == item.id }; saveSpaces(); render()
                }
                2 -> AlertDialog.Builder(this).setMessage(
                    "${item.appLabel}\n${item.packageName}\n${item.profileLabel}\n" +
                    DateFormat.getDateTimeInstance().format(Date(item.createdAt))
                ).setPositiveButton("OK", null).show()
            }
        }.show()
    }

    private fun showDeviceInfo() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val mem = android.app.ActivityManager.MemoryInfo()
        am.getMemoryInfo(mem)
        val um = getSystemService(Context.USER_SERVICE) as UserManager
        val profiles = try { (getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps).profiles.size } catch (_: Exception) { 1 }
        val msg = """
            Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})
            Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
            CPU ABI: ${android.os.Build.SUPPORTED_ABIS.joinToString()}
            RAM available: ${mem.availMem / (1024 * 1024)} MB
            RAM total: ${mem.totalMem / (1024 * 1024)} MB
            Accessible profiles: $profiles
            App version: 0.3.0
        """.trimIndent()
        AlertDialog.Builder(this).setTitle(tr("اطلاعات دستگاه", "Device information"))
            .setMessage(msg).setPositiveButton("OK", null).show()
    }

    private fun pageHeader(title: String, subtitle: String) {
        content.addView(TextView(this).apply {
            text = title; textSize = 26f; setTextColor(textColor)
            setTypeface(null, Typeface.BOLD); setPadding(0, dp(4), 0, dp(4))
        })
        content.addView(TextView(this).apply {
            text = subtitle; textSize = 13f; setTextColor(muted); setPadding(0, 0, 0, dp(16))
        })
    }

    private fun sectionTitle(title: String) {
        content.addView(TextView(this).apply {
            text = title; textSize = 11f; setTextColor(muted)
            setTypeface(null, Typeface.BOLD); setPadding(0, dp(2), 0, dp(9))
        })
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(15), dp(16), dp(15))
        background = rounded(cardColor, 20)
        elevation = dp(2).toFloat()
    }

    private fun infoCard(title: String, body: String) {
        val c = card()
        c.addView(TextView(this).apply {
            text = title; textSize = 15f; setTextColor(textColor); setTypeface(null, Typeface.BOLD)
        })
        c.addView(TextView(this).apply {
            text = body; textSize = 12f; setTextColor(muted); setPadding(0, dp(7), 0, 0)
        })
        content.addView(c)
    }

    private fun button(label: String, primary: Boolean, action: () -> Unit): View {
        return TextView(this).apply {
            text = label; textSize = 14f; gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (primary) Color.WHITE else purple)
            background = rounded(if (primary) purple else cardColor, 15)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setOnClickListener { action() }
        }
    }

    private fun settingRow(icon: String, title: String, value: String, action: () -> Unit) {
        val row = card().apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(13))
            setOnClickListener { action() }
        }
        row.addView(TextView(this).apply {
            text = icon; textSize = 22f; setTextColor(purple)
        })
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, dp(5), 0)
        }
        labels.addView(TextView(this).apply { text = title; textSize = 14f; setTextColor(textColor); setTypeface(null, Typeface.BOLD) })
        labels.addView(TextView(this).apply { text = value; textSize = 11f; setTextColor(muted); setPadding(0, dp(3), 0, 0) })
        row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(TextView(this).apply { text = "›"; textSize = 23f; setTextColor(muted) })
        content.addView(row)
        content.addView(space(dp(9)))
    }

    private fun space(height: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, height)
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

private class SimpleTextWatcher(val changed: (String) -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed(s?.toString() ?: "") }
    override fun afterTextChanged(s: android.text.Editable?) {}
}
