package com.marinov.powermanagement.ui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.marinov.powermanagement.R
import com.marinov.powermanagement.core.ModeLogic
import com.marinov.powermanagement.core.TaskerLogic
import com.marinov.powermanagement.core.TaskerLogic.Mode
import com.marinov.powermanagement.ultra.UltraBatterySaver

class MainActivity : AppCompatActivity() {

    private lateinit var rbPerformance: RadioButton
    private lateinit var rbStandard: RadioButton
    private lateinit var rbUltra: RadioButton

    private lateinit var cardPerformance: MaterialCardView
    private lateinit var cardStandard: MaterialCardView
    private lateinit var cardUltra: MaterialCardView
    private lateinit var cardChangeLauncher: MaterialCardView

    private lateinit var batteryProgress: CircularProgressIndicator
    private lateinit var tvBatteryPercent: TextView
    private lateinit var tvBatteryTime: TextView

    private var lastAppliedMode: Mode? = null
    private var isBatteryReceiverRegistered = false
    private var isModeReceiverRegistered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            if (level != -1 && scale != -1) {
                val batteryPct = (level * 100) / scale.toFloat()
                updateBatteryUI(batteryPct.toInt(), isCharging)
            }
        }
    }

    private val modeChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            lastAppliedMode = TaskerLogic.getLastAppliedMode(this@MainActivity)
            markCurrentMode()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ModeLogic.isInitialSetupComplete(this)) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        val currentMode = TaskerLogic.getLastAppliedMode(this)
        if (currentMode != Mode.ULTRA) {
            UltraBatterySaver.unsuspendAllSuspendedApps(this)
        }

        setContentView(R.layout.activity_main)

        requestPermissionsAndRoot()

        batteryProgress = findViewById(R.id.battery_progress)
        tvBatteryPercent = findViewById(R.id.tv_battery_percent)
        tvBatteryTime = findViewById(R.id.tv_battery_time)

        rbPerformance = findViewById(R.id.rb_performance)
        rbStandard = findViewById(R.id.rb_standard)
        rbUltra = findViewById(R.id.rb_ultra)

        cardPerformance = findViewById(R.id.card_performance)
        cardStandard = findViewById(R.id.card_standard)
        cardUltra = findViewById(R.id.card_ultra)
        cardChangeLauncher = findViewById(R.id.card_change_launcher)

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        isBatteryReceiverRegistered = true

        val filter = IntentFilter(TaskerLogic.ACTION_MODE_APPLIED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(modeChangedReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(modeChangedReceiver, filter)
        }

        isModeReceiverRegistered = true

        cardPerformance.setOnClickListener { handleModeSelection(Mode.PERFORMANCE) }
        cardStandard.setOnClickListener { handleModeSelection(Mode.STANDARD) }
        cardUltra.setOnClickListener { handleModeSelection(Mode.ULTRA) }
        cardChangeLauncher.setOnClickListener { showLauncherPicker() }

        findViewById<ImageButton>(R.id.btn_config_performance).setOnClickListener {
            openSettingsScreen(Mode.PERFORMANCE)
        }

        findViewById<ImageButton>(R.id.btn_config_standard).setOnClickListener {
            openSettingsScreen(Mode.STANDARD)
        }

        findViewById<ImageButton>(R.id.btn_config_ultra).setOnClickListener {
            openSettingsScreen(Mode.ULTRA)
        }
    }

    override fun onResume() {
        super.onResume()
        lastAppliedMode = TaskerLogic.getLastAppliedMode(this)
        markCurrentMode()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isBatteryReceiverRegistered) {
            try {
                unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {
            }
        }

        if (isModeReceiverRegistered) {
            try {
                unregisterReceiver(modeChangedReceiver)
            } catch (_: Exception) {
            }
        }
    }

    private fun requestPermissionsAndRoot() {
        Thread { com.marinov.powermanagement.core.RootCommands.isRootAvailable() }.start()
    }

    private fun updateBatteryUI(percent: Int, isCharging: Boolean) {
        batteryProgress.progress = percent
        tvBatteryPercent.text = "$percent%"

        val color = when {
            percent <= 15 -> "#D32F2F".toColorInt()
            percent <= 40 -> "#FBC02D".toColorInt()
            else -> "#388E3C".toColorInt()
        }

        batteryProgress.setIndicatorColor(color)

        tvBatteryTime.text = if (isCharging) {
            getString(R.string.charging)
        } else {
            estimateRemainingTime(percent)
        }
    }

    private fun estimateRemainingTime(percent: Int): String {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val chargeCounter = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val currentAvg = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)

        if (chargeCounter != Long.MIN_VALUE && currentAvg != Long.MIN_VALUE && currentAvg > 0) {
            val hours = chargeCounter.toFloat() / currentAvg

            if (hours > 0 && hours < 100) {
                val h = hours.toInt()
                val m = ((hours - h) * 60).toInt()

                return if (h > 0) {
                    getString(R.string.approx_time_format, h, m)
                } else {
                    getString(R.string.approx_min_format, m)
                }
            }
        }

        val minutesPerPercent = 6
        val estimatedMinutesTotal = percent * minutesPerPercent
        val hours = estimatedMinutesTotal / 60
        val mins = estimatedMinutesTotal % 60

        return if (hours > 0) {
            getString(R.string.estimate_format, hours, mins)
        } else {
            getString(R.string.estimate_min_format, mins)
        }
    }

    private fun handleModeSelection(mode: Mode) {
        if (mode == Mode.ULTRA && !UltraBatterySaver.isUltraSetupComplete(this)) {
            // Em vez de UltraSetupActivity, abre diretamente o seletor de apps
            startActivity(
                Intent(this, AppChooserActivity::class.java).apply {
                    putExtra("ultra_setup", true)
                }
            )
            return // NÃO fecha a MainActivity
        }

        val success = ModeLogic.applyModeWithLauncher(this, mode)

        if (success) {
            markCurrentMode()

            if (mode == Mode.ULTRA) {
                startActivity(
                    Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            }

            finish()
        } else {
            markCurrentMode()
            Toast.makeText(this, R.string.mode_apply_fail, Toast.LENGTH_LONG).show()
        }
    }

    private fun openSettingsScreen(mode: Mode) {
        val intent = Intent(this, ModeSettingsActivity::class.java)
        intent.putExtra("MODE", mode.name)
        startActivity(intent)
    }

    private fun markCurrentMode() {
        rbPerformance.isChecked = lastAppliedMode == Mode.PERFORMANCE
        rbStandard.isChecked = lastAppliedMode == Mode.STANDARD
        rbUltra.isChecked = lastAppliedMode == Mode.ULTRA
    }

    private fun showLauncherPicker() {
        startActivity(Intent(this, LauncherSelectionActivity::class.java))
    }
}