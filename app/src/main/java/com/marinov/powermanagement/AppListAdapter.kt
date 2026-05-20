package com.marinov.powermanagement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private var appList: List<AppInfo>,
    private val onSelectionChanged: () -> Unit,
    private val maxSelectable: Int,
    private val onMaxAttempt: () -> Unit,
    // BUG 2 FIX: contagem vem da lista global (allAppsList), não da lista filtrada visível
    private val getGlobalSelectedCount: () -> Int
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    fun updateList(newList: List<AppInfo>) {
        this.appList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = appList[position]
        holder.bind(app)

        holder.itemView.setOnClickListener {
            if (!app.isObrigatorio) {
                if (!app.isChecked) {
                    // Usa contagem global, não da lista filtrada
                    if (getGlobalSelectedCount() >= maxSelectable) {
                        onMaxAttempt()
                        return@setOnClickListener
                    }
                }
                app.isChecked = !app.isChecked
                holder.checkBox.isChecked = app.isChecked
                notifyDataSetChanged()
                onSelectionChanged()
            }
        }

        holder.checkBox.setOnClickListener {
            if (!app.isObrigatorio) {
                if (!app.isChecked) {
                    // Usa contagem global, não da lista filtrada
                    if (getGlobalSelectedCount() >= maxSelectable) {
                        onMaxAttempt()
                        holder.checkBox.isChecked = false
                        return@setOnClickListener
                    }
                }
                app.isChecked = holder.checkBox.isChecked
                notifyDataSetChanged()
                onSelectionChanged()
            } else {
                holder.checkBox.isChecked = true
            }
        }
    }

    override fun getItemCount() = appList.size

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.app_icon)
        val name: TextView = itemView.findViewById(R.id.app_name)
        val pack: TextView = itemView.findViewById(R.id.app_package)
        val checkBox: CheckBox = itemView.findViewById(R.id.app_checkbox)

        fun bind(appInfo: AppInfo) {
            icon.setImageDrawable(appInfo.icon)
            name.text = appInfo.appName
            pack.text = appInfo.packageName
            checkBox.isChecked = appInfo.isChecked

            val canSelect = !appInfo.isObrigatorio
            checkBox.isEnabled = canSelect

            if (!canSelect) {
                itemView.alpha = 0.6f
            } else {
                // Usa contagem global, não da lista filtrada
                val limitReached = getGlobalSelectedCount() >= maxSelectable
                val disableForLimit = !appInfo.isChecked && !appInfo.isHidden && limitReached
                checkBox.isEnabled = !disableForLimit
                itemView.alpha = if (disableForLimit) 0.4f else 1.0f
            }
        }
    }
}