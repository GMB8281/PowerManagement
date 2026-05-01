package com.marinov.powermanagement

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
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

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = "Configurações do modo ${mode.displayName}"
        toolbar.setNavigationOnClickListener { finish() }

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
            findViewById<LinearLayout>(R.id.layout_ultra_settings).visibility = View.VISIBLE
            findViewById<MaterialCardView>(R.id.btn_choose_apps).setOnClickListener {
                startActivity(Intent(this, AppChooserActivity::class.java))
            }
            // Removido o btn_choose_launcher
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
}