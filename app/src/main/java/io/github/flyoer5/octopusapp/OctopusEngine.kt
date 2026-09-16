package io.github.flyoer5.octopusapp

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * 负责把 octopus 二进制从 assets 释放到私有目录并管理其生命周期。
 */
object OctopusEngine {
    private const val TAG = "OctopusEngine"

    const val ASSET_BINARY = "octopus-android-arm64"
    const val HEALTH_URL = "http://127.0.0.1:%d/"

    @Volatile
    private var process: Process? = null

    @Synchronized
    fun start(context: Context): Boolean {
        if (isAlive()) {
            return true
        }
        val binary = extractBinary(context) ?: return false
        val workDir = workDir(context)
        val config = OctopusConfig.default(File(workDir, "data.db").absolutePath)

        val pb = ProcessBuilder(binary.absolutePath, "start")
        pb.directory(workDir)
        pb.environment().putAll(config.toEnvMap())
        pb.redirectErrorStream(true)

        return try {
            val p = pb.start()
            process = p
            pumpOutput(p)
            true
        } catch (e: IOException) {
            Log.e(TAG, "failed to start octopus", e)
            false
        }
    }

    @Synchronized
    fun stop() {
        process?.let { p ->
            runCatching { p.destroy() }
            process = null
        }
    }

    @Synchronized
    fun isAlive(): Boolean {
        val p = process ?: return false
        return try {
            p.exitValue()
            false
        } catch (e: IllegalThreadStateException) {
            true
        }
    }

    fun isPortOpen(
        port: Int,
        timeoutMs: Int = 1200,
    ): Boolean =
        runCatching {
            val conn = URL(HEALTH_URL.format(port)).openConnection() as HttpURLConnection
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.requestMethod = "GET"
            val code = conn.responseCode
            conn.disconnect()
            code in 200..499
        }.getOrDefault(false)

    fun workDir(context: Context): File = File(context.filesDir, "octopus").apply { mkdirs() }

    private fun extractBinary(context: Context): File? {
        val dir = File(workDir(context), "bin").apply { mkdirs() }
        val target = File(dir, "octopus")
        if (target.exists() && target.canExecute()) {
            return target
        }
        return try {
            context.assets.open(ASSET_BINARY).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            target.setExecutable(true, false)
            target.setReadable(true, false)
            target
        } catch (e: IOException) {
            Log.e(TAG, "failed to extract octopus binary", e)
            null
        }
    }

    private fun pumpOutput(p: Process) {
        Thread {
            try {
                p.inputStream.bufferedReader().forEachLine { line ->
                    Log.i(TAG, line)
                }
            } catch (e: IOException) {
                // 进程结束时输入流关闭属正常现象
            }
        }.start()
    }
}
