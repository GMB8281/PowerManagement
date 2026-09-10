package com.marinov.powermanagement.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.marinov.powermanagement.R
import com.marinov.powermanagement.core.LauncherRepository
import com.marinov.powermanagement.core.ModeLogic
import com.marinov.powermanagement.model.LauncherInfo
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
        supportActionBar?.setDisplayShowTitleEnabled(false)

        recyclerView = findViewById(R.id.rv_launchers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val currentPkg = LauncherRepository.getCurrentDefaultPackage(this)

        adapter = LauncherAdapter(emptyList()) { launcher ->
            ModeLogic.setDefaultLauncher(this, launcher.packageName, launcher.activityName)
            Toast.makeText(
                this,
                getString(R.string.launcher_set_toast, launcher.label),
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }

        recyclerView.adapter = adapter
        loadLaunchers(currentPkg)
    }

    private fun loadLaunchers(currentPkg: String?) {
        val launchers = LauncherRepository.getHomeLaunchers(this)

        launchersList.clear()
        launchersList.addAll(launchers)

        adapter.updateList(launchersList)
        adapter.selectedPackage = currentPkg
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}