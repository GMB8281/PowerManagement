package com.marinov.powermanagement

object RootCommands {
    fun run(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun isRootAvailable(): Boolean = run("echo test")
}