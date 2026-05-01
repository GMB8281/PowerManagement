package com.marinov.powermanagement.tiles

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.marinov.powermanagement.ModeLogic
import com.marinov.powermanagement.TaskerLogic
import com.marinov.powermanagement.TaskerLogic.Mode

@RequiresApi(Build.VERSION_CODES.N)
abstract class BaseTileService : TileService() {

    protected abstract val mode: Mode
    protected abstract val modeName: String

    override fun onTileAdded() {
        super.onTileAdded()
        qsTile?.state = Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.state = Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }

    override fun onClick() {
        super.onClick()

        val data = TaskerLogic.getProfileData(this, mode)
        if (data == null) {
            Toast.makeText(this, "Configure primeiro o perfil para o modo $modeName", Toast.LENGTH_SHORT).show()
        } else {
            ModeLogic.applyModeWithLauncher(this, mode)
        }

        qsTile?.state = Tile.STATE_INACTIVE
        qsTile?.updateTile()

        collapseStatusBar()
    }

    private fun collapseStatusBar() {
        val intent = Intent(this, CollapseActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // API 34+: startActivityAndCollapse(Intent) foi depreciado, usa PendingIntent
            val pending = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pending)
        } else {
            // API 24–33
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.N)
class PerformanceTileService : BaseTileService() {
    override val mode = Mode.PERFORMANCE
    override val modeName = "Performance"
}

@RequiresApi(Build.VERSION_CODES.N)
class StandardTileService : BaseTileService() {
    override val mode = Mode.STANDARD
    override val modeName = "Padrão"
}

@RequiresApi(Build.VERSION_CODES.N)
class UltraTileService : BaseTileService() {
    override val mode = Mode.ULTRA
    override val modeName = "Ultra‑Econômico"
}