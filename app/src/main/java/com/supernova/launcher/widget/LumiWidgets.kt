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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val ACTION_CLEAN_CACHE = "com.dimm.lumilite.action.CLEAN_CACHE"

abstract class LumiInfoProvider : AppWidgetProvider() {
    protected open fun views(context: Context): RemoteViews =
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
    override fun views(context: Context): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_weather_clock)

    override fun render(context: Context, views: RemoteViews) {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val atmosphere = when (hour) {
            in 6..11 -> context.getString(R.string.widget_weather_morning)
            in 12..17 -> context.getString(R.string.widget_weather_day)
            in 18..22 -> context.getString(R.string.widget_weather_evening)
            else -> context.getString(R.string.widget_weather_night)
        }
        // This is an offline launcher widget: show a calm local snapshot rather
        // than pretending to have a network weather feed. A future provider
        // can replace these values without changing the visual contract.
        val warm = hour in 10..18
        val temperature = if (warm) 28 else 23
        val maximum = temperature + 2
        val minimum = temperature - 7
        val dayFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        val forecastDay = SimpleDateFormat("EEE", Locale.getDefault())
        val forecast = listOf(
            Triple(forecastDay.format(Date(now.timeInMillis + 86_400_000L)), "☀", "$maximum° / $minimum°"),
            Triple(forecastDay.format(Date(now.timeInMillis + 172_800_000L)), "◒", "${maximum + 1}° / ${minimum + 1}°"),
            Triple(forecastDay.format(Date(now.timeInMillis + 259_200_000L)), "☀", "$maximum° / ${minimum + 2}°")
        )

        // Avoid emoji variation-selector sequences: some OEM RemoteViews
        // hosts reject them while applying the provider update.
        views.setTextViewText(R.id.weatherWidgetIcon, if (warm) "☀" else "☁")
        views.setTextViewText(R.id.weatherWidgetTemp, "$temperature°")
        views.setTextViewText(R.id.weatherWidgetCondition, atmosphere)
        views.setTextViewText(R.id.weatherWidgetLocation, context.getString(R.string.widget_weather_location))
        views.setTextViewText(R.id.weatherWidgetClock, timeLabel())
        views.setTextViewText(R.id.weatherWidgetDate, dayFormat.format(now.time))
        views.setTextViewText(
            R.id.weatherWidgetHighLow,
            context.getString(R.string.widget_weather_high_low, maximum, minimum)
        )
        forecast.forEachIndexed { index, (day, icon, values) ->
            val dayId = listOf(R.id.weatherForecastDay1, R.id.weatherForecastDay2, R.id.weatherForecastDay3)[index]
            val infoId = listOf(R.id.weatherForecastInfo1, R.id.weatherForecastInfo2, R.id.weatherForecastInfo3)[index]
            views.setTextViewText(dayId, day.replaceFirstChar { it.titlecase(Locale.getDefault()) })
            views.setTextViewText(infoId, "$icon  $values")
        }
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

/** A compact 4x1 system card that replaces the three separate Lumi cards. */
class LumiSystemInfoWidgetProvider : LumiInfoProvider() {
    override fun views(context: Context): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_system_info)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_CLEAN_CACHE) return
        runCatching { context.cacheDir.deleteRecursively() }
        runCatching { context.externalCacheDir?.deleteRecursively() }
        updateAll(context, LumiSystemInfoWidgetProvider::class.java) { render(context, it) }
    }

    override fun render(context: Context, views: RemoteViews) {
        val memory = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
            ?.getMemoryInfo(memory)
        val usedRam = (memory.totalMem - memory.availMem).coerceAtLeast(0L)
        views.setTextViewText(R.id.systemRamTitle, context.getString(R.string.widget_ram_title))
        views.setTextViewText(R.id.systemRamValue, formatBytes(usedRam))
        views.setTextViewText(
            R.id.systemRamDetail,
            context.getString(R.string.widget_ram_detail, formatBytes(memory.totalMem))
        )

        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val free = stat.availableBytes
        views.setTextViewText(
            R.id.systemStorageTitle,
            context.getString(R.string.widget_storage_title)
        )
        views.setTextViewText(R.id.systemStorageValue, formatBytes(total - free))
        views.setTextViewText(
            R.id.systemStorageDetail,
            context.getString(R.string.widget_storage_detail, formatBytes(free), formatBytes(total))
        )

        views.setTextViewText(R.id.systemCacheTitle, context.getString(R.string.widget_cache_title))
        views.setTextViewText(R.id.systemCacheDetail, context.getString(R.string.widget_cache_detail))
        views.setTextViewText(R.id.systemCacheAction, context.getString(R.string.widget_cache_button))
        val cleanIntent = Intent(context, LumiSystemInfoWidgetProvider::class.java)
            .setAction(ACTION_CLEAN_CACHE)
        views.setOnClickPendingIntent(
            R.id.systemCacheAction,
            PendingIntent.getBroadcast(
                context,
                7002,
                cleanIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
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
