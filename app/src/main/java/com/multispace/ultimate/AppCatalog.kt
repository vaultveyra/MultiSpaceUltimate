package com.multispace.ultimate

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

object AppCatalog {
    fun load(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map {
                val ai = it.activityInfo.applicationInfo
                AppEntry(
                    pkg = ai.packageName,
                    label = it.loadLabel(pm).toString(),
                    isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .filter { it.pkg != ctx.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }
}

object IconCache {
    private val cache = LruCache<String, ImageBitmap>(200)

    fun get(ctx: Context, pkg: String): ImageBitmap? {
        cache.get(pkg)?.let { return it }
        return runCatching {
            ctx.packageManager.getApplicationIcon(pkg).toBitmap(144, 144).asImageBitmap()
        }.getOrNull()?.also { cache.put(pkg, it) }
    }
}
