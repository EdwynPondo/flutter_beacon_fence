package com.flutter.beacon_fence.util

import android.content.Context
import androidx.core.content.edit

/**
 * Prevents duplicate ENTER callbacks (and therefore duplicate workers) for a region that is
 * already inside when AltBeacon replays region entries after a rebind (scan strategy switch or
 * scanning reactivation).
 *
 * Suppression only applies within [REBIND_WINDOW_MILLIS] of a rebind. Outside that window
 * AltBeacon stays the source of truth, so a missed EXIT can never block future ENTER events.
 */
object InsideRegionGuard {
    private const val PREFS = "beacon_fence_inside_regions"
    private const val INSIDE_IDS_KEY = "inside_region_ids"
    private const val LAST_REBIND_KEY = "last_rebind_millis"
    private const val REBIND_WINDOW_MILLIS = 60_000L

    private val lock = Any()

    fun markRebind(context: Context) = synchronized(lock) {
        prefs(context).edit { putLong(LAST_REBIND_KEY, System.currentTimeMillis()) }
    }

    /** Returns true if the ENTER for [regionId] should be dropped. Otherwise records it as inside. */
    fun shouldSuppressEnter(context: Context, regionId: String): Boolean = synchronized(lock) {
        val prefs = prefs(context)
        val inside = prefs.getStringSet(INSIDE_IDS_KEY, emptySet()) ?: emptySet()
        val lastRebind = prefs.getLong(LAST_REBIND_KEY, 0L)
        val withinRebindWindow = System.currentTimeMillis() - lastRebind < REBIND_WINDOW_MILLIS
        if (regionId in inside && withinRebindWindow) {
            return true
        }
        prefs.edit { putStringSet(INSIDE_IDS_KEY, inside + regionId) }
        return false
    }

    fun markOutside(context: Context, regionId: String) = synchronized(lock) {
        val prefs = prefs(context)
        val inside = prefs.getStringSet(INSIDE_IDS_KEY, emptySet()) ?: emptySet()
        prefs.edit { putStringSet(INSIDE_IDS_KEY, inside - regionId) }
    }

    fun clear(context: Context) = synchronized(lock) {
        prefs(context).edit { remove(INSIDE_IDS_KEY) }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
