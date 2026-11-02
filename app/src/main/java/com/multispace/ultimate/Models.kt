package com.multispace.ultimate

enum class Support { Supported, Partial, Unsupported, NotTested }

data class Instance(
    val id: String,
    val pkg: String,
    val label: String,
    val number: Int,
    val customName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val defaultName: String get() = "$label $number"
    val displayName: String get() = customName?.takeIf { it.isNotBlank() } ?: defaultName
}

data class AppEntry(val pkg: String, val label: String, val isSystem: Boolean)
