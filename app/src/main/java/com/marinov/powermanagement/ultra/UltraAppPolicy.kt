package com.marinov.powermanagement.ultra

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telecom.TelecomManager
import androidx.core.net.toUri
import com.marinov.powermanagement.model.AppInfo

object UltraAppPolicy {

    const val MAX_SELECTABLE = 12

    // BUG 1 FIX: detecta dinamicamente apps com serviço VPN instalados no dispositivo
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

        // BUG 1 FIX: Magisk e VPNs são obrigatórios (seleção imutável)
        set.add("com.topjohnwu.magisk")
        set.addAll(getVpnPackageNames(context))

        return set
    }

    fun getHiddenPackageNames(context: Context): Set<String> {
        // BUG 1 FIX: Magisk e VPNs também são ocultos (não contam nas 12 vagas)
        return setOf(
            "com.android.vending",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.smartpack.kernelmanager",
            "com.topjohnwu.magisk"
        ) + getVpnPackageNames(context)
    }

    // BUG 2 FIX: contagem baseada na lista global, não na lista filtrada do adapter
    fun countSelectableSelected(apps: List<AppInfo>): Int {
        return apps.count { it.isChecked && !it.isHidden }
    }
}