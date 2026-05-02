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
        tvModeTitle.text = "Configurações do modo ${mode.displayName}"
        tvModeSubtitle.text = when (mode) {
            Mode.PERFORMANCE -> "Máximo desempenho para jogos e tarefas pesadas"
            Mode.STANDARD -> "Equilíbrio ideal entre desempenho e economia"
            Mode.ULTRA -> "Economia extrema de bateria"
        }

        // Altera perfil de kernel
        findViewById<MaterialCardView>(R.id.btn_change_kernel).setOnClickListener {
            if (TaskerLogic.isPluginAvailable(this)) {
                TaskerLogic.requestProfilePicker(this, REQ_KERNEL_PROFILE)
            } else {
                Toast.makeText(this, "Smartpack Kernel Manager não instalado!", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this, "Não é possível alterar os apps permitidos enquanto o modo Ultra está ativo. Saia do modo Ultra primeiro.", Toast.LENGTH_LONG).show()
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
            val name = TaskerLogic.extractProfileName(data) ?: "Desconhecido"
            val profileData = TaskerLogic.extractProfileData(data) ?: return
            val versionCode = TaskerLogic.extractVersionCode(data)

            TaskerLogic.saveProfile(this, mode, name, profileData, versionCode)
            Toast.makeText(this, "Perfil '$name' salvo com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}