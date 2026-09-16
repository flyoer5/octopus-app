package io.github.flyoer5.octopusapp

/**
 * octopus 服务配置。viper 的 AutomaticEnv 在 Unmarshal 阶段不可靠，
 * 因此统一通过 --config 传入配置文件方式注入。
 */
data class OctopusConfig(
    val host: String = DEFAULT_HOST,
    val port: Int = DEFAULT_PORT,
    val databaseType: String = "sqlite",
    val databasePath: String,
    val logLevel: String = "info",
) {
    /** 生成 octopus 可读取的 JSON 配置内容。 */
    fun toJson(dbPath: String = databasePath): String =
        """{
  "server": {
    "host": "$host",
    "port": $port
  },
  "database": {
    "type": "$databaseType",
    "path": "$dbPath"
  },
  "log": {
    "level": "$logLevel"
  }
}"""

    companion object {
        const val DEFAULT_HOST = "0.0.0.0"
        const val DEFAULT_PORT = 18080

        fun default(dataDir: String): OctopusConfig =
            OctopusConfig(
                databasePath = if (dataDir.endsWith('/')) "${dataDir}data.db" else "$dataDir/data.db",
            )
    }
}
