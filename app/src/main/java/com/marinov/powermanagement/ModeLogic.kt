package com.marinov.powermanagement

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import com.marinov.powermanagement.TaskerLogic.Mode
import androidx.core.content.edit

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
        val data = TaskerLogic.getProfileData(context, mode) ?: return false

        val version = TaskerLogic.getVersionCode(context, mode)
        TaskerLogic.applyProfile(context, data, version)

        val previousMode = TaskerLogic.getLastAppliedMode(context)
        TaskerLogic.saveLastAppliedMode(context, mode)

        val intent = Intent(TaskerLogic.ACTION_MODE_APPLIED)
        intent.setPackage(context.packageName)
        context.sendBroadcast(intent)

        // Se estava em ULTRA e vai sair, inicia a desuspensão em background
        if (previousMode == Mode.ULTRA && mode != Mode.ULTRA) {
            UltraBatterySaver.unsuspendAllSuspendedApps(context) {
                // Opcional: notificar que a desuspensão terminou
                Toast.makeText(context, "Apps restaurados.", Toast.LENGTH_SHORT).show()
            }
        }

        when (mode) {
            Mode.PERFORMANCE, Mode.STANDARD -> {
                // Fecha o launcher ultra se estiver ativo
                try { LauncherManager.finishIfActive() } catch (_: Exception) {}
                val launcherComponent = getDefaultLauncherComponent(context)
                if (launcherComponent != null) {
                    if (!setHomeActivityRoot(context, launcherComponent)) {
                        Toast.makeText(context, "Falha ao trocar launcher. Verifique o acesso root.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Nenhum launcher padrão definido. Use a engrenagem para escolher.", Toast.LENGTH_SHORT).show()
                }
                Toast.makeText(context, "Modo ${mode.displayName} aplicado", Toast.LENGTH_SHORT).show()
            }
            Mode.ULTRA -> {
                // Define o nosso launcher primeiro (para segurança)
                activateUltraMode(context)

                // Depois suspende os apps não permitidos em background
                if (RootCommands.isRootAvailable()) {
                    UltraBatterySaver.suspendNonAllowedApps(context) {
                        // Opcional: Toast ao terminar
                        Toast.makeText(context, "Apps suspensos.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Root necessário para suspender apps.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return true
    }

    private fun activateUltraMode(context: Context) {
        val ourLauncher = "${context.packageName}/.LauncherActivity"
        if (!setHomeActivityRoot(context, ourLauncher)) {
            Toast.makeText(context, "Falha ao definir launcher Ultra. Verifique o root.", Toast.LENGTH_SHORT).show()
        }
        Toast.makeText(context, "Modo ${Mode.ULTRA.displayName} aplicado", Toast.LENGTH_SHORT).show()
    }

    private fun setHomeActivityRoot(context: Context, component: String): Boolean {
        if (!RootCommands.isRootAvailable()) {
            Toast.makeText(context, "Acesso root não disponível.", Toast.LENGTH_SHORT).show()
            return false
        }
        val success = RootCommands.run("cmd package set-home-activity $component")
        if (!success) {
            Toast.makeText(context, "Comando root falhou.", Toast.LENGTH_SHORT).show()
        }
        return success
    }
}