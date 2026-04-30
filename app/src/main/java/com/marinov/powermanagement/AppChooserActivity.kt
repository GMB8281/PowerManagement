package com.marinov.powermanagement

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telecom.TelecomManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_chooser)

        supportActionBar?.hide()

        recyclerView = findViewById(R.id.apps_recycler_view)
        progressBar = findViewById(R.id.progress_bar)
        continueButton = findViewById(R.id.continue_button)
        searchBar = findViewById(R.id.search_bar)

        // Inicializamos o adapter com a lista vazia primeiro
        adapter = AppListAdapter(mutableListOf()) { saveCurrentSelection() }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null // Remove animações nativas para evitar piscadas na busca

        continueButton.setOnClickListener { finish() }

        setupSearch()
        setupBackPress()
        loadApps()
    }

    private fun setupSearch() {
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Filtra de forma fluída no thread principal
                filterApps(s?.toString() ?: "")
            }
        })
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!searchBar.text.isNullOrEmpty()) {
                    searchBar.setText("") // Reseta ao invés de clear para disparar o watcher
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

        // Atualiza a lista no adapter (certifique-se que o seu AppListAdapter
        // atualize a lista interna e chame notifyDataSetChanged ou use DiffUtil)
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
            } catch (e: Exception) {
                emptyList<ApplicationInfo>()
            }
            val currentAllowed = UltraBatterySaver.getAllowedApps(this).toMutableSet()
            val obligatoryPackages = getObligatoryPackageNames()

            val loaded = mutableListOf<AppInfo>()

            for (app in packages) {
                if (app.packageName == packageName) continue

                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val canLaunch = pm.getLaunchIntentForPackage(app.packageName) != null

                if (isSystem && !canLaunch) continue

                val isObrig = obligatoryPackages.contains(app.packageName)
                val info = AppInfo(
                    appName = pm.getApplicationLabel(app).toString(),
                    packageName = app.packageName,
                    icon = pm.getApplicationIcon(app),
                    isChecked = isObrig || currentAllowed.contains(app.packageName),
                    isObrigatorio = isObrig
                )
                if (isObrig) currentAllowed.add(app.packageName)
                loaded.add(info)
            }

            loaded.sortWith(compareByDescending<AppInfo> { it.isChecked }.thenBy { it.appName.lowercase() })

            mainHandler.post {
                if (isFinishing || isDestroyed) return@post

                allAppsList.clear()
                allAppsList.addAll(loaded)

                // Força o filtro atual
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

    private fun getObligatoryPackageNames(): Set<String> {
        val set = mutableSetOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                getSystemService(TelecomManager::class.java)?.defaultDialerPackage?.let { set.add(it) }
            } catch (_: Exception) {}
        }
        try { Telephony.Sms.getDefaultSmsPackage(this)?.let { set.add(it) } } catch (_: Exception) {}
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"))
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName?.let { set.add(it) }
        } catch (_: Exception) {}
        return set
    }
}