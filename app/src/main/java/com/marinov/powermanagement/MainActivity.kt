package com.marinov.powermanagement

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.marinov.powermanagement.TaskerLogic.Mode

class MainActivity : AppCompatActivity() {

    private lateinit var rbPerformance: RadioButton
    private lateinit var rbStandard: RadioButton
    private lateinit var rbUltra: RadioButton
    private lateinit var cardPerformance: MaterialCardView
    private lateinit var cardStandard: MaterialCardView
    private lateinit var cardUltra: MaterialCardView

    private lateinit var batteryProgress: CircularProgressIndicator
    private lateinit var tvBatteryPercent: TextView
    private lateinit var tvBatteryTime: TextView

    private var lastAppliedMode: Mode? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

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

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val filter = IntentFilter(TaskerLogic.ACTION_MODE_APPLIED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(modeChangedReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(modeChangedReceiver, filter)
        }

        cardPerformance.setOnClickListener { handleModeSelection(Mode.PERFORMANCE) }
        cardStandard.setOnClickListener { handleModeSelection(Mode.STANDARD) }
        cardUltra.setOnClickListener { handleModeSelection(Mode.ULTRA) }

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
        unregisterReceiver(batteryReceiver)
        try {
            unregisterReceiver(modeChangedReceiver)
        } catch (e: IllegalArgumentException) { }
    }

    private fun requestPermissionsAndRoot() {
        Thread { RootCommands.isRootAvailable() }.start()
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun updateBatteryUI(percent: Int, isCharging: Boolean) {
        batteryProgress.progress = percent
        tvBatteryPercent.text = "$percent%"

        val color = when {
            percent <= 15 -> Color.parseColor("#D32F2F")
            percent <= 40 -> Color.parseColor("#FBC02D")
            else -> Color.parseColor("#388E3C")
        }
        batteryProgress.setIndicatorColor(color)

        if (isCharging) {
            tvBatteryTime.text = "Carregando..."
        } else {
            val timeText = estimateRemainingTime(percent)
            tvBatteryTime.text = timeText
        }
    }

    private fun estimateRemainingTime(percent: Int): String {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val chargeCounter = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val currentAvg = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)

        // Se os dados oficiais estiverem disponíveis, usa‑os
        if (chargeCounter != Long.MIN_VALUE && currentAvg != Long.MIN_VALUE && currentAvg > 0) {
            val hours = chargeCounter.toFloat() / currentAvg
            if (hours > 0 && hours < 100) {
                val h = hours.toInt()
                val m = ((hours - h) * 60).toInt()
                return if (h > 0) "Aprox. ${h}h ${m}min restantes"
                else "Aprox. ${m}min restantes"
            }
        }

        val minutesPerPercent = 6
        val estimatedMinutesTotal = percent * minutesPerPercent
        val hours = estimatedMinutesTotal / 60
        val mins = estimatedMinutesTotal % 60
        return if (hours > 0) "Aprox. ${hours}h ${mins}min (estimativa)"
        else "Aprox. ${mins}min (estimativa)"
    }

    private fun handleModeSelection(mode: Mode) {
        if (mode == Mode.ULTRA && !UltraBatterySaver.isUltraSetupComplete(this)) {
            startActivity(Intent(this, UltraSetupActivity::class.java))
            return
        }
        val success = ModeLogic.applyModeWithLauncher(this, mode)
        if (success) {
            markCurrentMode()
            if (mode == Mode.ULTRA) {
                startActivity(Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
            finish()
        } else {
            markCurrentMode()
            Toast.makeText(this, "Configure o perfil de kernel primeiro nas engrenagens.", Toast.LENGTH_LONG).show()
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
}