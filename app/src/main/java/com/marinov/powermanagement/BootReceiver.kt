package com.marinov.powermanagement

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.marinov.powermanagement.tiles.PerformanceTileService
import com.marinov.powermanagement.tiles.StandardTileService
import com.marinov.powermanagement.tiles.UltraTileService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action ||
            Intent.ACTION_MY_PACKAGE_REPLACED == intent.action) {

            val tileClasses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                listOf(
                    PerformanceTileService::class.java,
                    StandardTileService::class.java,
                    UltraTileService::class.java
                )
            } else {
                TODO("VERSION.SDK_INT < N")
            }
            tileClasses.forEach { cls ->
                val cn = ComponentName(context, cls)
                TileService.requestListeningState(context, cn)
            }
        }
    }
}