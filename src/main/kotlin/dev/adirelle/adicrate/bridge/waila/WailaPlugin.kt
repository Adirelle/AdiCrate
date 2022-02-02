package dev.adirelle.adicrate.bridge.waila

import dev.adirelle.adicrate.AdiCrate
import dev.adirelle.adicrate.block.entity.CrateBlockEntity
import mcp.mobius.waila.api.IRegistrar
import mcp.mobius.waila.api.IWailaPlugin
import mcp.mobius.waila.api.TooltipPosition

val showInteractionHints = AdiCrate.id("show_interaction_hints")

@Suppress("unused")
class WailaPlugin : IWailaPlugin {

    override fun register(registrar: IRegistrar) {
        registrar.addSyncedConfig(showInteractionHints, true)
        registrar.addComponent(CrateProvider, TooltipPosition.BODY, CrateBlockEntity::class.java)
        registrar.addComponent(CrateProvider, TooltipPosition.TAIL, CrateBlockEntity::class.java)
        registrar.addBlockData(CrateProvider, CrateBlockEntity::class.java)
    }
}
