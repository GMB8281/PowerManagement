package com.marinov.powermanagement.core

object RootCommands {

    fun run(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            process.waitFor()
            process.exitValue() == 0
        } catch (_: Exception) {
            false
        }
    }

    fun isRootAvailable(): Boolean = run("echo test")

    fun runBatch(commands: List<String>): Boolean {
        if (commands.isEmpty()) return true

        val script = commands.joinToString(" && ")

        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", script))
            process.waitFor()
            process.exitValue() == 0
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Executa comandos em lotes menores, separados por ";", para que uma falha
     * individual não impeça a execução dos demais.
     *
     * Retorna true se conseguiu iniciar os processos root, mesmo que algum
     * comando individual falhe.
     */
    fun runBatchBestEffort(commands: List<String>): Boolean {
        if (commands.isEmpty()) return true

        return try {
            commands.chunked(80).forEach { chunk ->
                val script = chunk.joinToString(" ; ")
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", script))
                process.waitFor()
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}