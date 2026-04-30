package com.marinov.powermanagement

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.marinov.powermanagement.TaskerLogic.Mode

object ModeLogic {

    fun applyModeWithLauncher(context: Context, mode: Mode): Boolean {
        val data = TaskerLogic.getProfileData(context, mode)
        if (data == null) return false

        val version = TaskerLogic.getVersionCode(context, mode)
        TaskerLogic.applyProfile(context, data, version)
        TaskerLogic.saveLastAppliedMode(context, mode)

        // Notifica interessados (MainActivity)
        val intent = Intent(TaskerLogic.ACTION_MODE_APPLIED)
        intent.setPackage(context.packageName)
        context.sendBroadcast(intent)

        when (mode) {
            Mode.PERFORMANCE, Mode.STANDARD -> {
                try { LauncherManager.finishIfActive() } catch (_: Exception) {}
                val launcherComponent = UltraBatterySaver.getDefaultLauncherComponent(context)
                if (launcherComponent != null) {
                    if (!setHomeActivityRoot(context, launcherComponent)) {
                        Toast.makeText(context, "Falha ao trocar launcher. Verifique o acesso root.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Nenhum launcher padrão definido. Use a engrenagem para escolher.", Toast.LENGTH_SHORT).show()
                }
                Toast.makeText(context, "Modo ${mode.name} aplicado", Toast.LENGTH_SHORT).show()
            }
            Mode.ULTRA -> {
                activateUltraMode(context)
            }
        }
        return true
    }

    private fun activateUltraMode(context: Context) {
        val ourLauncher = "${context.packageName}/.LauncherActivity"
        if (!setHomeActivityRoot(context, ourLauncher)) {
            Toast.makeText(context, "Falha ao definir launcher Ultra. Verifique o root.", Toast.LENGTH_SHORT).show()
        }
        Toast.makeText(context, "Modo Ultra-Econômico aplicado", Toast.LENGTH_SHORT).show()
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