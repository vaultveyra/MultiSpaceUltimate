package com.multispace.ultimate

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** ذخیره‌سازی پایدار (JSON در SharedPreferences). مرحله بعد: مهاجرت به Room. */
class SpacesRepository(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("spaces", Context.MODE_PRIVATE)

    fun loadInstances(): List<Instance> = runCatching {
        val arr = JSONArray(prefs.getString("instances", "[]"))
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Instance(
                id = o.getString("id"),
                pkg = o.getString("pkg"),
                label = o.getString("label"),
                number = o.getInt("number"),
                customName = if (o.has("custom")) o.getString("custom") else null,
                createdAt = o.optLong("created", 0L)
            )
        }
    }.getOrDefault(emptyList())

    fun loadSpaceNames(): Map<Int, String> = runCatching {
        val o = JSONObject(prefs.getString("names", "{}"))
        o.keys().asSequence().associate { it.toInt() to o.getString(it) }
    }.getOrDefault(emptyMap())

    fun save(list: List<Instance>, names: Map<Int, String>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id); put("pkg", it.pkg); put("label", it.label)
                put("number", it.number); put("created", it.createdAt)
                it.customName?.let { c -> put("custom", c) }
            })
        }
        val n = JSONObject()
        names.forEach { (k, v) -> n.put(k.toString(), v) }
        prefs.edit().putString("instances", arr.toString()).putString("names", n.toString()).apply()
    }
}

class SpacesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SpacesRepository(app)

    var instances by mutableStateOf(repo.loadInstances()); private set
    var spaceNames by mutableStateOf(repo.loadSpaceNames()); private set

    /** فضای N وقتی وجود دارد که حداقل یک نمونه با شماره N باشد. */
    val spaceNumbers: List<Int> get() = instances.map { it.number }.distinct().sorted()

    /** قانون اصلی: کوچک‌ترین شماره‌ی آزاد همان برنامه = شماره نمونه = شماره فضا. */
    fun addApp(app: AppEntry): Instance {
        val used = instances.filter { it.pkg == app.pkg }.map { it.number }.toSet()
        var n = 1
        while (n in used) n++
        val inst = Instance(UUID.randomUUID().toString(), app.pkg, app.label, n)
        instances = instances + inst
        persist()
        return inst
    }

    fun renameInstance(id: String, name: String?) {
        instances = instances.map {
            if (it.id == id) it.copy(customName = name?.trim()?.takeIf { s -> s.isNotEmpty() }) else it
        }
        persist()
    }

    fun renameSpace(number: Int, name: String?) {
        val clean = name?.trim()?.takeIf { it.isNotEmpty() }
        spaceNames = if (clean == null) spaceNames - number else spaceNames + (number to clean)
        persist()
    }

    fun delete(id: String) {
        instances = instances.filterNot { it.id == id }
        persist()
    }

    /** ترتیب جدید آیکن‌های یک فضا (بدون تغییر شماره‌ی نمونه‌ها). */
    fun reorder(space: Int, ids: List<String>) {
        val map = instances.associateBy { it.id }
        val others = instances.filter { it.number != space }
        instances = others + ids.mapNotNull { map[it] }
        persist()
    }

    fun countOf(pkg: String) = instances.count { it.pkg == pkg }

    private fun persist() {
        val alive = instances.map { it.number }.toSet()
        spaceNames = spaceNames.filterKeys { it in alive }
        repo.save(instances, spaceNames)
    }
}
