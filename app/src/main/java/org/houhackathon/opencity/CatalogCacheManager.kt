package org.houhackathon.opencity

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CatalogCacheManager {
    private const val CACHE_FILE_NAME = "catalog_offline_cache.json"
    private const val PREFS_NAME = "catalog_offline_prefs"
    private const val KEY_LAST_SYNCED = "catalog_last_synced"
    private const val KEY_PINNED_SERVICES = "catalog_pinned_services"

    val OFFLINE_CAPABLE_ROUTES = setOf(
        "find-bus",
        "bus-routes",
        "hotlines",
        "digital-wards",
        "air-quality",
        "map",
        "news",
        "communications"
    )

    fun getCatalog(context: Context): JSONObject {
        val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
        if (cacheFile.exists()) {
            try {
                val jsonStr = cacheFile.bufferedReader().use { it.readText() }
                if (jsonStr.isNotBlank()) {
                    return JSONObject(jsonStr)
                }
            } catch (_: Exception) {
            }
        }
        return refreshCacheFromAssets(context)
    }

    fun refreshCacheFromAssets(context: Context): JSONObject {
        val jsonStr = runCatching {
            context.assets.open("catalog.json").bufferedReader().use { it.readText() }
        }.getOrDefault("{}")

        val obj = runCatching { JSONObject(jsonStr) }.getOrDefault(JSONObject())

        try {
            val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
            cacheFile.writeText(jsonStr)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_LAST_SYNCED, System.currentTimeMillis()).apply()
        } catch (_: Exception) {
            // Write failure fallback
        }

        return obj
    }

    fun getLastSyncedTimeFormatted(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timestamp = prefs.getLong(KEY_LAST_SYNCED, 0L)
        return if (timestamp > 0) {
            val sdf = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        } else {
            "Sẵn sàng ngoại tuyến"
        }
    }

    fun isPinned(context: Context, serviceId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_PINNED_SERVICES, emptySet()) ?: emptySet()
        return serviceId in set
    }

    fun togglePin(context: Context, serviceId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = (prefs.getStringSet(KEY_PINNED_SERVICES, emptySet()) ?: emptySet()).toMutableSet()
        val willPin = if (serviceId in set) {
            set.remove(serviceId)
            false
        } else {
            set.add(serviceId)
            true
        }
        prefs.edit().putStringSet(KEY_PINNED_SERVICES, set).apply()
        return willPin
    }

    fun getPinnedIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_PINNED_SERVICES, emptySet()) ?: emptySet()
    }

    fun isOfflineCapable(service: JSONObject): Boolean {
        val route = service.optString("route", "").removePrefix("/")
        val id = service.optString("id", "")
        return route in OFFLINE_CAPABLE_ROUTES || id in setOf("find-bus", "bus-routes", "hotlines", "digital-ward", "air-quality", "report-map")
    }
}
