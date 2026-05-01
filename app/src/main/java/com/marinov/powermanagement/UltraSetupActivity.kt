package com.marinov.powermanagement

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class UltraSetupActivity : AppCompatActivity() {

    private lateinit var btnChooseApps: Button
    private lateinit var btnConclude: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ultra_setup)

        btnChooseApps = findViewById(R.id.btn_choose_apps)
        btnConclude = findViewById(R.id.btn_conclude)

        btnChooseApps.setOnClickListener {
            startActivity(Intent(this, AppChooserActivity::class.java))
        }

        btnConclude.setOnClickListener {
            if (UltraBatterySaver.getAllowedApps(this).isEmpty()) {
                Toast.makeText(this, "Selecione ao menos um aplicativo.", Toast.LENGTH_SHORT).show()
            } else {
                UltraBatterySaver.setUltraSetupComplete(this, true)
                ModeLogic.applyModeWithLauncher(this, TaskerLogic.Mode.ULTRA)
                startActivity(Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
                finish()
            }
        }
    }
}