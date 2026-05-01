package com.marinov.powermanagement

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.edit

object UltraBatterySaver {

    private const val PREFS_NAME = "ultra_battery_prefs"
    private const val KEY_ALLOWED_APPS = "allowed_apps"
    private const val KEY_SETUP_COMPLETE = "ultra_setup_complete"
    private const val KEY_SUSPENDED_PACKAGES = "suspended_packages"

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isUltraSetupComplete(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_SETUP_COMPLETE, false)

    fun setUltraSetupComplete(context: Context, complete: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_SETUP_COMPLETE, complete) }
    }

    fun saveAllowedApps(context: Context, apps: Set<String>) {
        getPrefs(context).edit { putStringSet(KEY_ALLOWED_APPS, apps) }
    }

    fun getAllowedApps(context: Context): Set<String> =
        getPrefs(context).getStringSet(KEY_ALLOWED_APPS, emptySet()) ?: emptySet()

    fun saveSuspendedPackages(context: Context, packages: Set<String>) {
        getPrefs(context).edit { putStringSet(KEY_SUSPENDED_PACKAGES, packages) }
    }

    fun getSuspendedPackages(context: Context): Set<String> =
        getPrefs(context).getStringSet(KEY_SUSPENDED_PACKAGES, emptySet()) ?: emptySet()

    private fun getVisiblePackages(context: Context): Set<String> {
        val pm = context.packageManager
        val packages = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (_: Exception) {
            emptyList<ApplicationInfo>()
        }

        val visible = mutableSetOf<String>()
        for (app in packages) {
            if (app.packageName == context.packageName) continue
            if (!app.enabled) continue
            if ((app.flags and ApplicationInfo.FLAG_SUSPENDED) != 0) continue

            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val canLaunch = pm.getLaunchIntentForPackage(app.packageName) != null
            if (isSystem && !canLaunch) continue

            visible.add(app.packageName)
        }
        return visible
    }

    fun suspendNonAllowedApps(context: Context, onComplete: (() -> Unit)? = null) {
        Thread {
            try {
                val allowed = getAllowedApps(context)
                val visible = getVisiblePackages(context)

                val toSuspend = visible.filter { it !in allowed }
                if (toSuspend.isEmpty()) {
                    saveSuspendedPackages(context, emptySet())
                    onComplete?.let { mainHandler.post(it) }
                    return@Thread
                }

                // Constrói script único
                val commands = toSuspend.map { "pm suspend $it" }
                val success = RootCommands.runBatch(commands)

                if (success) {
                    saveSuspendedPackages(context, toSuspend.toSet())
                } else {
                    // Se falhar, grava uma lista vazia para evitar inconsistências
                    saveSuspendedPackages(context, emptySet())
                }
            } catch (_: Exception) {
            } finally {
                onComplete?.let { mainHandler.post(it) }
            }
        }.start()
    }

    fun unsuspendAllSuspendedApps(context: Context, onComplete: (() -> Unit)? = null) {
        Thread {
            try {
                val suspended = getSuspendedPackages(context)
                if (suspended.isEmpty()) {
                    onComplete?.let { mainHandler.post(it) }
                    return@Thread
                }

                val commands = suspended.map { "pm unsuspend $it" }
                RootCommands.runBatch(commands)

                // Mesmo que alguns falhem, limpa a lista para evitar repetições desnecessárias
                saveSuspendedPackages(context, emptySet())
            } catch (_: Exception) {
            } finally {
                onComplete?.let { mainHandler.post(it) }
            }
        }.start()
    }
}