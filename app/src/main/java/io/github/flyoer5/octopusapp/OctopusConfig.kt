package io.github.flyoer5.octopusapp

/**
 * octopus 服务配置。启动时通过环境变量注入（octopus 使用 viper 自动读取
 * OCTOPUS_* 环境变量覆盖默认配置）。
 */
data class OctopusConfig(
    val host: String = DEFAULT_HOST,
    val port: Int = DEFAULT_PORT,
    val databaseType: String = "sqlite",
    val databasePath: String,
    val logLevel: String = "info",
) {
    fun toEnvMap(): Map<String, String> = mapOf(
        ENV_HOST to host,
        ENV_PORT to port.toString(),
        ENV_DATABASE_TYPE to databaseType,
        ENV_DATABASE_PATH to databasePath,
        ENV_LOG_LEVEL to logLevel,
    )

    companion object {
        const val DEFAULT_HOST = "0.0.0.0"
        const val DEFAULT_PORT = 18080

        const val ENV_HOST = "OCTOPUS_SERVER_HOST"
        const val ENV_PORT = "OCTOPUS_SERVER_PORT"
        const val ENV_DATABASE_TYPE = "OCTOPUS_DATABASE_TYPE"
        const val ENV_DATABASE_PATH = "OCTOPUS_DATABASE_PATH"
        const val ENV_LOG_LEVEL = "OCTOPUS_LOG_LEVEL"

        fun default(dataDir: String): OctopusConfig =
            OctopusConfig(
                databasePath = if (dataDir.endsWith('/')) "${dataDir}data.db" else "$dataDir/data.db",
            )
    }
}
