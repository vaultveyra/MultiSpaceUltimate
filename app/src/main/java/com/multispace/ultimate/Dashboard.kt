package com.multispace.ultimate

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

private fun gb(b: Long) = String.format(Locale.US, "%.1f GB", b / 1073741824.0)

private data class Res(val used: Float, val free: Long, val total: Long)

private fun ram(ctx: Context): Res {
    val mi = ActivityManager.MemoryInfo()
    (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
    return Res(1f - mi.availMem.toFloat() / mi.totalMem, mi.availMem, mi.totalMem)
}

private fun storage(): Res {
    val s = StatFs(Environment.getDataDirectory().path)
    val total = s.blockCountLong * s.blockSizeLong
    val free = s.availableBlocksLong * s.blockSizeLong
    return Res(1f - free.toFloat() / total, free, total)
}

@Composable
fun supportLabel(s: Support) = when (s) {
    Support.Supported -> stringResource(R.string.support_supported)
    Support.Partial -> stringResource(R.string.support_partial)
    Support.Unsupported -> stringResource(R.string.support_unsupported)
    Support.NotTested -> stringResource(R.string.support_nottested)
}

private fun supportColor(s: Support) = when (s) {
    Support.Supported -> Green
    Support.Partial -> Amber
    Support.Unsupported -> Red
    Support.NotTested -> Gray
}

@Composable
fun DashboardScreen(vm: SpacesViewModel, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val ramInfo = remember { ram(ctx) }
    val storageInfo = remember { storage() }
    val pm = ctx.packageManager
    val gms = remember { runCatching { pm.getPackageInfo("com.google.android.gms", 0); true }.getOrDefault(false) }
    val managed = remember { pm.hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS) }

    Column(
        modifier.fillMaxSize().background(screenBrush()).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            stringResource(R.string.dash_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.dash_spaces), "${vm.spaceNumbers.size}", Modifier.weight(1f))
            StatCard(stringResource(R.string.dash_instances), "${vm.instances.size}", Modifier.weight(1f))
        }

        SectionCard(stringResource(R.string.dash_engine)) {
            StatusRow(stringResource(R.string.dash_engine_none), NoEngine.status)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.dash_engine_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard(stringResource(R.string.dash_resources)) {
            ResourceBar(
                stringResource(R.string.dash_ram), ramInfo.used,
                stringResource(R.string.free_of, gb(ramInfo.free), gb(ramInfo.total))
            )
            Spacer(Modifier.height(12.dp))
            ResourceBar(
                stringResource(R.string.dash_storage), storageInfo.used,
                stringResource(R.string.free_of, gb(storageInfo.free), gb(storageInfo.total))
            )
        }

        SectionCard(stringResource(R.string.dash_compat)) {
            StatusRow(stringResource(R.string.compat_isolation), Support.Unsupported)
            StatusRow(
                stringResource(R.string.compat_profile),
                if (managed) Support.Partial else Support.Unsupported
            )
            StatusRow(
                stringResource(R.string.compat_gms),
                if (gms) Support.Supported else Support.Unsupported
            )
            StatusRow(stringResource(R.string.compat_virtual), Support.NotTested)
            StatusRow(stringResource(R.string.compat_notif), Support.NotTested)
            StatusRow(stringResource(R.string.compat_bg), Support.NotTested)
        }

        SectionCard(stringResource(R.string.dash_device)) {
            Text("${Build.MANUFACTURER} ${Build.MODEL}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) • ${Build.SUPPORTED_ABIS.firstOrNull() ?: "?"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard(stringResource(R.string.dash_recent)) {
            val recent = vm.instances.sortedByDescending { it.createdAt }.take(4)
            if (recent.isEmpty()) {
                Text(
                    stringResource(R.string.dash_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            recent.forEach {
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(it.pkg, Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)))
                    Spacer(Modifier.width(12.dp))
                    Text(it.displayName, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        vm.spaceNames[it.number] ?: stringResource(R.string.space_default, it.number),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier) {
    Card(
        modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(title, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun StatusRow(name: String, s: Support) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Box(Modifier.size(8.dp).background(supportColor(s), CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(supportLabel(s), style = MaterialTheme.typography.labelMedium, color = supportColor(s))
    }
}

@Composable
private fun ResourceBar(title: String, used: Float, caption: String) {
    Column {
        Row {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(caption, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { used.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = if (used > 0.85f) Red else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
