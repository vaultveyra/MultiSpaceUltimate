package com.multispace.ultimate

import android.content.Context

sealed class LaunchResult {
    object Ok : LaunchResult()
    data class Failed(val reason: String) : LaunchResult()
}

/**
 * رابط موتور جداسازی. موتور واقعی (Work Profile / موتور مجازی) پس از مرحله‌ی تحقیق و
 * آزمایش روی دستگاه واقعی اینجا وصل می‌شود.
 */
interface IsolationEngine {
    val status: Support
    fun launch(ctx: Context, instance: Instance): LaunchResult
}

/**
 * عمداً هیچ برنامه‌ای را اجرا نمی‌کند: اجرای برنامه‌ی اصلی زیر نام «Instagram 2»
 * طبق سند پروژه ممنوع است (کلون ظاهری).
 */
object NoEngine : IsolationEngine {
    override val status = Support.Unsupported
    override fun launch(ctx: Context, instance: Instance): LaunchResult =
        LaunchResult.Failed(ctx.getString(R.string.engine_missing))
}
