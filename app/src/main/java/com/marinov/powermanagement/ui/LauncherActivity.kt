package com.marinov.powermanagement.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import com.google.android.material.appbar.MaterialToolbar
import com.marinov.powermanagement.R
import com.marinov.powermanagement.core.LauncherManager
import com.marinov.powermanagement.core.ModeLogic
import com.marinov.powermanagement.core.TaskerLogic
import com.marinov.powermanagement.core.TaskerLogic.Mode
import com.marinov.powermanagement.ultra.UltraAppPolicy
import com.marinov.powermanagement.ultra.UltraBatterySaver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class LauncherActivity : AppCompatActivity() {

    companion object {
        private const val SWIPE_THRESHOLD = 120f
        private const val SWIPE_VELOCITY_THRESHOLD = 120f
    }

    private lateinit var tvTime: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvDayOfWeek: TextView
    private lateinit var tvPageIndicator: TextView
    private lateinit var viewFlipper: ViewFlipper

    private val imageViews = arrayOfNulls<ImageView>(24)
    private val textViews = arrayOfNulls<TextView>(24)

    private val handler = Handler(Looper.getMainLooper())

    private val timeDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())

    private var hasSecondPage = false

    // BUG 1 FIX: hiddenPackages agora inclui Magisk e VPNs detectadas dinamicamente.
    // Lazy garante que é calculado uma única vez, após o contexto estar disponível.
    private val hiddenPackages: Set<String> by lazy {
        UltraAppPolicy.getHiddenPackageNames(this)
    }

    private lateinit var gestureDetector: GestureDetectorCompat

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
        tvPageIndicator = findViewById(R.id.tv_page_indicator)
        viewFlipper = findViewById(R.id.view_flipper)

        setupAppViews()
        setupGestureDetector()

        loadAllowedApps()
        updateClock()
        updatePageIndicator()
    }

    private fun setupAppViews() {
        val imageIds = intArrayOf(
            R.id.app_slot_1,
            R.id.app_slot_2,
            R.id.app_slot_3,
            R.id.app_slot_4,
            R.id.app_slot_5,
            R.id.app_slot_6,
            R.id.app_slot_7,
            R.id.app_slot_8,
            R.id.app_slot_9,
            R.id.app_slot_10,
            R.id.app_slot_11,
            R.id.app_slot_12,
            R.id.app_slot_13,
            R.id.app_slot_14,
            R.id.app_slot_15,
            R.id.app_slot_16,
            R.id.app_slot_17,
            R.id.app_slot_18,
            R.id.app_slot_19,
            R.id.app_slot_20,
            R.id.app_slot_21,
            R.id.app_slot_22,
            R.id.app_slot_23,
            R.id.app_slot_24
        )

        val labelIds = intArrayOf(
            R.id.app_label_1,
            R.id.app_label_2,
            R.id.app_label_3,
            R.id.app_label_4,
            R.id.app_label_5,
            R.id.app_label_6,
            R.id.app_label_7,
            R.id.app_label_8,
            R.id.app_label_9,
            R.id.app_label_10,
            R.id.app_label_11,
            R.id.app_label_12,
            R.id.app_label_13,
            R.id.app_label_14,
            R.id.app_label_15,
            R.id.app_label_16,
            R.id.app_label_17,
            R.id.app_label_18,
            R.id.app_label_19,
            R.id.app_label_20,
            R.id.app_label_21,
            R.id.app_label_22,
            R.id.app_label_23,
            R.id.app_label_24
        )

        for (i in 0 until 24) {
            imageViews[i] = findViewById(imageIds[i])
            textViews[i] = findViewById(labelIds[i])
        }
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetectorCompat(
            this,
            object : GestureDetector.SimpleOnGestureListener() {

                override fun onDown(e: MotionEvent): Boolean {
                    return true
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 == null) return false

                    val diffX = e2.x - e1.x

                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            showPreviousPage()
                        } else {
                            showNextPage()
                        }
                        return true
                    }

                    return false
                }
            }
        )
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun showPreviousPage() {
        if (viewFlipper.displayedChild > 0) {
            viewFlipper.showPrevious()
            updatePageIndicator()
        }
    }

    private fun showNextPage() {
        if (hasSecondPage && viewFlipper.displayedChild < viewFlipper.childCount - 1) {
            viewFlipper.showNext()
            updatePageIndicator()
        }
    }

    private fun updatePageIndicator() {
        tvPageIndicator.text = if (hasSecondPage) {
            "${viewFlipper.displayedChild + 1}/2"
        } else {
            "1/1"
        }
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
            .take(24)

        hasSecondPage = allowedPackages.size > 12

        if (!hasSecondPage && viewFlipper.displayedChild != 0) {
            viewFlipper.displayedChild = 0
        }

        for (i in 0 until 24) {
            val iconView = imageViews[i] ?: continue
            val labelView = textViews[i] ?: continue

            if (i < allowedPackages.size) {
                val packageName = allowedPackages[i]

                try {
                    val appInfo = pm.getApplicationInfo(
                        packageName,
                        android.content.pm.PackageManager.GET_META_DATA
                    )

                    val icon = pm.getApplicationIcon(appInfo)
                    val label = pm.getApplicationLabel(appInfo).toString()

                    iconView.setImageDrawable(icon)
                    labelView.text = label
                    iconView.contentDescription = label

                    iconView.setOnClickListener {
                        try {
                            val intent = pm.getLaunchIntentForPackage(packageName)

                            if (intent != null) {
                                startActivity(intent)
                            } else {
                                Toast.makeText(this, R.string.app_launch_error, Toast.LENGTH_SHORT).show()
                            }
                        } catch (_: Exception) {
                            Toast.makeText(this, R.string.app_launch_error_generic, Toast.LENGTH_SHORT).show()
                        }
                    }

                } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
                    setEmptySlot(iconView, labelView)
                }
            } else {
                setEmptySlot(iconView, labelView)
            }
        }

        updatePageIndicator()
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

        // BUG 3 FIX: se o launcher aparecer na tela (inclusive via recentes) mas o modo
        // Ultra não estiver mais ativo, fecha imediatamente para evitar estado inconsistente.
        if (TaskerLogic.getLastAppliedMode(this) != Mode.ULTRA) {
            finish()
            return
        }

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