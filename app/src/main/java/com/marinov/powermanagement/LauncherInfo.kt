package com.marinov.powermanagement

import android.graphics.drawable.Drawable

data class LauncherInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable
)