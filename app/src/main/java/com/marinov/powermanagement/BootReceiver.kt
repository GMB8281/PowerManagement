package com.marinov.powermanagement

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService
import com.marinov.powermanagement.tiles.PerformanceTileService
import com.marinov.powermanagement.tiles.StandardTileService
import com.marinov.powermanagement.tiles.UltraTileService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action ||
            Intent.ACTION_MY_PACKAGE_REPLACED == intent.action) {

            // Reativar tiles
            val tileClasses = listOf(
                PerformanceTileService::class.java,
                StandardTileService::class.java,
                UltraTileService::class.java
            )
            tileClasses.forEach { cls ->
                val cn = ComponentName(context, cls)
                TileService.requestListeningState(context, cn)
            }
            val lastMode = TaskerLogic.getLastAppliedMode(context)
            if (lastMode != TaskerLogic.Mode.ULTRA) {
                UltraBatterySaver.unsuspendAllSuspendedApps(context)
            }
        }
    }
}