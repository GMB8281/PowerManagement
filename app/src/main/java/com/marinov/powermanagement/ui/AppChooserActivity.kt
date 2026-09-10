package com.marinov.powermanagement.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.marinov.powermanagement.R
import com.marinov.powermanagement.core.ModeLogic
import com.marinov.powermanagement.core.TaskerLogic
import com.marinov.powermanagement.model.AppInfo
import com.marinov.powermanagement.ultra.UltraAppPolicy
import com.marinov.powermanagement.ultra.UltraAppRepository
import com.marinov.powermanagement.ultra.UltraBatterySaver

class AppChooserActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var continueButton: Button
    private lateinit var searchBar: EditText

    private val allAppsList = mutableListOf<AppInfo>()
    private lateinit var adapter: AppListAdapter
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isUltraSetup = false

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
            maxSelectable = UltraAppPolicy.MAX_SELECTABLE,
            onMaxAttempt = {
                Toast.makeText(this, R.string.max_apps_toast, Toast.LENGTH_SHORT).show()
            },
            // BUG 2 FIX: contagem baseada na lista global, não na lista filtrada do adapter
            getGlobalSelectedCount = {
                UltraAppPolicy.countSelectableSelected(allAppsList)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null

        continueButton.setOnClickListener {
            if (isUltraSetup) {
                UltraBatterySaver.setUltraSetupComplete(this, true)
                ModeLogic.applyModeWithLauncher(this, TaskerLogic.Mode.ULTRA)

                startActivity(
                    Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
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
        adapter.updateList(
            UltraAppRepository.filterApps(allAppsList, query)
        )
    }

    private fun loadApps() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        continueButton.isEnabled = false

        Thread {
            val loaded = UltraAppRepository.loadAppList(this@AppChooserActivity)

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
        UltraAppRepository.saveSelection(this, allAppsList)
    }
}