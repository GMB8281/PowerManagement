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

    companion object {
        private const val MAX_SELECTABLE = 8
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_chooser)

        supportActionBar?.hide()

        recyclerView = findViewById(R.id.apps_recycler_view)
        progressBar = findViewById(R.id.progress_bar)
        continueButton = findViewById(R.id.continue_button)
        searchBar = findViewById(R.id.search_bar)

        adapter = AppListAdapter(
            mutableListOf(),
            onSelectionChanged = { saveCurrentSelection() },
            maxSelectable = MAX_SELECTABLE,
            onMaxAttempt = {
                Toast.makeText(this, "Só podem ser selecionados no máximo 8 apps", Toast.LENGTH_SHORT).show()
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null

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
            } catch (e: Exception) {
                emptyList<ApplicationInfo>()
            }
            val currentAllowed = UltraBatterySaver.getAllowedApps(this).toMutableSet()
            val obligatoryPackages = getObligatoryPackageNames()
            val hiddenPackages = getHiddenPackageNames()

            val loaded = mutableListOf<AppInfo>()

            for (app in packages) {
                if (app.packageName == packageName) continue

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

        set.add("com.android.vending")
        set.add("com.google.android.gms")
        set.add("com.google.android.gsf")
        set.add("com.android.settings")
        return set
    }

    private fun getHiddenPackageNames(): Set<String> {
        return setOf(
            "com.android.vending",
            "com.google.android.gms",
            "com.google.android.gsf"
        )
    }
}