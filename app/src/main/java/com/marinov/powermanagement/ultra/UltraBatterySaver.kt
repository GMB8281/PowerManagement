package com.marinov.powermanagement.ultra

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.edit
import com.marinov.powermanagement.R
import com.marinov.powermanagement.core.RootCommands
import com.marinov.powermanagement.core.TaskerLogic
import java.util.concurrent.Executors

object UltraBatterySaver {

    private const val PREFS_NAME = "ultra_battery_prefs"
    private const val KEY_ALLOWED_APPS = "allowed_apps"
    private const val KEY_SETUP_COMPLETE = "ultra_setup_complete"
    private const val KEY_SUSPENDED_PACKAGES = "suspended_packages"

    // Compatibilidade com a versão anterior que usava disable/enable.
    private const val LEGACY_KEY_DISABLED_PACKAGES = "disabled_packages"

    // Tamanho do lote de comandos root.
    private const val ROOT_BATCH_SIZE = 256

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Single-thread executor.
     *
     * Isso garante que entrada/saída do modo Ultra não se atropelam.
     */
    private val executor = Executors.newSingleThreadExecutor()

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isUltraSetupComplete(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_SETUP_COMPLETE, false)

    fun setUltraSetupComplete(context: Context, complete: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_SETUP_COMPLETE, complete) }
    }

    /**
     * Salva a lista de apps permitidos, apenas se o modo Ultra NÃO estiver ativo.
     */
    fun saveAllowedApps(context: Context, apps: Set<String>): Boolean {
        val currentMode = TaskerLogic.getLastAppliedMode(context)

        if (currentMode == TaskerLogic.Mode.ULTRA) {
            mainHandler.post {
                Toast.makeText(
                    context,
                    R.string.cant_change_allowed_ultra_active,
                    Toast.LENGTH_LONG
                ).show()
            }
            return false
        }

        getPrefs(context).edit { putStringSet(KEY_ALLOWED_APPS, apps) }
        return true
    }

    /**
     * Retorna os apps permitidos com prioridade para:
     * 1. discador padrão;
     * 2. SMS padrão;
     * 3. demais apps salvos.
     *
     * Isso ajuda o launcher do modo Ultra a mostrar telefone/SMS primeiro.
     */
    fun getAllowedApps(context: Context): Set<String> {
        val saved = getPrefs(context).getStringSet(KEY_ALLOWED_APPS, emptySet()) ?: emptySet()

        val ordered = LinkedHashSet<String>()

        UltraAppPolicy.getDefaultDialerPackage(context)?.let { ordered.add(it) }
        UltraAppPolicy.getDefaultSmsPackage(context)?.let { ordered.add(it) }

        ordered.addAll(saved)

        return ordered
    }

    private fun saveSuspendedPackages(context: Context, packages: Set<String>) {
        getPrefs(context).edit { putStringSet(KEY_SUSPENDED_PACKAGES, packages) }
    }

    private fun getSuspendedPackages(context: Context): Set<String> =
        getPrefs(context).getStringSet(KEY_SUSPENDED_PACKAGES, emptySet()) ?: emptySet()

    private fun getLegacyDisabledPackages(context: Context): Set<String> =
        getPrefs(context).getStringSet(LEGACY_KEY_DISABLED_PACKAGES, emptySet()) ?: emptySet()

    private fun clearLegacyDisabledPackages(context: Context) {
        getPrefs(context).edit { remove(LEGACY_KEY_DISABLED_PACKAGES) }
    }

    /**
     * Migração defensiva:
     * se ainda houver pacotes marcados como disabled pela versão anterior,
     * reativa eles e limpa essa chave antiga.
     */
    private fun migrateLegacyDisabledIfNeeded(context: Context) {
        val legacyDisabled = getLegacyDisabledPackages(context)

        if (legacyDisabled.isEmpty()) return

        for (chunk in legacyDisabled.chunked(ROOT_BATCH_SIZE)) {
            val commands = chunk.map { pkg ->
                "pm enable $pkg"
            }

            RootCommands.runBatch(commands, stopOnError = false)
        }

        clearLegacyDisabledPackages(context)
    }

    /**
     * Retorna somente apps que podem entrar no fluxo de suspensão:
     * - apps de usuário;
     * - apps de sistema atualizados;
     * - apps de sistema que possuem launcher.
     *
     * Observação:
     * - discador/SMS padrão até podem aparecer aqui, mas depois são protegidos
     *   porque entram como obrigatórios.
     */
    private fun getSuspendCandidatePackages(context: Context): Set<String> {
        val pm = context.packageManager

        val packages = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (_: Exception) {
            emptyList<ApplicationInfo>()
        }

        val hiddenPackages = UltraAppPolicy.getHiddenPackageNames(context)
        val defaultPhoneAndSmsPackages = UltraAppPolicy.getDefaultPhoneAndSmsPackages(context)

        val result = mutableSetOf<String>()

        for (app in packages) {
            if (app.packageName == context.packageName) continue
            if (!app.enabled) continue

            // Se já está suspenso manualmente, não mexe.
            if ((app.flags and ApplicationInfo.FLAG_SUSPENDED) != 0) continue

            val canLaunch = pm.getLaunchIntentForPackage(app.packageName) != null
            val isDefaultPhoneOrSms = defaultPhoneAndSmsPackages.contains(app.packageName)
            val isHidden = hiddenPackages.contains(app.packageName)

            if (isHidden && !isDefaultPhoneOrSms) continue

            if (!isDefaultPhoneOrSms && !UltraAppPolicy.isUserOrLaunchableSystemApp(app, canLaunch)) {
                continue
            }

            result.add(app.packageName)
        }

        return result
    }

    /**
     * Suspende os apps não permitidos e depois força parada.
     *
     * Fluxo:
     * 1. trabalha em lotes de 40 comandos;
     * 2. salva o estado antes de executar cada lote;
     * 3. roda pm suspend;
     * 4. depois roda am force-stop.
     */
    fun suspendNonAllowedApps(context: Context, onComplete: (() -> Unit)? = null) {
        executor.execute {
            try {
                // Se o modo já tiver saído do Ultra antes desta tarefa começar,
                // não faz sentido suspender nada.
                if (TaskerLogic.getLastAppliedMode(context) != TaskerLogic.Mode.ULTRA) {
                    return@execute
                }

                if (!RootCommands.isRootAvailable()) {
                    return@execute
                }

                // Migração da fase disable/enable, se existir.
                migrateLegacyDisabledIfNeeded(context)

                // Checagem novamente após migração.
                if (TaskerLogic.getLastAppliedMode(context) != TaskerLogic.Mode.ULTRA) {
                    return@execute
                }

                val allowed = getAllowedApps(context)
                val obligatory = UltraAppPolicy.getObligatoryPackageNames(context)

                val existingSuspended = getSuspendedPackages(context).toMutableSet()

                // Se algum app atualmente suspenso agora está permitido,
                // garante que ele volte antes de continuar.
                val mustUnsuspend = existingSuspended.filter { pkg ->
                    pkg in allowed || pkg in obligatory
                }

                if (mustUnsuspend.isNotEmpty()) {
                    for (chunk in mustUnsuspend.chunked(ROOT_BATCH_SIZE)) {
                        if (TaskerLogic.getLastAppliedMode(context) != TaskerLogic.Mode.ULTRA) {
                            return@execute
                        }

                        val commands = chunk.map { pkg ->
                            "pm unsuspend $pkg"
                        }

                        RootCommands.runBatch(commands, stopOnError = false)

                        existingSuspended.removeAll(chunk.toSet())
                        saveSuspendedPackages(context, existingSuspended)
                    }
                }

                val candidates = getSuspendCandidatePackages(context)

                val toSuspend = candidates.filter { pkg ->
                    pkg !in allowed && pkg !in obligatory
                }.distinct()

                if (toSuspend.isEmpty()) {
                    saveSuspendedPackages(context, existingSuspended)
                    return@execute
                }

                for (chunk in toSuspend.chunked(ROOT_BATCH_SIZE)) {
                    // Se o usuário saiu do Ultra no meio do processo,
                    // para de agredir o sistema e deixa a restauração assumir depois.
                    if (TaskerLogic.getLastAppliedMode(context) != TaskerLogic.Mode.ULTRA) {
                        break
                    }

                    val chunkSet = chunk.toSet()

                    // Salva antes de executar o lote.
                    // Assim, mesmo que algo interrompa, o estado fica recuperável.
                    existingSuspended.addAll(chunkSet)
                    saveSuspendedPackages(context, existingSuspended)

                    // 1) pm suspend em lote de 40
                    val suspendCommands = chunk.map { pkg ->
                        "pm suspend $pkg"
                    }
                    RootCommands.runBatch(suspendCommands, stopOnError = false)

                    // Se o usuário saiu durante o lote de suspensão,
                    // não precisa continuar com force-stop deste lote.
                    if (TaskerLogic.getLastAppliedMode(context) != TaskerLogic.Mode.ULTRA) {
                        break
                    }

                    // 2) am force-stop no mesmo lote de 40
                    val stopCommands = chunk.map { pkg ->
                        "am force-stop $pkg"
                    }
                    RootCommands.runBatch(stopCommands, stopOnError = false)
                }
            } catch (_: Exception) {
            } finally {
                onComplete?.let { mainHandler.post(it) }
            }
        }
    }

    /**
     * Restaura todos os apps suspensos, também em lotes de 40.
     */
    fun unsuspendAllSuspendedApps(context: Context, onComplete: (() -> Unit)? = null) {
        executor.execute {
            try {
                // Se o usuário já voltou para o Ultra, não deve restaurar agora.
                if (TaskerLogic.getLastAppliedMode(context) == TaskerLogic.Mode.ULTRA) {
                    return@execute
                }

                if (!RootCommands.isRootAvailable()) {
                    return@execute
                }

                // Migração defensiva, caso ainda exista resquício de disable/enable.
                migrateLegacyDisabledIfNeeded(context)

                // Checagem novamente após migração.
                if (TaskerLogic.getLastAppliedMode(context) == TaskerLogic.Mode.ULTRA) {
                    return@execute
                }

                val remaining = getSuspendedPackages(context).toMutableSet()

                if (remaining.isEmpty()) {
                    return@execute
                }

                for (chunk in remaining.toList().chunked(ROOT_BATCH_SIZE)) {
                    // Se o usuário voltou para o Ultra no meio da restauração,
                    // interrompe e deixa o estado restante para o fluxo do Ultra.
                    if (TaskerLogic.getLastAppliedMode(context) == TaskerLogic.Mode.ULTRA) {
                        break
                    }

                    val chunkSet = chunk.toSet()

                    val commands = chunk.map { pkg ->
                        "pm unsuspend $pkg"
                    }

                    RootCommands.runBatch(commands, stopOnError = false)

                    remaining.removeAll(chunkSet)
                    saveSuspendedPackages(context, remaining)
                }
            } catch (_: Exception) {
            } finally {
                onComplete?.let { mainHandler.post(it) }
            }
        }
    }
}