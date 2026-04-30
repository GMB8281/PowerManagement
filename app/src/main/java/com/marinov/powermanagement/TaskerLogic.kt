package com.marinov.powermanagement

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast

object TaskerLogic {

    // Constantes do Smartpack
    private const val PLUGIN_PACKAGE = "com.smartpack.kernelmanager"
    private const val ACTIVITY_PROFILES = "$PLUGIN_PACKAGE.activities.tools.profile.ProfileTaskerActivity"
    const val ACTION_FIRE_SETTING = "com.twofortyfouram.locale.intent.action.FIRE_SETTING"
    const val EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE"
    const val EXTRA_STRING_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB"
    const val BUNDLE_EXTRA_STRING_MESSAGE = "com.grarak.kerneladiutor.tasker.extra.STRING_MESSAGE"
    const val BUNDLE_EXTRA_INT_VERSION_CODE = "com.grarak.kerneladiutor.tasker.extra.INT_VERSION_CODE"
    enum class Mode { PERFORMANCE, STANDARD, ULTRA }

    private const val PREFS_NAME = "power_modes"
    private const val KEY_NAME_PREFIX = "profile_name_"
    private const val KEY_DATA_PREFIX = "profile_data_"
    private const val KEY_VERSION_PREFIX = "profile_version_"

    const val ACTION_MODE_APPLIED = "com.marinov.powermanagement.MODE_APPLIED"
    private const val KEY_LAST_APPLIED = "last_applied_mode"

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun nameKey(mode: Mode) = KEY_NAME_PREFIX + mode.name.lowercase()
    private fun dataKey(mode: Mode) = KEY_DATA_PREFIX + mode.name.lowercase()
    private fun versionKey(mode: Mode) = KEY_VERSION_PREFIX + mode.name.lowercase()

    fun saveProfile(context: Context, mode: Mode, name: String, data: String, versionCode: Int) {
        getPrefs(context).edit().apply {
            putString(nameKey(mode), name)
            putString(dataKey(mode), data)
            putInt(versionKey(mode), versionCode)
            apply()
        }
    }

    fun getProfileName(context: Context, mode: Mode): String? =
        getPrefs(context).getString(nameKey(mode), null)

    fun getProfileData(context: Context, mode: Mode): String? =
        getPrefs(context).getString(dataKey(mode), null)

    fun getVersionCode(context: Context, mode: Mode): Int =
        getPrefs(context).getInt(versionKey(mode), 1)

    fun saveLastAppliedMode(context: Context, mode: Mode) {
        getPrefs(context).edit().putString(KEY_LAST_APPLIED, mode.name).apply()
    }

    fun getLastAppliedMode(context: Context): Mode? {
        val name = getPrefs(context).getString(KEY_LAST_APPLIED, null) ?: return null
        return try { Mode.valueOf(name) } catch (_: Exception) { null }
    }

    fun isPluginAvailable(context: Context): Boolean {
        val intent = Intent().apply { setClassName(PLUGIN_PACKAGE, ACTIVITY_PROFILES) }
        return context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
    }

    fun requestProfilePicker(activity: Activity, requestCode: Int): Boolean {
        return try {
            activity.startActivityForResult(Intent().apply { setClassName(PLUGIN_PACKAGE, ACTIVITY_PROFILES) }, requestCode)
            true
        } catch (e: Exception) {
            Toast.makeText(activity, "Erro ao abrir o Smartpack", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun applyProfile(context: Context, data: String, versionCode: Int) {
        val bundle = Bundle().apply {
            putString(BUNDLE_EXTRA_STRING_MESSAGE, data)
            putInt(BUNDLE_EXTRA_INT_VERSION_CODE, versionCode)
        }
        context.sendBroadcast(Intent(ACTION_FIRE_SETTING).apply {
            setPackage(PLUGIN_PACKAGE)
            putExtra(EXTRA_BUNDLE, bundle)
        })
    }

    fun extractProfileName(data: Intent?) = data?.getStringExtra(EXTRA_STRING_BLURB)
    fun extractProfileData(data: Intent?) =
        data?.getBundleExtra(EXTRA_BUNDLE)?.getString(BUNDLE_EXTRA_STRING_MESSAGE)
    fun extractVersionCode(data: Intent?) =
        data?.getBundleExtra(EXTRA_BUNDLE)?.getInt(BUNDLE_EXTRA_INT_VERSION_CODE, 1) ?: 1
}