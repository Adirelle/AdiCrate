@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.bridge.waila

import dev.adirelle.adicrate.AdiCrate
import dev.adirelle.adicrate.block.entity.CrateBlockEntity
import dev.adirelle.adicrate.block.entity.internal.CrateStorage
import dev.adirelle.adicrate.utils.extensions.set
import mcp.mobius.waila.api.*
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.TranslatableText
import net.minecraft.world.World

object CrateProvider : IBlockComponentProvider, IServerDataProvider<CrateBlockEntity> {

    private const val DATA_KEY = "${AdiCrate.MOD_ID}:crate"

    override fun appendServerData(
        data: NbtCompound,
        player: ServerPlayerEntity,
        world: World,
        blockEntity: CrateBlockEntity
    ) {
        data[DATA_KEY] = { nbt ->
            with(blockEntity.storage) {
                nbt["resource"] = resource
                nbt["amount"] = amount
                nbt["capacity"] = realCapacity
                nbt["jammed"] = isJammed()
            }
        }
    }

    override fun appendBody(tooltip: ITooltip, accessor: IBlockAccessor, config: IPluginConfig) {
        val nbt = accessor.serverData.getCompound(DATA_KEY)
        tooltip.add(CrateStorage.contentText(nbt.getLong("amount"), nbt.getLong("capacity")))
        if (nbt.getBoolean("jammed")) {
            tooltip.add(TranslatableText("tooltip.adicrate.crate.jammed"))
        }
    }
}
