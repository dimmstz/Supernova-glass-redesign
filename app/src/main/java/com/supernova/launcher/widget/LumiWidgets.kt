package com.supernova.launcher.widget

import android.app.ActivityManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.StatFs
import android.widget.RemoteViews
import com.supernova.launcher.R
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private const val ACTION_CLEAN_CACHE = "com.dimm.lumilite.action.CLEAN_CACHE"

abstract class LumiInfoProvider : AppWidgetProvider() {
    protected fun views(context: Context): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_info)

    protected fun updateAll(context: Context, provider: Class<out AppWidgetProvider>, update: (RemoteViews) -> Unit) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, provider))
        ids.forEach { id ->
            val remote = views(context)
            update(remote)
            manager.updateAppWidget(id, remote)
        }
    }

    protected fun timeLabel(): String =
        DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(Date())

    protected fun dateLabel(): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date())

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val remote = views(context)
            render(context, remote)
            manager.updateAppWidget(id, remote)
        }
    }

    protected abstract fun render(context: Context, views: RemoteViews)
}

class WeatherClockWidgetProvider : LumiInfoProvider() {
    override fun render(context: Context, views: RemoteViews) {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val atmosphere = when (hour) {
            in 6..11 -> context.getString(R.string.widget_weather_morning)
            in 12..17 -> context.getString(R.string.widget_weather_day)
            in 18..22 -> context.getString(R.string.widget_weather_evening)
            else -> context.getString(R.string.widget_weather_night)
        }
        views.setTextViewText(R.id.widgetTitle, context.getString(R.string.widget_weather_title))
        views.setTextViewText(R.id.widgetValue, timeLabel())
        views.setTextViewText(R.id.widgetDetail, "$atmosphere  •  ${dateLabel()}")
        views.setViewVisibility(R.id.widgetAction, android.view.View.GONE)
    }
}

class RamWidgetProvider : LumiInfoProvider() {
    override fun render(context: Context, views: RemoteViews) {
        val info = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.getMemoryInfo(info)
        val used = (info.totalMem - info.availMem).coerceAtLeast(0L)
        views.setTextViewText(R.id.widgetTitle, context.getString(R.string.widget_ram_title))
        views.setTextViewText(R.id.widgetValue, formatBytes(used))
        views.setTextViewText(R.id.widgetDetail, context.getString(R.string.widget_ram_detail, formatBytes(info.totalMem)))
        views.setViewVisibility(R.id.widgetAction, android.view.View.GONE)
    }
}

class StorageWidgetProvider : LumiInfoProvider() {
    override fun render(context: Context, views: RemoteViews) {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val free = stat.availableBytes
        views.setTextViewText(R.id.widgetTitle, context.getString(R.string.widget_storage_title))
        views.setTextViewText(R.id.widgetValue, formatBytes(total - free))
        views.setTextViewText(R.id.widgetDetail, context.getString(R.string.widget_storage_detail, formatBytes(free), formatBytes(total)))
        views.setViewVisibility(R.id.widgetAction, android.view.View.GONE)
    }
}

class CacheCleanerWidgetProvider : LumiInfoProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_CLEAN_CACHE) return
        runCatching { context.cacheDir.deleteRecursively() }
        runCatching { context.externalCacheDir?.deleteRecursively() }
        updateAll(context, CacheCleanerWidgetProvider::class.java) { render(context, it) }
    }

    override fun render(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.widgetTitle, context.getString(R.string.widget_cache_title))
        views.setTextViewText(R.id.widgetValue, context.getString(R.string.widget_cache_value))
        views.setTextViewText(R.id.widgetDetail, context.getString(R.string.widget_cache_detail))
        val intent = Intent(context, CacheCleanerWidgetProvider::class.java).setAction(ACTION_CLEAN_CACHE)
        val pending = PendingIntent.getBroadcast(
            context, 7001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetAction, pending)
        views.setTextViewText(R.id.widgetAction, context.getString(R.string.widget_cache_button))
        views.setViewVisibility(R.id.widgetAction, android.view.View.VISIBLE)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = -1
    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[index])
}
