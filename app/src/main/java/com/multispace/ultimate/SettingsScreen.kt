package com.multispace.ultimate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val languages = listOf(
    "fa" to "فارسی", "en" to "English", "de" to "Deutsch", "fr" to "Français",
    "es" to "Español", "ar" to "العربية", "tr" to "Türkçe"
)
private val ready = setOf("fa", "en")

@Composable
fun SettingsScreen(
    themeMode: String,
    onTheme: (String) -> Unit,
    lang: String,
    onLang: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxSize().background(screenBrush()).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp),
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(18.dp)) {
                Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "system" to R.string.theme_system,
                        "light" to R.string.theme_light,
                        "dark" to R.string.theme_dark
                    ).forEach { (k, r) ->
                        FilterChip(selected = themeMode == k, onClick = { onTheme(k) }, label = { Text(stringResource(r)) })
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp),
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(vertical = 14.dp)) {
                Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 18.dp))
                Spacer(Modifier.height(6.dp))
                languages.forEach { (code, name) ->
                    val enabled = code in ready
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = enabled) { onLang(code) }
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = lang == code, onClick = { if (enabled) onLang(code) }, enabled = enabled)
                        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        if (!enabled) Text(stringResource(R.string.lang_soon), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp),
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(18.dp)) {
                Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.about_text, "0.1.0"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
