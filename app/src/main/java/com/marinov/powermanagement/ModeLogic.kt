package com.marinov.powermanagement

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.edit
import com.marinov.powermanagement.TaskerLogic.Mode

object ModeLogic {

    private const val PREFS_NAME = "ultra_battery_prefs"
    private const val KEY_DEFAULT_LAUNCHER_PKG = "default_launcher_pkg"
    private const val KEY_DEFAULT_LAUNCHER_CLS = "default_launcher_cls"
    private const val KEY_INITIAL_SETUP_COMPLETE = "initial_setup_complete"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isInitialSetupComplete(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_INITIAL_SETUP_COMPLETE, false)

    fun setInitialSetupComplete(context: Context, complete: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_INITIAL_SETUP_COMPLETE, complete) }
    }

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

    fun applyModeWithLauncher(context: Context, mode: Mode): Boolean {
        val currentMode = TaskerLogic.getLastAppliedMode(context)
        if (currentMode == Mode.ULTRA && mode == Mode.ULTRA) {
            Handler(Looper.getMainLooper()).post {
                // Nenhuma ação necessária
            }
            return true
        }

        val data = TaskerLogic.getProfileData(context, mode) ?: return false

        val version = TaskerLogic.getVersionCode(context, mode)
        TaskerLogic.applyProfile(context, data, version)

        val previousMode = TaskerLogic.getLastAppliedMode(context)
        TaskerLogic.saveLastAppliedMode(context, mode)

        val intent = Intent(TaskerLogic.ACTION_MODE_APPLIED)
        intent.setPackage(context.packageName)
        context.sendBroadcast(intent)

        if (previousMode == Mode.ULTRA && mode != Mode.ULTRA) {
            UltraBatterySaver.unsuspendAllSuspendedApps(context) {
                Toast.makeText(context, R.string.apps_restored_toast, Toast.LENGTH_SHORT).show()
            }
        }

        when (mode) {
            Mode.PERFORMANCE, Mode.STANDARD -> {
                try { LauncherManager.finishIfActive() } catch (_: Exception) {}
                val launcherComponent = getDefaultLauncherComponent(context)
                if (launcherComponent != null) {
                    if (!setHomeActivityRoot(context, launcherComponent)) {
                        Toast.makeText(context, R.string.launcher_switch_fail, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, R.string.no_default_launcher, Toast.LENGTH_SHORT).show()
                }
                Toast.makeText(
                    context,
                    context.getString(R.string.mode_applied_toast, mode.displayName),
                    Toast.LENGTH_SHORT
                ).show()
            }
            Mode.ULTRA -> {
                activateUltraMode(context)
                if (RootCommands.isRootAvailable()) {
                    UltraBatterySaver.suspendNonAllowedApps(context) {
                        Toast.makeText(context, R.string.apps_suspended_toast, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, R.string.root_required_suspend, Toast.LENGTH_SHORT).show()
                }
            }
        }
        return true
    }

    private fun activateUltraMode(context: Context) {
        val ourLauncher = "${context.packageName}/.LauncherActivity"
        val success = setHomeActivityRoot(context, ourLauncher)

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val launchIntent = Intent(context, LauncherActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                context.applicationContext.startActivity(launchIntent)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.ultra_launcher_error, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, 250)

        if (!success) {
            Toast.makeText(context, R.string.ultra_launcher_set_fail, Toast.LENGTH_SHORT).show()
        }
        Toast.makeText(
            context,
            context.getString(R.string.mode_applied_toast, Mode.ULTRA.displayName),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun setHomeActivityRoot(context: Context, component: String): Boolean {
        if (!RootCommands.isRootAvailable()) {
            Toast.makeText(context, R.string.root_not_available, Toast.LENGTH_SHORT).show()
            return false
        }
        val success = RootCommands.run("cmd package set-home-activity $component")
        if (!success) {
            Toast.makeText(context, R.string.root_command_fail, Toast.LENGTH_SHORT).show()
        }
        return success
    }
}