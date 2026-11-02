package com.veyra.multispace

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import java.util.Locale

private data class SpaceItem(val id: Long, val name: String)
private data class LanguageItem(val code: String, val flag: String, val nativeName: String, val englishName: String)

private val languages = listOf(
    LanguageItem("fa", "🇮🇷", "فارسی", "Persian"),
    LanguageItem("en", "🇺🇸", "English", "English"),
    LanguageItem("de", "🇩🇪", "Deutsch", "German"),
    LanguageItem("fr", "🇫🇷", "Français", "French"),
    LanguageItem("es", "🇪🇸", "Español", "Spanish"),
    LanguageItem("ar", "🇸🇦", "العربية", "Arabic"),
    LanguageItem("tr", "🇹🇷", "Türkçe", "Turkish")
)

private class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("multispace_preferences", Context.MODE_PRIVATE)
    fun language(): String = prefs.getString("language", "fa") ?: "fa"
    fun saveLanguage(code: String) { prefs.edit().putString("language", code).apply() }
    fun spaces(): List<SpaceItem> {
        val raw = prefs.getString("spaces", null) ?: return listOf(SpaceItem(1, "فضای شخصی"), SpaceItem(2, "فضای آزمایشی")).also { saveSpaces(it) }
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i -> array.getJSONObject(i).let { SpaceItem(it.getLong("id"), it.getString("name")) } }
        } catch (_: Exception) { emptyList() }
    }
    fun saveSpaces(items: List<SpaceItem>) {
        val array = JSONArray()
        items.forEach { item -> array.put(org.json.JSONObject().put("id", item.id).put("name", item.name)) }
        prefs.edit().putString("spaces", array.toString()).apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = AppPrefs(this)
        setContent { MultiSpaceApp(prefs) }
    }
}

