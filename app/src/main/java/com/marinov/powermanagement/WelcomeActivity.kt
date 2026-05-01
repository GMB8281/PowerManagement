package com.marinov.powermanagement

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.marinov.powermanagement.TaskerLogic.Mode

class WelcomeActivity : AppCompatActivity() {

    private lateinit var cardPerformance: MaterialCardView
    private lateinit var cardStandard: MaterialCardView
    private lateinit var cardUltra: MaterialCardView
    private lateinit var tvStatusPerformance: TextView
    private lateinit var tvStatusStandard: TextView
    private lateinit var tvStatusUltra: TextView
    private lateinit var btnNext: Button

    private val configuredModes = mutableSetOf<Mode>()
    private var currentModeForPicker: Mode? = null

    private val profilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val mode = currentModeForPicker ?: return@registerForActivityResult
            val name = TaskerLogic.extractProfileName(result.data) ?: "Desconhecido"
            val data = TaskerLogic.extractProfileData(result.data) ?: return@registerForActivityResult
            val version = TaskerLogic.extractVersionCode(result.data)

            TaskerLogic.saveProfile(this, mode, name, data, version)
            configuredModes.add(mode)
            updateStatusIndicators()
            checkAllConfigured()
            Toast.makeText(this, "Perfil '$name' salvo para ${mode.displayName}", Toast.LENGTH_SHORT).show()
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

        // Pré‑marca os modos que já possuem perfil salvo (pode recomeçar)
        for (mode in Mode.values()) {
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
            showLauncherPickerAndFinish()
        }
    }

    private fun launchProfilePicker(mode: Mode) {
        if (!TaskerLogic.isPluginAvailable(this)) {
            Toast.makeText(this, "Smartpack Kernel Manager não instalado!", Toast.LENGTH_SHORT).show()
            return
        }
        currentModeForPicker = mode
        val intent = Intent().apply {
            setClassName("com.smartpack.kernelmanager", "com.smartpack.kernelmanager.activities.tools.profile.ProfileTaskerActivity")
        }
        profilePickerLauncher.launch(intent)
    }

    private fun updateStatusIndicators() {
        tvStatusPerformance.text = if (Mode.PERFORMANCE in configuredModes) "✓ Perfil selecionado" else "Toque para selecionar"
        tvStatusStandard.text = if (Mode.STANDARD in configuredModes) "✓ Perfil selecionado" else "Toque para selecionar"
        tvStatusUltra.text = if (Mode.ULTRA in configuredModes) "✓ Perfil selecionado" else "Toque para selecionar"
    }

    private fun checkAllConfigured() {
        btnNext.isEnabled = configuredModes.size == Mode.values().size
    }

    private fun showLauncherPickerAndFinish() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        val launcherItems = resolveInfos.map { info ->
            val label = info.loadLabel(pm).toString()
            val component = "${info.activityInfo.packageName}/${info.activityInfo.name}"
            Pair(label, component)
        }

        val labels = launcherItems.map { it.first }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Escolher Launcher Principal")
            .setSingleChoiceItems(labels, -1) { dialog, which ->
                val selectedComponent = launcherItems[which].second
                val parts = selectedComponent.split("/")
                if (parts.size == 2) {
                    UltraBatterySaver.setDefaultLauncher(this, parts[0], parts[1])
                }
                dialog.dismiss()
                finishSetup()
            }
            .setCancelable(false)
            .show()
    }

    private fun finishSetup() {
        UltraBatterySaver.setInitialSetupComplete(this, true)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}