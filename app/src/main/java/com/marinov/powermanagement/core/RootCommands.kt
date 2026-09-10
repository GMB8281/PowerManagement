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

    /**
     * Executa comandos em lote.
     *
     * @param stopOnError Se true, usa "&&" e para no primeiro erro.
     *                    Se false, usa ";" e tenta executar todos os comandos.
     */
    fun runBatch(commands: List<String>, stopOnError: Boolean = true): Boolean {
        if (commands.isEmpty()) return true

        val separator = if (stopOnError) " && " else " ; "
        val script = commands.joinToString(separator)

        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", script))
            process.waitFor()
            process.exitValue() == 0
        } catch (_: Exception) {
            false
        }
    }
}