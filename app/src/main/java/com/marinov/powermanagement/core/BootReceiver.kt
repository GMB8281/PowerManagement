package com.marinov.powermanagement.core

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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

            val lastMode = TaskerLogic.getLastAppliedMode(context)

            if (lastMode != TaskerLogic.Mode.ULTRA) {
                UltraBatterySaver.unsuspendAllSuspendedApps(context)
            }
        }
    }
}