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
    fun `custom port takes effect`() {
        val config =
            OctopusConfig(
                databasePath = "/tmp/data.db",
                port = 19090,
            )
        assertEquals(19090, config.port)
    }

    @Test
    fun `toJson contains server port 18080`() {
        val config = OctopusConfig.default(dataDir = "/data/octopus")
        assertTrue(config.toJson().contains("\"port\": 18080"))
    }

    @Test
    fun `toJson contains absolute database path`() {
        val config = OctopusConfig.default(dataDir = "/data/octopus")
        assertTrue(config.toJson().contains("\"path\": \"/data/octopus/data.db\""))
    }

    @Test
    fun `toJson contains all config sections`() {
        val json = OctopusConfig.default(dataDir = "/data/octopus").toJson()
        assertTrue(json.contains("\"server\""))
        assertTrue(json.contains("\"database\""))
        assertTrue(json.contains("\"log\""))
        assertTrue(json.contains("\"type\": \"sqlite\""))
    }
}
