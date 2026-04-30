package com.marinov.powermanagement

import android.graphics.drawable.Drawable

class AppInfo(
    var appName: String = "",
    var packageName: String = "",
    var icon: Drawable? = null,
    var isChecked: Boolean = false,
    var isObrigatorio: Boolean = false
)