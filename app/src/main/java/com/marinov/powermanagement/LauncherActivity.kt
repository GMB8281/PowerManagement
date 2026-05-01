package com.marinov.powermanagement

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.*
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

    private val imageViews = arrayOfNulls<ImageView>(8)
    private val textViews = arrayOfNulls<TextView>(8)

    private val handler = Handler(Looper.getMainLooper())
    private val timeDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())

    private val hiddenPackages = setOf(
        "com.android.vending",
        "com.google.android.gms",
        "com.google.android.gsf"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        LauncherManager.currentActivity = this

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        tvTime = findViewById(R.id.tv_time)
        tvDate = findViewById(R.id.tv_date)
        tvDayOfWeek = findViewById(R.id.tv_day_of_week)
        gridApps = findViewById(R.id.grid_apps)

        imageViews[0] = findViewById<ImageView>(R.id.app_slot_1)
        imageViews[1] = findViewById<ImageView>(R.id.app_slot_2)
        imageViews[2] = findViewById<ImageView>(R.id.app_slot_3)
        imageViews[3] = findViewById<ImageView>(R.id.app_slot_4)
        imageViews[4] = findViewById<ImageView>(R.id.app_slot_5)
        imageViews[5] = findViewById<ImageView>(R.id.app_slot_6)
        imageViews[6] = findViewById<ImageView>(R.id.app_slot_7)
        imageViews[7] = findViewById<ImageView>(R.id.app_slot_8)

        textViews[0] = findViewById<TextView>(R.id.app_label_1)
        textViews[1] = findViewById<TextView>(R.id.app_label_2)
        textViews[2] = findViewById<TextView>(R.id.app_label_3)
        textViews[3] = findViewById<TextView>(R.id.app_label_4)
        textViews[4] = findViewById<TextView>(R.id.app_label_5)
        textViews[5] = findViewById<TextView>(R.id.app_label_6)
        textViews[6] = findViewById<TextView>(R.id.app_label_7)
        textViews[7] = findViewById<TextView>(R.id.app_label_8)

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
            .take(8)

        for (i in 0 until 8) {
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
                            else Toast.makeText(this, "Não foi possível abrir.", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this, "Erro.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: PackageManager.NameNotFoundException) {
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
        updateClock() // força atualização imediata
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000) // atualiza a cada 1 segundo
        }
    }

    private fun updateClock() {
        val now = Date()
        tvTime.text = timeDateFormat.format(now)
        tvDate.text = dateFormat.format(now)
        tvDayOfWeek.text = dayOfWeekFormat.format(now)
    }
}