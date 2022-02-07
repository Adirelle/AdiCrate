package dev.adirelle.adicrate.bridge.waila

import mcp.mobius.waila.api.IRegistrar
import mcp.mobius.waila.api.IWailaPlugin

@Suppress("unused")
class WailaPlugin : IWailaPlugin {

    override fun register(registrar: IRegistrar) {
        CrateProvider.register(registrar)
    }
}
