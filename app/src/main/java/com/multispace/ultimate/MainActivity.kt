package com.multispace.ultimate

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val vm: SpacesViewModel by viewModels()

    override fun attachBaseContext(base: Context) {
        val lang = base.getSharedPreferences("settings", MODE_PRIVATE).getString("lang", "fa") ?: "fa"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val cfg = Configuration(base.resources.configuration)
        cfg.setLocale(locale)
        cfg.setLayoutDirection(locale)
        super.attachBaseContext(base.createConfigurationContext(cfg))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App(vm) { recreate() } }
    }
}

@Composable
fun App(vm: SpacesViewModel, onLanguageChanged: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var themeMode by remember { mutableStateOf(prefs.getString("theme", "system") ?: "system") }
    var lang by remember { mutableStateOf(prefs.getString("lang", "fa") ?: "fa") }
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) { "dark" -> true; "light" -> false; else -> systemDark }

    MultiSpaceTheme(dark) {
        var tab by rememberSaveable { mutableIntStateOf(1) }
        var showPicker by remember { mutableStateOf(false) }
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Rounded.Dashboard, null) },
                        label = { Text(stringResource(R.string.nav_dashboard)) })
                    NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.Rounded.GridView, null) },
                        label = { Text(stringResource(R.string.nav_spaces)) })
                    NavigationBarItem(tab == 2, { tab = 2 }, { Icon(Icons.Rounded.Settings, null) },
                        label = { Text(stringResource(R.string.nav_settings)) })
                }
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                when (tab) {
                    0 -> DashboardScreen(vm)
                    1 -> SpacesScreen(vm)
                    else -> SettingsScreen(
                        themeMode = themeMode,
                        onTheme = { themeMode = it; prefs.edit().putString("theme", it).apply() },
                        lang = lang,
                        onLang = {
                            if (it != lang) {
                                lang = it
                                prefs.edit().putString("lang", it).apply()
                                onLanguageChanged()
                            }
                        }
                    )
                }
                if (tab == 1) {
                    // پایین-چپ فیزیکی: در RTL سمت End و در LTR سمت Start
                    FloatingActionButton(
                        onClick = { showPicker = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(if (rtl) Alignment.BottomEnd else Alignment.BottomStart)
                            .padding(20.dp)
                    ) { Icon(Icons.Rounded.Add, stringResource(R.string.add_app)) }
                }
            }
        }
        if (showPicker) AppPickerSheet(vm) { showPicker = false }
    }
}