private fun tr(code: String, key: String): String {
    val fa = mapOf(
        "welcome" to "خوش آمدید", "overview" to "مدیریت فضای کاری شما", "spaces" to "فضاها", "apps" to "برنامه‌ها",
        "compat" to "گزارش سازگاری", "settings" to "تنظیمات", "language" to "انتخاب زبان", "create" to "ایجاد فضای جدید",
        "empty" to "هنوز فضایی ایجاد نشده است", "delete" to "حذف", "spaceName" to "نام فضا", "cancel" to "انصراف",
        "save" to "ذخیره", "device" to "مشخصات دستگاه", "manufacturer" to "سازنده", "model" to "مدل دستگاه",
        "android" to "نسخه اندروید", "api" to "سطح API", "appTitle" to "مدیریت برنامه‌ها",
        "appNote" to "در این نسخه فهرست و مدیریت اطلاعات اولیه فراهم شده است؛ اجرای ایزوله برنامه‌های دیگر هنوز پیاده‌سازی نشده است.",
        "dashboard" to "داشبورد", "settingsNote" to "زبان انتخابی و فضاها روی همین دستگاه ذخیره می‌شوند.", "count" to "تعداد فضاها"
    )
    val en = mapOf(
        "welcome" to "Welcome", "overview" to "Your workspace overview", "spaces" to "Spaces", "apps" to "Apps",
        "compat" to "Compatibility", "settings" to "Settings", "language" to "Choose language", "create" to "Create space",
        "empty" to "No spaces yet", "delete" to "Delete", "spaceName" to "Space name", "cancel" to "Cancel",
        "save" to "Save", "device" to "Device information", "manufacturer" to "Manufacturer", "model" to "Device model",
        "android" to "Android version", "api" to "API level", "appTitle" to "App management",
        "appNote" to "This version stores and manages basic space information. True isolated execution of third-party apps is not implemented yet.",
        "dashboard" to "Dashboard", "settingsNote" to "Your language and spaces are saved on this device.", "count" to "Spaces"
    )
    val de = mapOf("welcome" to "Willkommen", "overview" to "Ihre Arbeitsbereiche", "spaces" to "Bereiche", "apps" to "Apps", "compat" to "Kompatibilität", "settings" to "Einstellungen", "language" to "Sprache wählen", "create" to "Bereich erstellen", "empty" to "Noch keine Bereiche", "delete" to "Löschen", "spaceName" to "Bereichsname", "cancel" to "Abbrechen", "save" to "Speichern", "device" to "Geräteinformationen", "manufacturer" to "Hersteller", "model" to "Gerätemodell", "android" to "Android-Version", "api" to "API-Level", "appTitle" to "App-Verwaltung", "appNote" to "Diese Version speichert und verwaltet grundlegende Bereichsdaten. Eine isolierte Ausführung von Drittanbieter-Apps ist noch nicht implementiert.", "dashboard" to "Dashboard", "settingsNote" to "Sprache und Bereiche werden auf diesem Gerät gespeichert.", "count" to "Bereiche")
    val fr = mapOf("welcome" to "Bienvenue", "overview" to "Aperçu de votre espace", "spaces" to "Espaces", "apps" to "Applications", "compat" to "Compatibilité", "settings" to "Paramètres", "language" to "Choisir la langue", "create" to "Créer un espace", "empty" to "Aucun espace", "delete" to "Supprimer", "spaceName" to "Nom de l’espace", "cancel" to "Annuler", "save" to "Enregistrer", "device" to "Informations sur l’appareil", "manufacturer" to "Fabricant", "model" to "Modèle", "android" to "Version Android", "api" to "Niveau API", "appTitle" to "Gestion des applications", "appNote" to "Cette version gère les informations de base. L’exécution isolée d’applications tierces n’est pas encore implémentée.", "dashboard" to "Tableau de bord", "settingsNote" to "La langue et les espaces sont enregistrés sur cet appareil.", "count" to "Espaces")
    val es = mapOf("welcome" to "Bienvenido", "overview" to "Resumen de tu espacio", "spaces" to "Espacios", "apps" to "Aplicaciones", "compat" to "Compatibilidad", "settings" to "Ajustes", "language" to "Elegir idioma", "create" to "Crear espacio", "empty" to "Aún no hay espacios", "delete" to "Eliminar", "spaceName" to "Nombre del espacio", "cancel" to "Cancelar", "save" to "Guardar", "device" to "Información del dispositivo", "manufacturer" to "Fabricante", "model" to "Modelo", "android" to "Versión de Android", "api" to "Nivel API", "appTitle" to "Gestión de aplicaciones", "appNote" to "Esta versión guarda y administra datos básicos. La ejecución aislada de aplicaciones de terceros aún no está implementada.", "dashboard" to "Panel", "settingsNote" to "El idioma y los espacios se guardan en este dispositivo.", "count" to "Espacios")
    val ar = mapOf("welcome" to "مرحباً", "overview" to "نظرة عامة على مساحاتك", "spaces" to "المساحات", "apps" to "التطبيقات", "compat" to "التوافق", "settings" to "الإعدادات", "language" to "اختر اللغة", "create" to "إنشاء مساحة", "empty" to "لا توجد مساحات بعد", "delete" to "حذف", "spaceName" to "اسم المساحة", "cancel" to "إلغاء", "save" to "حفظ", "device" to "معلومات الجهاز", "manufacturer" to "الشركة المصنعة", "model" to "طراز الجهاز", "android" to "إصدار أندرويد", "api" to "مستوى API", "appTitle" to "إدارة التطبيقات", "appNote" to "هذا الإصدار يدير المعلومات الأساسية. لم يتم تنفيذ التشغيل المعزول لتطبيقات الجهات الخارجية بعد.", "dashboard" to "لوحة التحكم", "settingsNote" to "يتم حفظ اللغة والمساحات على هذا الجهاز.", "count" to "المساحات")
    val tr = mapOf("welcome" to "Hoş geldiniz", "overview" to "Çalışma alanınıza genel bakış", "spaces" to "Alanlar", "apps" to "Uygulamalar", "compat" to "Uyumluluk", "settings" to "Ayarlar", "language" to "Dil seçin", "create" to "Alan oluştur", "empty" to "Henüz alan yok", "delete" to "Sil", "spaceName" to "Alan adı", "cancel" to "İptal", "save" to "Kaydet", "device" to "Cihaz bilgileri", "manufacturer" to "Üretici", "model" to "Cihaz modeli", "android" to "Android sürümü", "api" to "API seviyesi", "appTitle" to "Uygulama yönetimi", "appNote" to "Bu sürüm temel alan bilgilerini yönetir. Üçüncü taraf uygulamaların yalıtılmış çalıştırılması henüz uygulanmadı.", "dashboard" to "Kontrol paneli", "settingsNote" to "Dil ve alanlar bu cihazda saklanır.", "count" to "Alanlar")
    val map = when (code) { "en" -> en; "de" -> de; "fr" -> fr; "es" -> es; "ar" -> ar; "tr" -> tr; else -> fa }
    return map[key] ?: en[key] ?: key
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiSpaceApp(prefs: AppPrefs) {
    var language by remember { mutableStateOf(prefs.language()) }
    var spaces by remember { mutableStateOf(prefs.spaces()) }
    var tab by remember { mutableIntStateOf(0) }
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val rtl = language == "fa" || language == "ar"
    val direction = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF7255A4), secondary = androidx.compose.ui.graphics.Color(0xFFB18ACB))) {
            Scaffold(
                topBar = { TopAppBar(title = { Column { Text("MultiSpace Ultimate", fontWeight = FontWeight.Bold); Text("Personal workspace manager", style = MaterialTheme.typography.labelSmall) } }) },
                bottomBar = {
                    NavigationBar {
                        val labels = listOf(tr(language, "dashboard"), tr(language, "spaces"), tr(language, "apps"), tr(language, "compat"), tr(language, "settings"))
                        val icons = listOf("⌂", "▦", "▣", "◈", "⚙")
                        labels.forEachIndexed { index, label -> NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Text(icons[index]) }, label = { Text(label) }) }
                    }
                },
                floatingActionButton = { if (tab == 1) FloatingActionButton(onClick = { newName = ""; showCreate = true }) { Text("+") } }
            ) { padding ->
                LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (tab) {
                        0 -> {
                            item { Text(tr(language, "welcome"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(tr(language, "overview"), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            item { LanguagePicker(language, onSelect = { language = it; prefs.saveLanguage(it) }, title = tr(language, "language")) }
                            item { SummaryCard(tr(language, "count"), spaces.size.toString()) }
                            item { SummaryCard(tr(language, "spaces"), spaces.joinToString().ifBlank { tr(language, "empty") }) }
                            item { SummaryCard("MultiSpace Ultimate", "Version 0.2.0") }
                        }
                        1 -> {
                            item { Text(tr(language, "spaces"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                            if (spaces.isEmpty()) item { Text(tr(language, "empty")) }
                            items(spaces, key = { it.id }) { space ->
                                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("▦", style = MaterialTheme.typography.headlineMedium)
                                        Spacer(Modifier.width(12.dp))
                                        Text(space.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                        TextButton(onClick = { spaces = spaces.filterNot { it.id == space.id }; prefs.saveSpaces(spaces) }) { Text(tr(language, "delete")) }
                                    }
                                }
                            }
                            item { Text(tr(language, "create") + "  +", color = MaterialTheme.colorScheme.primary) }
                        }
                        2 -> { item { Text(tr(language, "appTitle"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }; item { SummaryCard(tr(language, "apps"), tr(language, "appNote")) } }
                        3 -> {
                            item { Text(tr(language, "compat"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                            item { SummaryCard(tr(language, "device"), "${Build.MANUFACTURER} ${Build.MODEL}") }
                            item { SummaryCard(tr(language, "android"), Build.VERSION.RELEASE ?: "Unknown") }
                            item { SummaryCard(tr(language, "api"), Build.VERSION.SDK_INT.toString()) }
                            item { SummaryCard("CPU ABI", Build.SUPPORTED_ABIS.joinToString()) }
                            item { Text("Device information only. Virtual app compatibility has not yet been validated.", style = MaterialTheme.typography.bodySmall) }
                        }
                        else -> { item { Text(tr(language, "settings"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }; item { LanguagePicker(language, onSelect = { language = it; prefs.saveLanguage(it) }, title = tr(language, "language")) }; item { SummaryCard(tr(language, "settings"), tr(language, "settingsNote")) } }
                    }
                }
                if (showCreate) AlertDialog(
                    onDismissRequest = { showCreate = false },
                    title = { Text(tr(language, "create")) },
                    text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text(tr(language, "spaceName")) }, singleLine = true) },
                    confirmButton = { TextButton(onClick = { val name = newName.trim(); if (name.isNotEmpty()) { spaces = spaces + SpaceItem(System.currentTimeMillis(), name); prefs.saveSpaces(spaces) }; showCreate = false }) { Text(tr(language, "save")) } },
                    dismissButton = { TextButton(onClick = { showCreate = false }) { Text(tr(language, "cancel")) } }
                )
            }
        }
    }
}

@Composable
private fun LanguagePicker(language: String, onSelect: (String) -> Unit, title: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            languages.forEach { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.flag, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(item.nativeName, fontWeight = FontWeight.SemiBold); Text(item.englishName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    RadioButton(selected = language == item.code, onClick = { onSelect(item.code) })
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
