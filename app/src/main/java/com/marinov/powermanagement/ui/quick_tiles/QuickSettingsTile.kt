package com.marinov.powermanagement.ui.quick_tiles

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.marinov.powermanagement.R
import com.marinov.powermanagement.core.ModeLogic
import com.marinov.powermanagement.core.TaskerLogic
import com.marinov.powermanagement.core.TaskerLogic.Mode
import com.marinov.powermanagement.ui.AppChooserActivity
import com.marinov.powermanagement.ultra.UltraBatterySaver

abstract class BaseTileService : TileService() {

    protected abstract val mode: Mode
    protected abstract val modeNameResId: Int

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

        // Guarda 1: perfil de kernel configurado?
        val data = TaskerLogic.getProfileData(this, mode)

        if (data == null) {
            val modeName = getString(modeNameResId)

            Toast.makeText(
                this,
                getString(R.string.configure_mode_first, modeName),
                Toast.LENGTH_SHORT
            ).show()

            qsTile?.state = Tile.STATE_INACTIVE
            qsTile?.updateTile()
            return
        }

        // Guarda 2 (exclusiva do Ultra): lista de apps permitidos configurada?
        if (mode == Mode.ULTRA && !UltraBatterySaver.isUltraSetupComplete(this)) {
            qsTile?.state = Tile.STATE_INACTIVE
            qsTile?.updateTile()
            openAppChooser()
            return
        }

        ModeLogic.applyModeWithLauncher(this, mode)

        qsTile?.state = Tile.STATE_INACTIVE
        qsTile?.updateTile()

        collapseStatusBar()
    }

    private fun openAppChooser() {
        val intent = Intent(this, AppChooserActivity::class.java).apply {
            putExtra("ultra_setup", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                1,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun collapseStatusBar() {
        val intent = Intent(this, CollapseActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

class PerformanceTileService : BaseTileService() {
    override val mode = Mode.PERFORMANCE
    override val modeNameResId = R.string.mode_tile_performance
}

class StandardTileService : BaseTileService() {
    override val mode = Mode.STANDARD
    override val modeNameResId = R.string.mode_tile_standard
}

class UltraTileService : BaseTileService() {
    override val mode = Mode.ULTRA
    override val modeNameResId = R.string.mode_tile_ultra
}