package com.marinov.powermanagement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// OTIMIZADO PARA A BUSCA FLUÍDA
class AppListAdapter(
    private var appList: List<AppInfo>,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    fun updateList(newList: List<AppInfo>) {
        this.appList = newList
        notifyDataSetChanged() // DiffUtil seria melhor, mas notifyDataSetChanged com animação desativada é 100% fluído para buscas rápidas.
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
                app.isChecked = !app.isChecked
                holder.checkBox.isChecked = app.isChecked
                onSelectionChanged()
            }
        }

        holder.checkBox.setOnClickListener {
            if (!app.isObrigatorio) {
                app.isChecked = holder.checkBox.isChecked
                onSelectionChanged()
            } else {
                holder.checkBox.isChecked = true // Trava forçadamente
            }
        }
    }

    override fun getItemCount() = appList.size

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.app_icon)
        val name: TextView = itemView.findViewById(R.id.app_name)
        val pack: TextView = itemView.findViewById(R.id.app_package)
        val checkBox: CheckBox = itemView.findViewById(R.id.app_checkbox)

        fun bind(appInfo: AppInfo) {
            icon.setImageDrawable(appInfo.icon)
            name.text = appInfo.appName
            pack.text = appInfo.packageName
            checkBox.isChecked = appInfo.isChecked
            checkBox.isEnabled = !appInfo.isObrigatorio
            itemView.alpha = if (appInfo.isObrigatorio) 0.6f else 1.0f
        }
    }
}