package com.marinov.powermanagement.core

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.TileService
import com.marinov.powermanagement.ui.quick_tiles.PerformanceTileService
import com.marinov.powermanagement.ui.quick_tiles.StandardTileService
import com.marinov.powermanagement.ui.quick_tiles.UltraTileService
import com.marinov.powermanagement.ultra.UltraBatterySaver

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action ||
            Intent.ACTION_MY_PACKAGE_REPLACED == intent.action
        ) {
            val tileClasses = listOf(
                PerformanceTileService::class.java,
                StandardTileService::class.java,
                UltraTileService::class.java
            )

            tileClasses.forEach { cls ->
                val cn = ComponentName(context, cls)
                TileService.requestListeningState(context, cn)
            }

            val appContext = context.applicationContext
            val pendingResult = goAsync()
            val mainHandler = Handler(Looper.getMainLooper())

            Thread {
                try {
                    val lastMode = TaskerLogic.getLastAppliedMode(appContext)

                    when (lastMode) {
                        TaskerLogic.Mode.ULTRA -> {
                            /*
                               Reversão inteligente do Ultra no boot:
                               - O modo persistido volta a ser Standard.
                               - Não reaplicamos perfil de kernel aqui, pois o dispositivo
                                 acabou de iniciar e o usuário não espera novo apply de perfil.
                               - Todos os apps suspensos são restaurados.
                               - O launcher padrão escolhido pelo usuário é restaurado.
                            */
                            TaskerLogic.saveLastAppliedMode(appContext, TaskerLogic.Mode.STANDARD)
                            UltraBatterySaver.unsuspendAllSuspendedAppsSync(appContext)
                            ModeLogic.restoreUserLauncherSilent(appContext)
                        }

                        TaskerLogic.Mode.PERFORMANCE -> {
                            /*
                               Correção de UI após reboot:
                               Se o aparelho reiniciar em Performance, a UI mostraria Performance
                               selecionado, mas o estado real não deve permanecer assim.
                               Forçamos o estado persistido para Standard.
                            */
                            TaskerLogic.saveLastAppliedMode(appContext, TaskerLogic.Mode.STANDARD)

                            // Por segurança, também limpa qualquer suspensão residual.
                            UltraBatterySaver.unsuspendAllSuspendedAppsSync(appContext)
                        }

                        else -> {
                            /*
                               Comportamento conservador:
                               Se houver apps suspensos em qualquer outro estado inconsistente,
                               remove as suspensões.
                            */
                            UltraBatterySaver.unsuspendAllSuspendedAppsSync(appContext)
                        }
                    }

                    /*
                       Detalhe adicional solicitado:
                       Se a LauncherActivity do modo Ultra ainda estiver aberta/ativa,
                       força o encerramento dela na thread principal.
                    */
                    mainHandler.post {
                        try {
                            LauncherManager.finishIfActive()
                        } catch (_: Exception) {
                            // Evita qualquer crash acidental no boot.
                        }
                    }

                } catch (_: Exception) {
                    // Falhas silenciosas no boot para não quebrar o receiver.
                } finally {
                    pendingResult.finish()
                }
            }.start()
        }
    }
}