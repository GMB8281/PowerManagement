package com.marinov.powermanagement.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.marinov.powermanagement.R
import com.marinov.powermanagement.core.LauncherRepository
import com.marinov.powermanagement.core.ModeLogic
import com.marinov.powermanagement.core.TaskerLogic
import com.marinov.powermanagement.core.TaskerLogic.Mode
import com.marinov.powermanagement.model.LauncherInfo

class WelcomeActivity : AppCompatActivity() {

    private lateinit var cardPerformance: MaterialCardView
    private lateinit var cardStandard: MaterialCardView
    private lateinit var cardUltra: MaterialCardView

    private lateinit var tvStatusPerformance: TextView
    private lateinit var tvStatusStandard: TextView
    private lateinit var tvStatusUltra: TextView

    private lateinit var btnNext: Button
    private lateinit var layoutModes: LinearLayout
    private lateinit var layoutLauncher: LinearLayout
    private lateinit var rvLauncherWelcome: RecyclerView

    private val configuredModes = mutableSetOf<Mode>()
    private var currentModeForPicker: Mode? = null
    private var selectedLauncherInfo: LauncherInfo? = null

    private lateinit var welcomeLauncherAdapter: LauncherAdapter

    private val profilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val mode = currentModeForPicker ?: return@registerForActivityResult

            val name = TaskerLogic.extractProfileName(result.data)
                ?: getString(R.string.unknown_profile)

            val data = TaskerLogic.extractProfileData(result.data)
                ?: return@registerForActivityResult

            val version = TaskerLogic.extractVersionCode(result.data)

            TaskerLogic.saveProfile(this, mode, name, data, version)

            configuredModes.add(mode)
            updateStatusIndicators()
            checkAllConfigured()

            Toast.makeText(
                this,
                getString(R.string.profile_saved_toast, name),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        cardPerformance = findViewById(R.id.card_performance_welcome)
        cardStandard = findViewById(R.id.card_standard_welcome)
        cardUltra = findViewById(R.id.card_ultra_welcome)

        tvStatusPerformance = findViewById(R.id.tv_status_performance)
        tvStatusStandard = findViewById(R.id.tv_status_standard)
        tvStatusUltra = findViewById(R.id.tv_status_ultra)

        btnNext = findViewById(R.id.btn_next)
        layoutModes = findViewById(R.id.layout_modes)
        layoutLauncher = findViewById(R.id.layout_launcher_selection)
        rvLauncherWelcome = findViewById(R.id.rv_launcher_welcome)

        layoutLauncher.visibility = View.GONE

        for (mode in Mode.entries) {
            if (TaskerLogic.getProfileData(this, mode) != null) {
                configuredModes.add(mode)
            }
        }

        updateStatusIndicators()
        checkAllConfigured()

        cardPerformance.setOnClickListener { launchProfilePicker(Mode.PERFORMANCE) }
        cardStandard.setOnClickListener { launchProfilePicker(Mode.STANDARD) }
        cardUltra.setOnClickListener { launchProfilePicker(Mode.ULTRA) }

        btnNext.setOnClickListener {
            if (configuredModes.size == Mode.entries.size && !layoutLauncher.isVisible) {
                showLauncherSelectionStep()
            } else if (layoutLauncher.isVisible) {
                finishSetup()
            }
        }
    }

    private fun launchProfilePicker(mode: Mode) {
        if (!TaskerLogic.isPluginAvailable(this)) {
            Toast.makeText(this, R.string.smartpack_not_installed, Toast.LENGTH_SHORT).show()
            return
        }

        currentModeForPicker = mode

        val intent = Intent().apply {
            setClassName(
                "com.smartpack.kernelmanager",
                "com.smartpack.kernelmanager.activities.tools.profile.ProfileTaskerActivity"
            )
        }

        profilePickerLauncher.launch(intent)
    }

    private fun updateStatusIndicators() {
        tvStatusPerformance.text = if (Mode.PERFORMANCE in configuredModes) {
            getString(R.string.profile_selected)
        } else {
            getString(R.string.tap_to_select)
        }

        tvStatusStandard.text = if (Mode.STANDARD in configuredModes) {
            getString(R.string.profile_selected)
        } else {
            getString(R.string.tap_to_select)
        }

        tvStatusUltra.text = if (Mode.ULTRA in configuredModes) {
            getString(R.string.profile_selected)
        } else {
            getString(R.string.tap_to_select)
        }
    }

    private fun checkAllConfigured() {
        btnNext.isEnabled = configuredModes.size == Mode.entries.size
    }

    private fun showLauncherSelectionStep() {
        layoutModes.visibility = View.GONE
        layoutLauncher.visibility = View.VISIBLE
        btnNext.text = getString(R.string.welcome_finish)
        btnNext.isEnabled = false

        val currentPkg = LauncherRepository.getCurrentDefaultPackage(this)
        val launchers = LauncherRepository.getHomeLaunchers(this)

        if (currentPkg != null) {
            selectedLauncherInfo = launchers.find { it.packageName == currentPkg }
        }

        welcomeLauncherAdapter = LauncherAdapter(launchers) { launcher ->
            ModeLogic.setDefaultLauncher(this, launcher.packageName, launcher.activityName)
            selectedLauncherInfo = launcher
            welcomeLauncherAdapter.selectedPackage = launcher.packageName
            btnNext.isEnabled = true
        }

        welcomeLauncherAdapter.selectedPackage = selectedLauncherInfo?.packageName

        rvLauncherWelcome.layoutManager = LinearLayoutManager(this)
        rvLauncherWelcome.adapter = welcomeLauncherAdapter
    }

    private fun finishSetup() {
        if (configuredModes.size != Mode.entries.size) return

        if (layoutLauncher.isVisible && selectedLauncherInfo == null) {
            Toast.makeText(this, R.string.select_launcher_first, Toast.LENGTH_SHORT).show()
            return
        }

        ModeLogic.setInitialSetupComplete(this, true)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}