package com.marinov.powermanagement

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.marinov.powermanagement.TaskerLogic.Mode
import java.text.SimpleDateFormat
import java.util.*

class LauncherActivity : AppCompatActivity() {

    private lateinit var tvTime: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvDayOfWeek: TextView
    private lateinit var gridApps: GridLayout

    private val imageViews = arrayOfNulls<ImageView>(12)
    private val textViews = arrayOfNulls<TextView>(12)

    private val handler = Handler(Looper.getMainLooper())
    private val timeDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())

    private val hiddenPackages = setOf(
        "com.android.vending",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.smartpack.kernelmanager"   // ← oculto no launcher
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (TaskerLogic.getLastAppliedMode(this) != Mode.ULTRA) {
            Toast.makeText(this, R.string.ultra_disabled_toast, Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            finish()
            return
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Não faz nada, bloqueia o botão voltar
            }
        })

        setContentView(R.layout.activity_launcher)

        LauncherManager.currentActivity = this

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        tvTime = findViewById(R.id.tv_time)
        tvDate = findViewById(R.id.tv_date)
        tvDayOfWeek = findViewById(R.id.tv_day_of_week)
        gridApps = findViewById(R.id.grid_apps)

        imageViews[0] = findViewById(R.id.app_slot_1)
        imageViews[1] = findViewById(R.id.app_slot_2)
        imageViews[2] = findViewById(R.id.app_slot_3)
        imageViews[3] = findViewById(R.id.app_slot_4)
        imageViews[4] = findViewById(R.id.app_slot_5)
        imageViews[5] = findViewById(R.id.app_slot_6)
        imageViews[6] = findViewById(R.id.app_slot_7)
        imageViews[7] = findViewById(R.id.app_slot_8)
        imageViews[8] = findViewById(R.id.app_slot_9)
        imageViews[9] = findViewById(R.id.app_slot_10)
        imageViews[10] = findViewById(R.id.app_slot_11)
        imageViews[11] = findViewById(R.id.app_slot_12)

        textViews[0] = findViewById(R.id.app_label_1)
        textViews[1] = findViewById(R.id.app_label_2)
        textViews[2] = findViewById(R.id.app_label_3)
        textViews[3] = findViewById(R.id.app_label_4)
        textViews[4] = findViewById(R.id.app_label_5)
        textViews[5] = findViewById(R.id.app_label_6)
        textViews[6] = findViewById(R.id.app_label_7)
        textViews[7] = findViewById(R.id.app_label_8)
        textViews[8] = findViewById(R.id.app_label_9)
        textViews[9] = findViewById(R.id.app_label_10)
        textViews[10] = findViewById(R.id.app_label_11)
        textViews[11] = findViewById(R.id.app_label_12)

        loadAllowedApps()
        updateClock()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_launcher, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_exit) {
            ModeLogic.applyModeWithLauncher(this, Mode.STANDARD)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadAllowedApps() {
        val pm = packageManager
        val allowedPackages = UltraBatterySaver.getAllowedApps(this)
            .filterNot { it in hiddenPackages }
            .take(12)

        for (i in 0 until 12) {
            val iconView = imageViews[i] ?: continue
            val labelView = textViews[i] ?: continue

            if (i < allowedPackages.size) {
                val packageName = allowedPackages[i]
                try {
                    val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                    val icon = pm.getApplicationIcon(appInfo)
                    val label = pm.getApplicationLabel(appInfo).toString()
                    iconView.setImageDrawable(icon)
                    labelView.text = label
                    iconView.contentDescription = label
                    iconView.setOnClickListener {
                        try {
                            val intent = pm.getLaunchIntentForPackage(packageName)
                            if (intent != null) startActivity(intent)
                            else Toast.makeText(this, R.string.app_launch_error, Toast.LENGTH_SHORT).show()
                        } catch (_: Exception) {
                            Toast.makeText(this, R.string.app_launch_error_generic, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (_: PackageManager.NameNotFoundException) {
                    setEmptySlot(iconView, labelView)
                }
            } else {
                setEmptySlot(iconView, labelView)
            }
        }
    }

    private fun setEmptySlot(icon: ImageView, label: TextView) {
        icon.setImageDrawable(null)
        icon.contentDescription = null
        icon.setOnClickListener(null)
        label.text = ""
    }

    override fun onDestroy() {
        super.onDestroy()
        LauncherManager.currentActivity = null
    }

    override fun onResume() {
        super.onResume()
        updateClock()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000)
        }
    }

    private fun updateClock() {
        val now = Date()
        tvTime.text = timeDateFormat.format(now)
        tvDate.text = dateFormat.format(now)
        tvDayOfWeek.text = dayOfWeekFormat.format(now)
    }
}