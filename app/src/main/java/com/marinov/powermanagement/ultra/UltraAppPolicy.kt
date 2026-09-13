package com.marinov.powermanagement.ultra

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telecom.TelecomManager
import androidx.core.net.toUri
import com.marinov.powermanagement.model.AppInfo

object UltraAppPolicy {

    const val MAX_SELECTABLE = 24

    fun getVpnPackageNames(context: Context): Set<String> {
        return try {
            val intent = Intent("android.net.VpnService")
            context.packageManager.queryIntentServices(
                intent,
                PackageManager.GET_META_DATA
            )
                .map { it.serviceInfo.packageName }
                .toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun getObligatoryPackageNames(context: Context): Set<String> {
        val set = mutableSetOf<String>()

        try {
            context.getSystemService(TelecomManager::class.java)
                ?.defaultDialerPackage
                ?.let { set.add(it) }
        } catch (_: Exception) {
        }

        try {
            Telephony.Sms.getDefaultSmsPackage(context)?.let { set.add(it) }
        } catch (_: Exception) {
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, "http://www.google.com".toUri())
            context.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
                ?.activityInfo
                ?.packageName
                ?.let { set.add(it) }
        } catch (_: Exception) {
        }

        set.add("com.android.vending")
        set.add("com.google.android.gms")
        set.add("com.google.android.gsf")
        set.add("com.android.settings")
        set.add("com.smartpack.kernelmanager")
        set.add("com.google.android.apps.wellbeing")
        set.add("com.arlosoft.macrodroid")
        set.add("com.google.android.apps.kids.familylink")
        set.add("com.llamalab.automate")
        set.add("com.marinov.clearcache")
        set.add("com.topjohnwu.magisk")
        set.add("org.fdroid")
        set.add("com.looker.droidify")
        set.add("org.breezyweather")
        set.add("com.kms.free")
        set.add("com.bitdefender.antivirus")
        set.add("com.eset.ems2.gp")
        set.add("com.eset.etvs.gp")
        set.add("com.bitdefender.security")
        set.add("com.marinov.mirrorscreensettings")
        set.add("com.rosan.installer.x.revived")
        set.addAll(getVpnPackageNames(context))

        return set
    }

    fun getHiddenPackageNames(context: Context): Set<String> {
        return setOf(
            "com.google.android.gms",
            "org.fdroid",
            "org.adaway",
            "com.google.android.gsf",
            "com.smartpack.kernelmanager",
            "com.google.android.apps.wellbeing",
            "com.arlosoft.macrodroid",
            "com.google.android.apps.kids.familylink",
            "com.marinov.clearcache",
            "com.llamalab.automate",
            "com.android.vending",
            "com.looker.droidify",
            "com.kms.free",
            "com.bitdefender.security",
            "com.bitdefender.antivirus",
            "com.eset.ems2.gp",
            "com.eset.etvs.gp",
            "com.marinov.mirrorscreensettings",
            "com.rosan.installer.x.revived",
            "com.topjohnwu.magisk"
        ) + getVpnPackageNames(context)
    }

    fun countSelectableSelected(apps: List<AppInfo>): Int {
        return apps.count { it.isChecked && !it.isHidden }
    }
}