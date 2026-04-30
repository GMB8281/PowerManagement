package com.marinov.powermanagement

import android.content.Context
import android.content.SharedPreferences

object UltraBatterySaver {

    private const val PREFS_NAME = "ultra_battery_prefs"
    private const val KEY_ALLOWED_APPS = "allowed_apps"
    private const val KEY_DEFAULT_LAUNCHER_PKG = "default_launcher_pkg"
    private const val KEY_DEFAULT_LAUNCHER_CLS = "default_launcher_cls"
    private const val KEY_SETUP_COMPLETE = "ultra_setup_complete"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isUltraSetupComplete(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_SETUP_COMPLETE, false)

    fun setUltraSetupComplete(context: Context, complete: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SETUP_COMPLETE, complete).apply()
    }

    fun saveAllowedApps(context: Context, apps: Set<String>) {
        getPrefs(context).edit().putStringSet(KEY_ALLOWED_APPS, apps).apply()
    }

    fun getAllowedApps(context: Context): Set<String> =
        getPrefs(context).getStringSet(KEY_ALLOWED_APPS, emptySet()) ?: emptySet()

    fun setDefaultLauncher(context: Context, packageName: String, activityName: String) {
        getPrefs(context).edit().apply {
            putString(KEY_DEFAULT_LAUNCHER_PKG, packageName)
            putString(KEY_DEFAULT_LAUNCHER_CLS, activityName)
            apply()
        }
    }

    fun getDefaultLauncherComponent(context: Context): String? {
        val pkg = getPrefs(context).getString(KEY_DEFAULT_LAUNCHER_PKG, null) ?: return null
        val cls = getPrefs(context).getString(KEY_DEFAULT_LAUNCHER_CLS, null) ?: return null
        return "$pkg/$cls"
    }
}