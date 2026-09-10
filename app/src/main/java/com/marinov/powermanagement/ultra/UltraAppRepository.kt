package com.marinov.powermanagement.ultra

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.marinov.powermanagement.model.AppInfo

object UltraAppRepository {

    fun loadAppList(context: Context): List<AppInfo> {
        val pm = context.packageManager

        val packages = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (_: Exception) {
            emptyList<ApplicationInfo>()
        }

        val currentAllowed = UltraBatterySaver.getAllowedApps(context).toMutableSet()
        val obligatoryPackages = UltraAppPolicy.getObligatoryPackageNames(context)
        val hiddenPackages = UltraAppPolicy.getHiddenPackageNames(context)
        val defaultPhoneAndSmsPackages = UltraAppPolicy.getDefaultPhoneAndSmsPackages(context)

        val loaded = mutableListOf<AppInfo>()

        for (app in packages) {
            if (app.packageName == context.packageName) continue

            val canLaunch = pm.getLaunchIntentForPackage(app.packageName) != null
            val isDefaultPhoneOrSms = defaultPhoneAndSmsPackages.contains(app.packageName)
            val isHidden = hiddenPackages.contains(app.packageName)

            // Se estiver desabilitado, normalmente não entra.
            // Mas discador/SMS padrão entram mesmo assim, para garantir visibilidade.
            if (!app.enabled && !isDefaultPhoneOrSms) continue

            // Regras normais para apps que não são discador/SMS padrão.
            if (!isDefaultPhoneOrSms) {
                // Ocultos não aparecem na lista.
                if (isHidden) continue

                // Somente:
                // - apps de usuário;
                // - sistema atualizado;
                // - sistema com launcher.
                if (!UltraAppPolicy.isUserOrLaunchableSystemApp(app, canLaunch)) continue
            }

            val isObrig = obligatoryPackages.contains(app.packageName)

            val info = AppInfo(
                appName = pm.getApplicationLabel(app).toString(),
                packageName = app.packageName,
                icon = pm.getApplicationIcon(app),
                isChecked = isObrig || currentAllowed.contains(app.packageName),
                isObrigatorio = isObrig,
                isHidden = isHidden
            )

            if (isObrig) {
                currentAllowed.add(app.packageName)
            }

            loaded.add(info)
        }

        loaded.sortWith(
            compareByDescending<AppInfo> { it.isChecked }
                .thenBy { it.appName.lowercase() }
        )

        return loaded
    }

    fun filterApps(apps: List<AppInfo>, query: String): List<AppInfo> {
        val lowerQuery = query.trim().lowercase()

        return if (lowerQuery.isEmpty()) {
            apps.toList()
        } else {
            apps.filter {
                it.appName.lowercase().contains(lowerQuery) ||
                        it.packageName.lowercase().contains(lowerQuery)
            }
        }
    }

    fun saveSelection(context: Context, apps: List<AppInfo>) {
        val selected = apps.filter { it.isChecked }
            .map { it.packageName }
            .toSet()

        // Garante que os obrigatórios sempre permaneçam allowlistados,
        // mesmo que não estejam visíveis na lista.
        val obligatory = UltraAppPolicy.getObligatoryPackageNames(context)

        UltraBatterySaver.saveAllowedApps(context, selected + obligatory)
    }
}