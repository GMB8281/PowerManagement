package com.marinov.powermanagement

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.marinov.powermanagement.TaskerLogic.Mode

class ModeSettingsActivity : AppCompatActivity() {

    private lateinit var mode: Mode

    companion object {
        private const val REQ_KERNEL_PROFILE = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mode_settings)

        val modeString = intent.getStringExtra("MODE") ?: return finish()
        mode = Mode.valueOf(modeString)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Configura o título grande e subtítulo
        val tvModeTitle = findViewById<TextView>(R.id.tv_mode_title)
        val tvModeSubtitle = findViewById<TextView>(R.id.tv_mode_subtitle)
        tvModeTitle.text = getString(R.string.mode_settings_title, mode.displayName)
        tvModeSubtitle.text = when (mode) {
            Mode.PERFORMANCE -> getString(R.string.mode_settings_subtitle_performance)
            Mode.STANDARD -> getString(R.string.mode_settings_subtitle_standard)
            Mode.ULTRA -> getString(R.string.mode_settings_subtitle_ultra)
        }

        // Altera perfil de kernel
        findViewById<MaterialCardView>(R.id.btn_change_kernel).setOnClickListener {
            if (TaskerLogic.isPluginAvailable(this)) {
                TaskerLogic.requestProfilePicker(this, REQ_KERNEL_PROFILE)
            } else {
                Toast.makeText(this, R.string.smartpack_not_installed, Toast.LENGTH_SHORT).show()
            }
        }

        // Configurações específicas do Ultra
        if (mode == Mode.ULTRA) {
            val layoutUltra = findViewById<LinearLayout>(R.id.layout_ultra_settings)
            layoutUltra.visibility = View.VISIBLE

            val btnChooseApps = findViewById<MaterialCardView>(R.id.btn_choose_apps)
            val isUltraActive = TaskerLogic.getLastAppliedMode(this) == Mode.ULTRA

            if (isUltraActive) {
                btnChooseApps.alpha = 0.5f
                btnChooseApps.isClickable = false
                btnChooseApps.setOnClickListener(null)
                btnChooseApps.setOnLongClickListener {
                    Toast.makeText(this, R.string.cant_change_ultra_active, Toast.LENGTH_LONG).show()
                    true
                }
            } else {
                btnChooseApps.setOnClickListener {
                    startActivity(Intent(this, AppChooserActivity::class.java))
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_KERNEL_PROFILE && resultCode == RESULT_OK && data != null) {
            val name = TaskerLogic.extractProfileName(data) ?: getString(R.string.unknown_profile)
            val profileData = TaskerLogic.extractProfileData(data) ?: return
            val versionCode = TaskerLogic.extractVersionCode(data)

            TaskerLogic.saveProfile(this, mode, name, profileData, versionCode)
            Toast.makeText(
                this,
                getString(R.string.profile_saved_toast, name),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}