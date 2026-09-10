package com.marinov.powermanagement.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.marinov.powermanagement.R
import com.marinov.powermanagement.model.LauncherInfo

class LauncherAdapter(
    private var launchers: List<LauncherInfo>,
    private val onItemClick: (LauncherInfo) -> Unit
) : RecyclerView.Adapter<LauncherAdapter.ViewHolder>() {

    var selectedPackage: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

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
        private val checkIcon: ImageView = itemView.findViewById(R.id.iv_check)

        fun bind(launcher: LauncherInfo) {
            icon.setImageDrawable(launcher.icon)
            name.text = launcher.label
            packageNameText.text = launcher.packageName

            val isSelected = launcher.packageName == selectedPackage
            checkIcon.visibility = if (isSelected) View.VISIBLE else View.GONE
            itemView.isSelected = isSelected

            itemView.setOnClickListener { onItemClick(launcher) }
        }
    }
}