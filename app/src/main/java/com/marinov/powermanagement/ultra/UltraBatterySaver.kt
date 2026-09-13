package com.marinov.powermanagement.ultra

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.edit
import com.marinov.powermanagement.R
import com.marinov.powermanagement.core.RootCommands
import com.marinov.powermanagement.core.TaskerLogic

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

    /**
     * Salva a lista de apps permitidos, apenas se o modo Ultra NÃO estiver ativo.
     * Se o modo Ultra estiver ativo, a operação é ignorada e um Toast é mostrado.
     *
     * @return true se salvou, false se foi bloqueado.
     */
    fun saveAllowedApps(context: Context, apps: Set<String>): Boolean {
        val currentMode = TaskerLogic.getLastAppliedMode(context)

        if (currentMode == TaskerLogic.Mode.ULTRA) {
            mainHandler.post {
                Toast.makeText(
                    context,
                    R.string.cant_change_allowed_ultra_active,
                    Toast.LENGTH_LONG
                ).show()
            }
            return false
        }

        getPrefs(context).edit { putStringSet(KEY_ALLOWED_APPS, apps) }
        return true
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

    /**
     * Suspende apps não permitidos e também força a parada deles.
     *
     * Importante:
     * - pm suspend garante estado suspenso.
     * - am force-stop garante que processos desses apps não permaneçam em execução.
     * - Apenas apps suspensos são mortos. Apps permitidos não são afetados.
     */
    fun suspendNonAllowedApps(context: Context, onComplete: (() -> Unit)? = null) {
        Thread {
            try {
                val allowed = getAllowedApps(context)
                val visible = getVisiblePackages(context)
                val toSuspend = visible.filter { it !in allowed }

                if (toSuspend.isEmpty()) {
                    saveSuspendedPackages(context, emptySet())
                    return@Thread
                }

                if (!RootCommands.isRootAvailable()) {
                    saveSuspendedPackages(context, emptySet())
                    return@Thread
                }

                val suspendCommands = toSuspend.map { "pm suspend $it" }
                val suspendStarted = RootCommands.runBatchBestEffort(suspendCommands)

                if (!suspendStarted) {
                    saveSuspendedPackages(context, emptySet())
                    return@Thread
                }

                // Salva a lista antes de matar processos para garantir rollback posterior.
                saveSuspendedPackages(context, toSuspend.toSet())

                val killCommands = toSuspend.map { "am force-stop $it" }
                RootCommands.runBatchBestEffort(killCommands)

            } catch (_: Exception) {
                // Ignora falhas inesperadas para não quebrar o fluxo principal.
            } finally {
                onComplete?.let { mainHandler.post(it) }
            }
        }.start()
    }

    /**
     * Versão síncrona para uso em background, principalmente no BootReceiver.
     */
    fun unsuspendAllSuspendedAppsSync(context: Context) {
        try {
            val suspended = getSuspendedPackages(context)
            if (suspended.isEmpty()) return

            if (!RootCommands.isRootAvailable()) return

            val commands = suspended.map { "pm unsuspend $it" }
            RootCommands.runBatchBestEffort(commands)

            saveSuspendedPackages(context, emptySet())
        } catch (_: Exception) {
            // Silencioso de propósito.
        }
    }

    fun unsuspendAllSuspendedApps(context: Context, onComplete: (() -> Unit)? = null) {
        Thread {
            unsuspendAllSuspendedAppsSync(context)
            onComplete?.let { mainHandler.post(it) }
        }.start()
    }
}