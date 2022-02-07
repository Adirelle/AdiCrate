@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.bridge.waila

import dev.adirelle.adicrate.AdiCrate
import dev.adirelle.adicrate.block.entity.CrateBlockEntity
import dev.adirelle.adicrate.block.entity.internal.CrateStorage
import dev.adirelle.adicrate.utils.extensions.set
import mcp.mobius.waila.api.*
import mcp.mobius.waila.api.component.ItemComponent
import mcp.mobius.waila.api.component.PairComponent
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.client.MinecraftClient
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.LiteralText
import net.minecraft.text.TranslatableText
import net.minecraft.util.math.Direction
import net.minecraft.world.World

object CrateProvider : IWailaPlugin, IBlockComponentProvider, IServerDataProvider<CrateBlockEntity> {

    val showContent = AdiCrate.id("show_content")
    val showController = AdiCrate.id("show_controller")
    val showInteractionHints = AdiCrate.id("show_interaction_hints")

    val IPluginConfig.showContent: Boolean
        get() = getBoolean(this@CrateProvider.showContent)

    val IPluginConfig.showController: Boolean
        get() = getBoolean(this@CrateProvider.showController)

    val IPluginConfig.showInteractionHints: Boolean
        get() = getBoolean(this@CrateProvider.showInteractionHints)

    private const val DATA_KEY = "${AdiCrate.MOD_ID}:crate"

    override fun register(registrar: IRegistrar) {
        registrar.addSyncedConfig(showContent, true)
        registrar.addSyncedConfig(showController, true)
        registrar.addSyncedConfig(showInteractionHints, true)
        registrar.addComponent(this, TooltipPosition.BODY, CrateBlockEntity::class.java)
        registrar.addComponent(this, TooltipPosition.TAIL, CrateBlockEntity::class.java)
        registrar.addBlockData(this, CrateBlockEntity::class.java)
    }

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
            nbt["facing"] = blockEntity.facing.toString()
            blockEntity.networkInfo?.let {
                nbt["networkInfo"] = it.name
            }
        }
    }

    override fun appendBody(tooltip: ITooltip, accessor: IBlockAccessor, config: IPluginConfig) {
        val nbt = accessor.serverData.getCompound(DATA_KEY)

        if (config.showContent) {
            val resource = ItemVariant.fromNbt(nbt.getCompound("resource"))
            tooltip.addLine()
                .with(ItemComponent(resource.toStack(1)))
                .with(CrateStorage.contentText(nbt.getLong("amount"), nbt.getLong("capacity")))
        }

        if (config.showController) {
            val networkInfo = nbt.getString("networkInfo")
            if (!networkInfo.isEmpty()) {
                tooltip.addLine()
                    .with(
                        PairComponent(
                            TranslatableText("tooltip.adicrate.crate.network_info"),
                            LiteralText(networkInfo)
                        )
                    )
            }
        }

        if (config.showContent) {
            if (nbt.getBoolean("jammed")) {
                tooltip.addLine(TranslatableText("tooltip.adicrate.crate.jammed"))
            }
        }
    }

    override fun appendTail(tooltip: ITooltip, accessor: IBlockAccessor, config: IPluginConfig) {
        if (!config.showInteractionHints) return

        val facing = Direction.valueOf(accessor.serverData.getCompound(DATA_KEY).getString("facing"))

        fun addHint(id: String, vararg args: Any) {
            tooltip.addLine(TranslatableText("tooltip.adicrate.crate.interaction_hints.$id", *args))
        }

        with(MinecraftClient.getInstance().options) {
            val useKey = keyUse.boundKeyLocalizedText
            val attackKey = keyAttack.boundKeyLocalizedText
            val sneakKey = keySneak.boundKeyLocalizedText

            if (accessor.side == facing) {
                addHint("pull_item", attackKey)
                addHint("pull_one_item", sneakKey, attackKey)
                addHint("push_item", useKey)
                addHint("push_inventory", sneakKey, useKey)
            } else {
                addHint("open_gui", useKey)
            }
        }
    }
}
