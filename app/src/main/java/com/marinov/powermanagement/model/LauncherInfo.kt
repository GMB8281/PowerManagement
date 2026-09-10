package com.marinov.powermanagement.model

import android.graphics.drawable.Drawable

data class LauncherInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable
)