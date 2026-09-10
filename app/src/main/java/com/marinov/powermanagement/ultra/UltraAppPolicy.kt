package com.marinov.powermanagement.ultra

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telecom.TelecomManager
import androidx.core.net.toUri
import com.marinov.powermanagement.model.AppInfo

object UltraAppPolicy {

    const val MAX_SELECTABLE = 12

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getDefaultDialerPackage(context: Context): String? {
        return try {
            context.getSystemService(TelecomManager::class.java)
                ?.defaultDialerPackage
        } catch (_: Exception) {
            null
        }
    }

    fun getDefaultSmsPackage(context: Context): String? {
        return try {
            Telephony.Sms.getDefaultSmsPackage(context)
        } catch (_: Exception) {
            null
        }
    }

    fun getDefaultPhoneAndSmsPackages(context: Context): Set<String> {
        val set = mutableSetOf<String>()

        getDefaultDialerPackage(context)
            ?.takeIf { isPackageInstalled(context, it) }
            ?.let { set.add(it) }

        getDefaultSmsPackage(context)
            ?.takeIf { isPackageInstalled(context, it) }
            ?.let { set.add(it) }

        return set
    }

    fun isDefaultPhoneOrSms(context: Context, packageName: String): Boolean {
        return packageName in getDefaultPhoneAndSmsPackages(context)
    }

    /**
     * Regra de elegibilidade:
     * - apps de usuário: podem entrar;
     * - apps de sistema atualizados: podem entrar;
     * - apps de sistema normais: só podem entrar se tiverem launcher.
     */
    fun isUserOrLaunchableSystemApp(app: ApplicationInfo, canLaunch: Boolean): Boolean {
        val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isUpdatedSystem = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

        if (!isSystem) return true

        return isUpdatedSystem || canLaunch
    }

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

    /**
     * Obrigatórios:
     * - discador padrão;
     * - SMS padrão;
     * - pacotes essenciais de fundo;
     * - VPNs;
     * - Magisk;
     * - Smartpack.
     */
    fun getObligatoryPackageNames(context: Context): Set<String> {
        val set = mutableSetOf<String>()

        // Discador e SMS padrão devem ficar disponíveis e protegidos.
        set.addAll(getDefaultPhoneAndSmsPackages(context))

        set.add("com.android.vending")
        set.add("com.google.android.gms")
        set.add("com.google.android.gsf")
        set.add("com.smartpack.kernelmanager")

        // BUG 1 FIX: Magisk e VPNs são obrigatórios (seleção imutável)
        set.add("com.topjohnwu.magisk")
        set.addAll(getVpnPackageNames(context))

        return set
    }

    /**
     * Ocultos:
     * - essenciais de fundo;
     * - VPNs;
     * - Magisk.
     *
     * Importante:
     * - discador padrão e SMS padrão NÃO podem ser ocultos,
     *   pois precisam aparecer na tela inicial do modo Ultra.
     */
    fun getHiddenPackageNames(context: Context): Set<String> {
        val hidden = mutableSetOf(
            "com.android.vending",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.smartpack.kernelmanager",
            "com.topjohnwu.magisk"
        )

        hidden.addAll(getVpnPackageNames(context))

        // Garante que discador/SMS padrão não sejam ocultados.
        hidden.removeAll(getDefaultPhoneAndSmsPackages(context))

        return hidden
    }

    // BUG 2 FIX: contagem baseada na lista global, não na lista filtrada do adapter
    fun countSelectableSelected(apps: List<AppInfo>): Int {
        return apps.count { it.isChecked && !it.isHidden }
    }
}