package com.marinov.powermanagement.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.marinov.powermanagement.model.LauncherInfo

object LauncherRepository {

    fun getHomeLaunchers(context: Context): List<LauncherInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

        val resolveInfos = pm.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )

        val ourPackage = context.packageName

        return resolveInfos.mapNotNull { info ->
            val packageName = info.activityInfo.packageName
            val activityName = info.activityInfo.name
            val label = info.loadLabel(pm).toString()

            if (packageName == ourPackage || label.isBlank()) {
                return@mapNotNull null
            }

            val icon = info.loadIcon(pm)

            LauncherInfo(
                packageName = packageName,
                activityName = activityName,
                label = label,
                icon = icon
            )
        }.sortedBy { it.label.lowercase() }
    }

    fun getCurrentDefaultPackage(context: Context): String? {
        return ModeLogic.getDefaultLauncherComponent(context)?.substringBefore("/")
    }
}