package gov.sci.causelist.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sc_causelist_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SEEN_IDS = "seen_cause_list_ids"
        private const val KEY_LAST_CHECKED = "last_checked_timestamp"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_FIRST_RUN = "is_first_run"
    }

    fun isFirstRun(): Boolean {
        val firstRun = prefs.getBoolean(KEY_FIRST_RUN, true)
        if (firstRun) {
            prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply()
        }
        return firstRun
    }

    fun getSeenIds(): Set<String> {
        return prefs.getStringSet(KEY_SEEN_IDS, emptySet()) ?: emptySet()
    }

    fun addSeenIds(newIds: Collection<String>) {
        val current = getSeenIds().toMutableSet()
        current.addAll(newIds)
        prefs.edit().putStringSet(KEY_SEEN_IDS, current).apply()
    }

    fun setLastChecked(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_CHECKED, timestamp).apply()
    }

    fun getLastChecked(): Long {
        return prefs.getLong(KEY_LAST_CHECKED, 0L)
    }

    fun isNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }
}
