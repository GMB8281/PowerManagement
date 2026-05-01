package com.marinov.powermanagement

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
}