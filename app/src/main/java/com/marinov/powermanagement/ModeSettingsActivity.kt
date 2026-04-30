package com.marinov.powermanagement

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
        toolbar.setNavigationOnClickListener { finish() }

        findViewById<TextView>(R.id.tv_mode_title).text = "Configurar Modo ${mode.name}"

        findViewById<MaterialCardView>(R.id.btn_change_kernel).setOnClickListener {
            if (TaskerLogic.isPluginAvailable(this)) {
                TaskerLogic.requestProfilePicker(this, REQ_KERNEL_PROFILE)
            } else {
                Toast.makeText(this, "Smartpack Kernel Manager não instalado!", Toast.LENGTH_SHORT).show()
            }
        }

        // Se for Ultra, exibe os botões específicos
        if (mode == Mode.ULTRA) {
            findViewById<LinearLayout>(R.id.layout_ultra_settings).visibility = View.VISIBLE

            findViewById<MaterialCardView>(R.id.btn_choose_apps).setOnClickListener {
                startActivity(Intent(this, AppChooserActivity::class.java))
            }

            findViewById<MaterialCardView>(R.id.btn_choose_launcher).setOnClickListener {
                showLauncherPicker()
            }
        }
    }

    private fun showLauncherPicker() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        if (resolveInfos.isEmpty()) {
            Toast.makeText(this, "Nenhum launcher encontrado.", Toast.LENGTH_SHORT).show()
            return
        }

        val launcherItems = resolveInfos.map { info ->
            val label = info.loadLabel(pm).toString()
            val component = "${info.activityInfo.packageName}/${info.activityInfo.name}"
            Pair(label, component)
        }

        val labels = launcherItems.map { it.first }.toTypedArray()
        val currentComponent = UltraBatterySaver.getDefaultLauncherComponent(this)
        var selectedIndex = launcherItems.indexOfFirst { it.second == currentComponent }
        if (selectedIndex == -1) selectedIndex = 0

        AlertDialog.Builder(this)
            .setTitle("Escolher Launcher Padrão")
            .setSingleChoiceItems(labels, selectedIndex) { dialog, which ->
                val selectedComponent = launcherItems[which].second
                val parts = selectedComponent.split("/")
                if (parts.size == 2) {
                    UltraBatterySaver.setDefaultLauncher(this, parts[0], parts[1])
                    Toast.makeText(this, "Launcher padrão salvo.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_KERNEL_PROFILE && resultCode == Activity.RESULT_OK && data != null) {
            val name = TaskerLogic.extractProfileName(data) ?: "Desconhecido"
            val profileData = TaskerLogic.extractProfileData(data) ?: return
            val versionCode = TaskerLogic.extractVersionCode(data)

            TaskerLogic.saveProfile(this, mode, name, profileData, versionCode)
            Toast.makeText(this, "Perfil '$name' salvo com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }
}