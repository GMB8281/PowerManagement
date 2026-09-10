package com.marinov.powermanagement.core

import android.app.Activity

object LauncherManager {

    @Volatile
    var currentActivity: Activity? = null

    fun finishIfActive() {
        currentActivity?.finish()
    }
}