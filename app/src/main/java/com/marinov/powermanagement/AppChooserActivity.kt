package com.marinov.powermanagement

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telecom.TelecomManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppChooserActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var continueButton: Button
    private lateinit var searchBar: EditText

    private val allAppsList = mutableListOf<AppInfo>()
    private lateinit var adapter: AppListAdapter
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isUltraSetup = false

    companion object {
        private const val MAX_SELECTABLE = 12
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bloqueia a activity se o modo Ultra já estiver ativo
        val currentMode = TaskerLogic.getLastAppliedMode(this)
        if (currentMode == TaskerLogic.Mode.ULTRA) {
            Toast.makeText(this, R.string.ultra_active_block, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContentView(R.layout.activity_app_chooser)
        supportActionBar?.hide()

        isUltraSetup = intent.getBooleanExtra("ultra_setup", false)

        recyclerView = findViewById(R.id.apps_recycler_view)
        progressBar = findViewById(R.id.progress_bar)
        continueButton = findViewById(R.id.continue_button)
        searchBar = findViewById(R.id.search_bar)

        adapter = AppListAdapter(
            mutableListOf(),
            onSelectionChanged = { saveCurrentSelection() },
            maxSelectable = MAX_SELECTABLE,
            onMaxAttempt = {
                Toast.makeText(this, R.string.max_apps_toast, Toast.LENGTH_SHORT).show()
            },
            // BUG 2 FIX: contagem baseada na lista global, não na lista filtrada do adapter
            getGlobalSelectedCount = {
                allAppsList.count { it.isChecked && !it.isHidden }
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null

        continueButton.setOnClickListener {
            if (isUltraSetup) {
                UltraBatterySaver.setUltraSetupComplete(this, true)
                ModeLogic.applyModeWithLauncher(this, TaskerLogic.Mode.ULTRA)
                startActivity(Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
            finish()
        }

        setupSearch()
        setupBackPress()
        loadApps()
    }

    private fun setupSearch() {
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterApps(s?.toString() ?: "")
            }
        })
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!searchBar.text.isNullOrEmpty()) {
                    searchBar.setText("")
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun filterApps(query: String) {
        val lowerQuery = query.trim().lowercase()
        val filteredList = if (lowerQuery.isEmpty()) {
            allAppsList.toList()
        } else {
            allAppsList.filter {
                it.appName.lowercase().contains(lowerQuery) || it.packageName.lowercase().contains(lowerQuery)
            }
        }
        adapter.updateList(filteredList)
    }

    private fun loadApps() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        continueButton.isEnabled = false

        Thread {
            val pm = packageManager
            val packages = try {
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            } catch (_: Exception) {
                emptyList<ApplicationInfo>()
            }
            val currentAllowed = UltraBatterySaver.getAllowedApps(this).toMutableSet()
            val obligatoryPackages = getObligatoryPackageNames()
            val hiddenPackages = getHiddenPackageNames()

            val loaded = mutableListOf<AppInfo>()

            for (app in packages) {
                if (app.packageName == packageName) continue

                if (!app.enabled) continue
                if ((app.flags and ApplicationInfo.FLAG_SUSPENDED) != 0) continue

                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val canLaunch = pm.getLaunchIntentForPackage(app.packageName) != null

                if (isSystem && !canLaunch) continue

                val isObrig = obligatoryPackages.contains(app.packageName)
                val isHidden = hiddenPackages.contains(app.packageName)
                val info = AppInfo(
                    appName = pm.getApplicationLabel(app).toString(),
                    packageName = app.packageName,
                    icon = pm.getApplicationIcon(app),
                    isChecked = isObrig || currentAllowed.contains(app.packageName),
                    isObrigatorio = isObrig,
                    isHidden = isHidden
                )
                if (isObrig) currentAllowed.add(app.packageName)
                loaded.add(info)
            }

            loaded.sortWith(compareByDescending<AppInfo> { it.isChecked }.thenBy { it.appName.lowercase() })

            mainHandler.post {
                if (isFinishing || isDestroyed) return@post

                allAppsList.clear()
                allAppsList.addAll(loaded)
                filterApps(searchBar.text.toString())

                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                continueButton.isEnabled = true

                saveCurrentSelection()
            }
        }.start()
    }

    private fun saveCurrentSelection() {
        val selected = allAppsList.filter { it.isChecked }.map { it.packageName }.toSet()
        UltraBatterySaver.saveAllowedApps(this, selected)
    }

    // BUG 1 FIX: detecta dinamicamente apps com serviço VPN instalados no dispositivo
    private fun getVpnPackageNames(): Set<String> {
        return try {
            val intent = Intent("android.net.VpnService")
            packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA)
                .map { it.serviceInfo.packageName }
                .toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun getObligatoryPackageNames(): Set<String> {
        val set = mutableSetOf<String>()
        try {
            getSystemService(TelecomManager::class.java)?.defaultDialerPackage?.let { set.add(it) }
        } catch (_: Exception) {}
        try { Telephony.Sms.getDefaultSmsPackage(this)?.let { set.add(it) } } catch (_: Exception) {}
        try {
            val intent = Intent(Intent.ACTION_VIEW, "http://www.google.com".toUri())
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName?.let { set.add(it) }
        } catch (_: Exception) {}

        set.add("com.android.vending")
        set.add("com.google.android.gms")
        set.add("com.google.android.gsf")
        set.add("com.android.settings")
        set.add("com.smartpack.kernelmanager")
        // BUG 1 FIX: Magisk e VPNs são obrigatórios (seleção imutável)
        set.add("com.topjohnwu.magisk")
        set.addAll(getVpnPackageNames())
        return set
    }

    private fun getHiddenPackageNames(): Set<String> {
        // BUG 1 FIX: Magisk e VPNs também são ocultos (não contam nas 12 vagas)
        return setOf(
            "com.android.vending",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.smartpack.kernelmanager",
            "com.topjohnwu.magisk"
        ) + getVpnPackageNames()
    }
}