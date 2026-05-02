package com.marinov.powermanagement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LauncherAdapter(
    private var launchers: List<LauncherInfo>,
    private val onItemClick: (LauncherInfo) -> Unit
) : RecyclerView.Adapter<LauncherAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_launcher, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(launchers[position])
    }

    override fun getItemCount() = launchers.size

    fun updateList(newList: List<LauncherInfo>) {
        launchers = newList
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.iv_icon)
        private val name: TextView = itemView.findViewById(R.id.tv_name)
        private val packageNameText: TextView = itemView.findViewById(R.id.tv_package)

        fun bind(launcher: LauncherInfo) {
            icon.setImageDrawable(launcher.icon)
            name.text = launcher.label
            packageNameText.text = launcher.packageName
            itemView.setOnClickListener { onItemClick(launcher) }
        }
    }
}