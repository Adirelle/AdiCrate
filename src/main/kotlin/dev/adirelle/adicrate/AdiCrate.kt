package dev.adirelle.adicrate

import dev.adirelle.adicrate.network.PullItemC2SPacket
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType.CLIENT
import net.fabricmc.api.Environment
import net.fabricmc.api.ModInitializer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Suppress("unused", "MemberVisibilityCanBePrivate")
object AdiCrate : ModInitializer, ClientModInitializer {

    const val MOD_ID = "adicrate"
    val LOGGER: Logger = LogManager.getLogger("AdiCrate")

    override fun onInitialize() {
        Crate.onInitialize()
        Controller.onInitialize()
        PullItemC2SPacket.registerReceiver()
        LOGGER.info("AdiCrafter initialized")
    }

    @Environment(CLIENT)
    override fun onInitializeClient() {
        Crate.onInitializeClient()
        Controller.onInitializeClient()
        LOGGER.info("AdiCrafter initialized on client")
    }
}
