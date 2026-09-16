package io.github.flyoer5.octopusapp

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * 负责管理 octopus 二进制生命周期。二进制以 liboctopus.so 形式打包在
 * jniLibs 中，系统安装时提取到 nativeLibraryDir（可执行目录），规避
 * Android 10+ 对 app 私有目录的 noexec 限制。
 */
object OctopusEngine {
    private const val TAG = "OctopusEngine"

    const val BINARY_NAME = "liboctopus.so"
    const val HEALTH_URL = "http://127.0.0.1:%d/"

    @Volatile
    private var process: Process? = null

    @Synchronized
    fun start(context: Context): Boolean {
        if (isAlive()) {
            return true
        }
        val binary = binaryPath(context)
        if (!binary.exists() || !binary.canExecute()) {
            Log.e(TAG, "octopus binary not found or not executable: ${binary.absolutePath}")
            return false
        }
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

    fun binaryPath(context: Context): File = File(context.applicationInfo.nativeLibraryDir, BINARY_NAME)

    fun workDir(context: Context): File = File(context.filesDir, "octopus").apply { mkdirs() }

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
