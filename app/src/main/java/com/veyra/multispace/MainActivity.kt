package com.veyra.multispace

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private data class AppTarget(
    val packageName: String, val label: String, val serial: Long,
    val profile: String, val user: UserHandle,
    val icon: android.graphics.drawable.Drawable?, val system: Boolean
)
private data class AppEntry(
    val id: String, val packageName: String, val appLabel: String,
    var customName: String, val serial: Long, val profile: String,
    val instanceNo: Int, var spaceNo: Int, var position: Int
)

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("multispace_local", MODE_PRIVATE) }
    private val entries = mutableListOf<AppEntry>()
    private val spaceNames = mutableMapOf<Int, String>()
    private val appCounters = mutableMapOf<String, Int>()
    private var tab = "home"
    private var dark = false
    private var lang = "fa"
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private val accent = Color.rgb(117, 83, 255)
    private val ink = Color.rgb(35, 31, 48)
    private val muted = Color.rgb(132, 126, 148)
    private val bg get() = if (dark) Color.rgb(18, 17, 29) else Color.rgb(246, 244, 251)
    private val panel get() = if (dark) Color.rgb(39, 36, 54) else Color.WHITE
    private val fg get() = if (dark) Color.rgb(248, 246, 255) else ink
    private fun fa() = lang == "fa"
    private fun tr(f: String, e: String) = if (fa()) f else e

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dark = prefs.getBoolean("dark", false)
        lang = prefs.getString("lang", "fa") ?: "fa"
        load()
        render()
    }

    private fun load() {
        entries.clear(); spaceNames.clear(); appCounters.clear()
        val a = try { JSONArray(prefs.getString("entries", "[]")) } catch (_: Exception) { JSONArray() }
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            entries.add(AppEntry(o.optString("id"), o.optString("pkg"), o.optString("label"),
                o.optString("custom", o.optString("label")), o.optLong("serial"),
                o.optString("profile"), o.optInt("instance", 1), o.optInt("space", 1), o.optInt("pos", i)))
        }
        val n = try { JSONObject(prefs.getString("space_names", "{}")) } catch (_: Exception) { JSONObject() }
        n.keys().forEach { k -> spaceNames[k.toIntOrNull() ?: return@forEach] = n.optString(k) }
        val c = try { JSONObject(prefs.getString("app_counters", "{}")) } catch (_: Exception) { JSONObject() }
        c.keys().forEach { k -> appCounters[k] = c.optInt(k, 0) }
    }

    private fun save() {
        val a = JSONArray()
        entries.forEach { e -> a.put(JSONObject().put("id",e.id).put("pkg",e.packageName)
            .put("label",e.appLabel).put("custom",e.customName).put("serial",e.serial)
            .put("profile",e.profile).put("instance",e.instanceNo).put("space",e.spaceNo).put("pos",e.position)) }
        val n = JSONObject(); spaceNames.forEach { (k,v) -> n.put(k.toString(),v) }
        val c = JSONObject(); appCounters.forEach { (k,v) -> c.put(k,v) }
        prefs.edit().putString("entries",a.toString()).putString("space_names",n.toString())
            .putString("app_counters",c.toString()).apply()
    }

    private fun render() {
        root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(bg)
            layoutDirection=if(fa()) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR }
        val scroll = ScrollView(this).apply { isFillViewport=true }
        content = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(18),dp(18),dp(18),dp(22)) }
        scroll.addView(content)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        root.addView(nav(),LinearLayout.LayoutParams(-1,dp(66)))
        setContentView(root)
        when(tab) { "spaces" -> showSpaces(); "settings" -> showSettings(); else -> showHome() }
    }

    private fun nav(): View {
        val bar=LinearLayout(this).apply { gravity=Gravity.CENTER; setBackgroundColor(panel); elevation=dp(9).toFloat() }
        listOf(Triple("home","⌂",tr("خانه","Home")),Triple("spaces","▦",tr("فضاها","Spaces")),Triple("settings","⚙",tr("تنظیمات","Settings"))).forEach { (id,ic,label) ->
            val item=LinearLayout(this).apply { orientation=1; gravity=Gravity.CENTER; setOnClickListener { tab=id; render() } }
            item.addView(TextView(this).apply { text=ic; textSize=23f; gravity=Gravity.CENTER; setTextColor(if(tab==id) accent else muted) })
            item.addView(TextView(this).apply { text=label; textSize=11f; setTextColor(if(tab==id) accent else muted); setTypeface(null,if(tab==id) Typeface.BOLD else Typeface.NORMAL) })
            bar.addView(item,LinearLayout.LayoutParams(0,-1,1f))
        }
        return bar
    }

    private fun heading(title:String, subtitle:String) {
        content.addView(TextView(this).apply { text=title; textSize=26f; setTextColor(fg); setTypeface(null,Typeface.BOLD) })
        content.addView(TextView(this).apply { text=subtitle; textSize=12f; setTextColor(muted); setPadding(0,dp(5),0,dp(16)) })
    }

    private fun showHome() {
        heading("MultiSpace Ultimate",tr("مدیریت هوشمند فضاهای برنامه","Organize your app spaces"))
        val count=spaceNumbers().size
        val total=entries.size
        val stats=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        stats.addView(stat(tr("فضاها","Spaces"),count.toString()),LinearLayout.LayoutParams(0,dp(108),1f))
        stats.addView(space(dp(10)))
        stats.addView(stat(tr("نمونه‌ها","Entries"),total.toString()),LinearLayout.LayoutParams(0,dp(108),1f))
        content.addView(stats)
        content.addView(space(dp(14)))
        section(tr("وضعیت موتور جداسازی","ISOLATION ENGINE"))
        info(tr("نیازمند پروفایل اندروید","Android profile required"),
            tr("این نسخه هنوز موتور مجازی‌سازی چندنمونه‌ای ندارد. فقط برنامه‌هایی را که واقعاً در پروفایل‌های قابل‌دسترسی اندروید نصب شده‌اند، جداگانه اجرا می‌کند.",
                "This release does not include a multi-instance virtualization engine. It launches apps installed in Android profiles that are actually accessible."))
        content.addView(space(dp(14)))
        section(tr("فعالیت و دسترسی سریع","QUICK ACCESS"))
        entries.take(4).forEach { entryRow(it) }
        if(entries.isEmpty()) info(tr("هنوز برنامه‌ای اضافه نشده","No apps added yet"),tr("از صفحه فضاها روی + بزن و یک برنامه انتخاب کن.","Open Spaces and tap + to add an app."))
        content.addView(space(dp(12)))
        button(tr("رفتن به فضاها  →","Go to Spaces  →"),true) { tab="spaces"; render() }
    }

    private fun showSpaces() {
        heading(tr("فضاهای من","My Spaces"),tr("چیدمان شبکه‌ای شبیه صفحه اصلی گوشی؛ برای جابه‌جایی، آیکون را نگه دار و بکش.","Phone-style icon grid. Long-press an icon and drag it to rearrange."))
        val nums=spaceNumbers()
        if(nums.isEmpty()) info(tr("فضاها هنوز خالی هستند","No spaces yet"),tr("با افزودن اولین برنامه، فضای 1 ساخته می‌شود.","Adding your first app creates Space 1."))
        nums.forEach { n -> content.addView(spaceCard(n)) ; content.addView(space(dp(12))) }
        val add=TextView(this).apply {
            text=tr("＋  افزودن برنامه","＋  Add app"); textSize=16f; gravity=Gravity.CENTER
            setTextColor(Color.WHITE); setTypeface(null,Typeface.BOLD); background=round(accent,18)
            elevation=dp(5).toFloat(); setPadding(dp(18),dp(16),dp(18),dp(16))
            setOnClickListener { appPicker() }
        }
        val lp=LinearLayout.LayoutParams(dp(190),dp(54)); lp.gravity=if(fa()) Gravity.LEFT else Gravity.RIGHT
        content.addView(add,lp)
    }

    private fun spaceCard(number:Int): View {
        val card=cardView()
        val header=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text=spaceNames[number] ?: tr("فضای $number","Space $number")
            textSize=18f; setTextColor(fg); setTypeface(null,Typeface.BOLD)
            setOnLongClickListener { renameSpace(number); true }
            setOnClickListener { renameSpace(number) }
        },LinearLayout.LayoutParams(0,-2,1f))
        header.addView(TextView(this).apply { text="${entries.count{it.spaceNo==number}}"; textSize=12f; setTextColor(muted) })
        card.addView(header)
        val grid=GridLayout(this).apply { columnCount=4; useDefaultMargins=false; setPadding(0,dp(12),0,dp(4)) }
        grid.setOnDragListener { _,event ->
            if(event.action==DragEvent.ACTION_DROP) {
                val id=event.localState as? String
                val e=entries.firstOrNull{it.id==id}
                if(e!=null) { e.spaceNo=number; e.position=entries.count{it.spaceNo==number}; save(); render() }
                true
            } else event.action==DragEvent.ACTION_DRAG_STARTED || event.action==DragEvent.ACTION_DRAG_ENTERED || event.action==DragEvent.ACTION_DRAG_LOCATION || event.action==DragEvent.ACTION_DRAG_EXITED
        }
        val sorted=entries.filter{it.spaceNo==number}.sortedBy{it.position}
        sorted.forEachIndexed { index,e ->
            val tile=appTile(e)
            val params=GridLayout.LayoutParams().apply { width=0; height=dp(104); columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); setMargins(dp(3),dp(3),dp(3),dp(3)) }
            tile.setOnLongClickListener {
                val clip=ClipData.newPlainText("entry",e.id)
                tile.startDragAndDrop(clip,View.DragShadowBuilder(tile),e.id,0); true
            }
            tile.setOnDragListener { _,ev ->
                if(ev.action==DragEvent.ACTION_DROP) {
                    val id=ev.localState as? String
                    val moving=entries.firstOrNull{it.id==id}
                    if(moving!=null && moving.id!=e.id) {
                        val target=e.position
                        val oldSpace=moving.spaceNo
                        if(oldSpace==number) entries.filter{it.spaceNo==number && it.id!=moving.id && it.position>=target}.forEach{it.position++}
                        moving.spaceNo=number; moving.position=target
                        save(); render()
                    }; true
                } else ev.action==DragEvent.ACTION_DRAG_STARTED || ev.action==DragEvent.ACTION_DRAG_ENTERED || ev.action==DragEvent.ACTION_DRAG_LOCATION || ev.action==DragEvent.ACTION_DRAG_EXITED
            }
            grid.addView(tile,params)
        }
        card.addView(grid)
        card.addView(TextView(this).apply {
            text=tr("برای تغییر نام فضا، عنوان را چند لحظه نگه دار.","Long-press the space title to rename it.")
            textSize=10f; setTextColor(muted); setPadding(0,dp(5),0,0)
        })
        return card
    }

    private fun appTile(e:AppEntry): View {
        val tile=LinearLayout(this).apply { orientation=1; gravity=Gravity.CENTER; setPadding(dp(4),dp(6),dp(4),dp(5)); background=round(if(dark) Color.rgb(48,44,66) else Color.rgb(249,248,253),14) }
        val icon=ImageView(this)
        icon.setImageDrawable(getIcon(e))
        tile.addView(icon,LinearLayout.LayoutParams(dp(42),dp(42)))
        tile.addView(TextView(this).apply { text=e.customName; textSize=10.5f; maxLines=2; gravity=Gravity.CENTER; setTextColor(fg); setPadding(0,dp(4),0,0) },LinearLayout.LayoutParams(-1,0,1f))
        tile.addView(TextView(this).apply {
            text="⋮"; textSize=15f; gravity=Gravity.CENTER; setTextColor(muted)
            setOnClickListener { showEntryMenu(e) }
        },LinearLayout.LayoutParams(-1,dp(20)))
        tile.setOnClickListener { launch(e) }
        return tile
    }

    private fun getIcon(e:AppEntry): android.graphics.drawable.Drawable? = try {
        val um=getSystemService(Context.USER_SERVICE) as UserManager
        val user=um.getUserForSerialNumber(e.serial) ?: Process.myUserHandle()
        val la=getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        la.getActivityList(e.packageName,user).firstOrNull()?.getBadgedIcon(0)
            ?: packageManager.getApplicationIcon(e.packageName)
    } catch(_:Exception) { getDrawable(android.R.drawable.sym_def_app_icon) }

    private fun entryRow(e:AppEntry): View {
        val row=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL; background=round(panel,14); setPadding(dp(12),dp(9),dp(12),dp(9)) }
        val icon=ImageView(this).apply { setImageDrawable(getIcon(e)) }
        row.addView(icon,LinearLayout.LayoutParams(dp(38),dp(38)))
        row.addView(TextView(this).apply { text=e.customName; textSize=13f; setTextColor(fg); setPadding(dp(10),0,0,0) },LinearLayout.LayoutParams(0,-2,1f))
        row.addView(TextView(this).apply { text="↗"; textSize=20f; setTextColor(accent); setOnClickListener{launch(e)} })
        row.setOnClickListener{launch(e)}
        row.setOnLongClickListener{showEntryMenu(e);true}
        content.addView(row); content.addView(space(dp(7)))
        return row
    }

    private fun showEntryMenu(e:AppEntry) {
        val opts=arrayOf(tr("تغییر نام نمایشی","Rename display name"),tr("انتقال به فضای دیگر","Move to another Space"),tr("اجرای برنامه","Launch app"),tr("حذف ورودی","Remove entry"),tr("اطلاعات برنامه","App details"))
        AlertDialog.Builder(this).setTitle(e.customName).setItems(opts){_,which-> when(which){
            0 -> { val input=EditText(this).apply{setText(e.customName)}; AlertDialog.Builder(this).setTitle(tr("نام نمایشی جدید","New display name")).setView(input).setNegativeButton(tr("لغو","Cancel"),null).setPositiveButton(tr("ذخیره","Save")){_,_->e.customName=input.text.toString().ifBlank{e.appLabel};save();render()}.show() }
            1 -> { val nums=(1..maxOf(1,(spaceNumbers().maxOrNull()?:1)+1)).map{it.toString()}.toTypedArray(); AlertDialog.Builder(this).setTitle(tr("انتخاب فضا","Choose Space")).setItems(nums){_,i->e.spaceNo=nums[i].toInt();e.position=entries.count{it.spaceNo==e.spaceNo};save();render()}.show() }
            2 -> launch(e)
            3 -> { entries.removeAll{it.id==e.id};save();render() }
            4 -> { try{startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${e.packageName}")))}catch(_:Exception){} }
        }}.show()
    }

    private fun renameSpace(n:Int) {
        val input=EditText(this).apply{setText(spaceNames[n]?:tr("فضای $n","Space $n"))}
        AlertDialog.Builder(this).setTitle(tr("تغییر نام فضا","Rename Space")).setView(input)
            .setNegativeButton(tr("لغو","Cancel"),null).setPositiveButton(tr("ذخیره","Save")){_,_->spaceNames[n]=input.text.toString().ifBlank{tr("فضای $n","Space $n")};save();render()}.show()
    }

    private fun appPicker() {
        val all=getApps()
        var mode=0
        val layout=LinearLayout(this).apply{orientation=1;setPadding(dp(12),dp(8),dp(12),dp(8))}
        val search=EditText(this).apply{hint=tr("جست‌وجوی برنامه‌ها","Search apps");setSingleLine(true)}
        layout.addView(search)
        val tabs=LinearLayout(this).apply{orientation=0}
        val list=LinearLayout(this).apply{orientation=1}
        val scroll=ScrollView(this).apply{addView(list)}
        layout.addView(tabs);layout.addView(scroll,LinearLayout.LayoutParams(-1,dp(380)))
        val dlg=AlertDialog.Builder(this).setTitle(tr("انتخاب برنامه","Choose an app")).setView(layout).setNegativeButton(tr("بستن","Close"),null).create()
        fun fill(q:String=""){
            list.removeAllViews()
            val found=all.filter{it.system==(mode==1) && (it.label.contains(q,true)||it.packageName.contains(q,true))}
            found.forEach{a->
                val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(6),dp(8),dp(6),dp(8))}
                row.addView(ImageView(this).apply{setImageDrawable(a.icon)},LinearLayout.LayoutParams(dp(40),dp(40)))
                row.addView(TextView(this).apply{text="${a.label}\n${a.profile}";textSize=12f;setTextColor(fg);setPadding(dp(9),0,dp(6),0)},LinearLayout.LayoutParams(0,-2,1f))
                row.addView(TextView(this).apply{text="＋";textSize=22f;setTextColor(accent)})
                row.setOnClickListener{addApp(a);dlg.dismiss();tab="spaces";render()}
                list.addView(row)
            }
            if(found.isEmpty())list.addView(TextView(this).apply{text=tr("برنامه‌ای پیدا نشد","No apps found");setTextColor(muted);setPadding(dp(10),dp(20),dp(10),dp(20))})
        }
        listOf(tr("نصب‌شده‌ها","Installed"),tr("سیستمی","System"),tr("کشف برنامه","Discover")).forEachIndexed{idx,label->
            tabs.addView(TextView(this).apply{text=label;textSize=11f;gravity=Gravity.CENTER;setTextColor(accent);setPadding(dp(7),dp(12),dp(7),dp(12));setOnClickListener{
                if(idx==2){try{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("market://search?q=apps")))}catch(_:Exception){startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/search?q=apps")))}}
                else{mode=idx;fill(search.text.toString())}
            }},LinearLayout.LayoutParams(0,dp(42),1f))
        }
        search.addTextChangedListener(Watcher{fill(it)})
        fill();dlg.show()
    }

    private fun getApps():List<AppTarget>{
        val out=mutableListOf<AppTarget>()
        val la=getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val um=getSystemService(Context.USER_SERVICE) as UserManager
        val profiles=try{la.profiles}catch(_:Exception){listOf(Process.myUserHandle())}
        profiles.forEach{u->
            val serial=try{um.getSerialNumberForUser(u)}catch(_:Exception){0L}
            val profile=if(u==Process.myUserHandle())tr("شخصی","Personal") else tr("پروفایل دیگر","Other profile")
            try{la.getActivityList(null,u).forEach{a->
                val app=a.applicationInfo
                out.add(AppTarget(app.packageName,a.label?.toString()?:app.loadLabel(packageManager).toString(),serial,profile,u,a.getBadgedIcon(0),(app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM)!=0))
            }}catch(_:Exception){}
        }
        return out.distinctBy{"${it.packageName}:${it.serial}"}.sortedBy{it.label.lowercase()}
    }

    private fun addApp(a:AppTarget){
        val counterKey="${a.packageName}:${a.serial}"
        val number=(appCounters[counterKey] ?: entries.filter{it.packageName==a.packageName&&it.serial==a.serial}.maxOfOrNull{it.instanceNo} ?: 0)+1
        appCounters[counterKey]=number
        // Product rule: the Nth selection of an app belongs in Space N.
        entries.add(AppEntry(UUID.randomUUID().toString(),a.packageName,a.label,a.label,a.serial,a.profile,number,number,0))
        entries.filter{it.spaceNo==number}.forEachIndexed{idx,e->e.position=idx}
        save()
        toast(tr("ورودی ثبت شد؛ این مورد در فضای $number قرار گرفت. این کار کلون مستقل نصب نمی‌کند.","Entry added to Space $number. This does not install an independent clone."))
    }

    private fun launch(e:AppEntry){
        try{
            val um=getSystemService(Context.USER_SERVICE) as UserManager
            val user=um.getUserForSerialNumber(e.serial)?:throw Exception()
            val la=getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val activity=la.getActivityList(e.packageName,user).firstOrNull()?:throw Exception()
            la.startMainActivity(activity.componentName,user,null,null)
        }catch(_:Exception){toast(tr("برنامه در این پروفایل نصب یا قابل اجرا نیست.","App is not installed or cannot launch in this profile."))}
    }

    private fun showSettings(){
        heading(tr("تنظیمات","Settings"),tr("شخصی‌سازی MultiSpace Ultimate","Personalize MultiSpace Ultimate"))
        setting("🌐",tr("زبان","Language"),if(fa())"فارسی" else "English"){
            AlertDialog.Builder(this).setTitle(tr("انتخاب زبان","Choose language")).setItems(arrayOf("فارسی","English")){_,i->lang=if(i==0)"fa" else "en";prefs.edit().putString("lang",lang).apply();render()}.show()
        }
        setting("◐",tr("ظاهر","Appearance"),if(dark)tr("تیره","Dark") else tr("روشن","Light")){dark=!dark;prefs.edit().putBoolean("dark",dark).apply();render()}
        setting("▣",tr("اطلاعات دستگاه","Device information"),tr("مشاهده منابع و نسخه اندروید","View Android and resources")){deviceInfo()}
        setting("🔐",tr("امنیت دستگاه","Device security"),tr("تنظیمات امنیتی اندروید","Android security settings")){startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))}
        setting("ⓘ",tr("درباره","About"),"MultiSpace Ultimate 1.0.0"){
            AlertDialog.Builder(this).setTitle("MultiSpace Ultimate 1.0.0").setMessage(tr("مدیر شبکه‌ای فضاها و میانبرهای پروفایل. موتور کلون‌سازی عمومی هنوز پیاده‌سازی نشده است.","Grid-based profile-aware app organizer. A universal cloning engine is not implemented in this release.")).setPositiveButton("OK",null).show()
        }
    }

    private fun deviceInfo(){
        val am=getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val m=android.app.ActivityManager.MemoryInfo();am.getMemoryInfo(m)
        val msg="Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\nRAM ${m.availMem/1048576} MB free / ${m.totalMem/1048576} MB\nCPU ${android.os.Build.SUPPORTED_ABIS.joinToString()}\nApp 1.0.0"
        AlertDialog.Builder(this).setTitle(tr("اطلاعات دستگاه","Device information")).setMessage(msg).setPositiveButton("OK",null).show()
    }

    private fun stat(title:String,value:String)=LinearLayout(this).apply{
        orientation=1;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(15),dp(12),dp(15),dp(12));background=round(panel,18)
        addView(TextView(this@MainActivity).apply{text=title;textSize=11f;setTextColor(muted)})
        addView(TextView(this@MainActivity).apply{text=value;textSize=29f;setTextColor(fg);setTypeface(null,Typeface.BOLD)})
    }
    private fun info(title:String,body:String){val c=cardView();c.addView(TextView(this).apply{text=title;textSize=14f;setTextColor(fg);setTypeface(null,Typeface.BOLD)});c.addView(TextView(this).apply{text=body;textSize=11f;setTextColor(muted);setPadding(0,dp(7),0,0)});content.addView(c)}
    private fun section(s:String){content.addView(TextView(this).apply{text=s;textSize=10f;setTextColor(muted);setTypeface(null,Typeface.BOLD);setPadding(0,dp(5),0,dp(8))})}
    private fun cardView()=LinearLayout(this).apply{orientation=1;setPadding(dp(15),dp(14),dp(15),dp(14));background=round(panel,18);elevation=dp(2).toFloat()}
    private fun button(s:String,primary:Boolean,action:()->Unit)=TextView(this).apply{text=s;textSize=13f;gravity=Gravity.CENTER;setTextColor(if(primary)Color.WHITE else accent);setTypeface(null,Typeface.BOLD);background=round(if(primary)accent else panel,15);setPadding(dp(15),dp(14),dp(15),dp(14));setOnClickListener{action()}}
    private fun setting(icon:String,title:String,value:String,action:()->Unit){
        val r=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(13),dp(13),dp(13),dp(13));background=round(panel,15);setOnClickListener{action()}}
        r.addView(TextView(this).apply{text=icon;textSize=21f;setTextColor(accent)})
        r.addView(LinearLayout(this).apply{orientation=1;setPadding(dp(12),0,0,0);addView(TextView(this@MainActivity).apply{text=title;textSize=13f;setTextColor(fg)});addView(TextView(this@MainActivity).apply{text=value;textSize=10f;setTextColor(muted)})},LinearLayout.LayoutParams(0,-2,1f))
        r.addView(TextView(this).apply{text="›";textSize=22f;setTextColor(muted)})
        content.addView(r);content.addView(space(dp(8)))
    }
    private fun spaceNumbers()=entries.map{it.spaceNo}.distinct().sorted()
    private fun space(h:Int)=View(this).apply{layoutParams=LinearLayout.LayoutParams(1,h)}
    private fun round(c:Int,r:Int)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat()}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
private class Watcher(val onChange:(String)->Unit):android.text.TextWatcher{
    override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){}
    override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){onChange(s?.toString()?: "")}
    override fun afterTextChanged(s:android.text.Editable?){}
}
