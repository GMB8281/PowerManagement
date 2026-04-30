package com.marinov.powermanagement

object LauncherManager {
    @Volatile
    var currentActivity: LauncherActivity? = null

    fun finishIfActive() {
        currentActivity?.run {
            finish()
        }
    }
}