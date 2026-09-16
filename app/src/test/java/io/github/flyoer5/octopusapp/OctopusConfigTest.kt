package io.github.flyoer5.octopusapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OctopusConfigTest {

    @Test
    fun `default port is 18080`() {
        val config = OctopusConfig.default(dataDir = "/tmp/octopus")
        assertEquals(18080, config.port)
    }

    @Test
    fun `default host binds all interfaces`() {
        val config = OctopusConfig.default(dataDir = "/tmp/octopus")
        assertEquals("0.0.0.0", config.host)
    }

    @Test
    fun `database path joins data dir`() {
        val config = OctopusConfig.default(dataDir = "/data/octopus")
        assertEquals("/data/octopus/data.db", config.databasePath)
    }

    @Test
    fun `database path handles trailing slash`() {
        val config = OctopusConfig.default(dataDir = "/data/octopus/")
        assertEquals("/data/octopus/data.db", config.databasePath)
    }

    @Test
    fun `toEnvMap contains expected octopus env vars`() {
        val config = OctopusConfig.default(dataDir = "/data/octopus")
        val env = config.toEnvMap()

        assertEquals("0.0.0.0", env[OctopusConfig.ENV_HOST])
        assertEquals("18080", env[OctopusConfig.ENV_PORT])
        assertEquals("sqlite", env[OctopusConfig.ENV_DATABASE_TYPE])
        assertEquals("/data/octopus/data.db", env[OctopusConfig.ENV_DATABASE_PATH])
        assertEquals("info", env[OctopusConfig.ENV_LOG_LEVEL])
    }

    @Test
    fun `custom port takes effect`() {
        val config = OctopusConfig(
            databasePath = "/tmp/data.db",
            port = 19090,
        )
        assertEquals("19090", config.toEnvMap()[OctopusConfig.ENV_PORT])
    }

    @Test
    fun `env keys use octopus prefix`() {
        assertTrue(OctopusConfig.ENV_PORT.startsWith("OCTOPUS_"))
    }
}
