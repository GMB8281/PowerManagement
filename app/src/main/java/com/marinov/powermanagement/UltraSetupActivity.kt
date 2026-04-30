package com.marinov.powermanagement

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class UltraSetupActivity : AppCompatActivity() {

    private var currentStep = 1

    private lateinit var tvTitle: TextView
    private lateinit var tvDesc: TextView
    private lateinit var btnAction: Button

    companion object {
        private const val REQ_KERNEL_PROFILE = 3001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ultra_setup)

        tvTitle = findViewById(R.id.tv_step_title)
        tvDesc = findViewById(R.id.tv_step_desc)
        btnAction = findViewById(R.id.btn_step_action)

        updateStepUI()
    }

    private fun updateStepUI() {
        when (currentStep) {
            1 -> {
                tvTitle.text = "Passo 1: Seus Apps"
                tvDesc.text = "Selecione até 8 aplicativos essenciais que você deseja usar durante a ultra-economia."
                btnAction.text = "Escolher Apps"
                btnAction.setOnClickListener {
                    startActivity(Intent(this, AppChooserActivity::class.java))
                    currentStep = 2
                }
            }
            2 -> {
                tvTitle.text = "Passo 2: Launcher"
                tvDesc.text = "Qual o seu launcher principal? Precisamos saber para restaurá-lo ao sair da ultra-economia."
                btnAction.text = "Escolher Launcher"
                btnAction.setOnClickListener { showLauncherPicker() }
            }
            3 -> {
                tvTitle.text = "Passo 3: Kernel"
                tvDesc.text = "Quase lá! Agora selecione o perfil de bateria extrema no Smartpack Kernel Manager."
                btnAction.text = "Escolher Perfil"
                btnAction.setOnClickListener {
                    if (TaskerLogic.isPluginAvailable(this)) {
                        TaskerLogic.requestProfilePicker(this, REQ_KERNEL_PROFILE)
                    } else {
                        Toast.makeText(this, "Smartpack não instalado!", Toast.LENGTH_SHORT).show()
                        finishSetup()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Atualiza a tela quando volta de outra activity
        updateStepUI()
    }

    private fun showLauncherPicker() {
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
                currentStep = 3
                updateStepUI()
            }
            .setCancelable(false)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_KERNEL_PROFILE && resultCode == Activity.RESULT_OK && data != null) {
            val name = TaskerLogic.extractProfileName(data) ?: "Desconhecido"
            val profileData = TaskerLogic.extractProfileData(data) ?: return
            val versionCode = TaskerLogic.extractVersionCode(data)

            TaskerLogic.saveProfile(this, TaskerLogic.Mode.ULTRA, name, profileData, versionCode)
            finishSetup()
        }
    }

    private fun finishSetup() {
        UltraBatterySaver.setUltraSetupComplete(this, true)
        Toast.makeText(this, "Configuração concluída!", Toast.LENGTH_SHORT).show()

        // Aplica o modo automaticamente e vai pra home
        ModeLogic.applyModeWithLauncher(this, TaskerLogic.Mode.ULTRA)
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }
}