package com.marinov.powermanagement

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar

class LauncherSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LauncherAdapter
    private val launchersList = mutableListOf<LauncherInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher_selection)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false) // esconde o título da toolbar para não duplicar

        recyclerView = findViewById(R.id.rv_launchers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = LauncherAdapter(emptyList()) { launcher ->
            ModeLogic.setDefaultLauncher(this, launcher.packageName, launcher.activityName)
            Toast.makeText(this, "Launcher padrão definido: ${launcher.label}", Toast.LENGTH_SHORT).show()
            finish()
        }
        recyclerView.adapter = adapter

        loadLaunchers()
    }

    private fun loadLaunchers() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        val ourPackage = packageName
        val launchers = resolveInfos.mapNotNull { info ->
            val packageName = info.activityInfo.packageName
            val activityName = info.activityInfo.name
            val label = info.loadLabel(pm).toString()
            if (packageName == ourPackage || label.isBlank()) return@mapNotNull null
            val icon = info.loadIcon(pm)
            LauncherInfo(packageName, activityName, label, icon)
        }.sortedBy { it.label.lowercase() }

        launchersList.clear()
        launchersList.addAll(launchers)
        adapter.updateList(launchersList)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}