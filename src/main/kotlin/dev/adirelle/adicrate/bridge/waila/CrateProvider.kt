@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.bridge.waila

import dev.adirelle.adicrate.AdiCrate
import dev.adirelle.adicrate.block.entity.CrateBlockEntity
import dev.adirelle.adicrate.block.entity.internal.CrateStorage
import dev.adirelle.adicrate.utils.extensions.set
import dev.adirelle.adicrate.utils.logger
import mcp.mobius.waila.api.*
import mcp.mobius.waila.api.component.ItemComponent
import mcp.mobius.waila.api.component.PairComponent
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.client.MinecraftClient
import net.minecraft.client.item.TooltipContext
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.LiteralText
import net.minecraft.text.TranslatableText
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import java.util.*

object CrateProvider : IWailaPlugin, IBlockComponentProvider, IServerDataProvider<CrateBlockEntity> {

    private val LOGGER by logger

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
        val clazz = CrateBlockEntity::class.java
        registrar.addIcon(this, clazz)
        registrar.addComponent(this, TooltipPosition.BODY, clazz)
        registrar.addComponent(this, TooltipPosition.TAIL, clazz)
        registrar.addBlockData(this, clazz)
    }

    override fun appendServerData(
        data: NbtCompound,
        player: ServerPlayerEntity,
        world: World,
        blockEntity: CrateBlockEntity
    ) {
        data[DATA_KEY] = ServerData.of(blockEntity)
            .writeNbt(NbtCompound())
    }

    private inline fun <R> IBlockAccessor.withServerData(crossinline block: (ServerData) -> R): Optional<R> =
        Optional
            .of(serverData.getCompound(DATA_KEY))
            .filter { !it.isEmpty }
            .map(ServerData.Companion::fromNbt)
            .map { block(it) }

    override fun getIcon(accessor: IBlockAccessor, config: IPluginConfig): ItemComponent? {
        LOGGER.info("getIcon, data={}", accessor.serverData.asString())
        return accessor
            .withServerData { data ->
                if (true || data.isFacing(accessor.side))
                    ItemComponent(data.resource.toStack())
                else
                    null
            }
            .orElse(null)

    }

    override fun appendBody(tooltip: ITooltip, accessor: IBlockAccessor, config: IPluginConfig) {
        accessor.withServerData { data ->

            if (config.showContent) {
                val stack = data.resource.toStack(data.amount.toInt())

                if (!stack.isEmpty) {
                    stack.getTooltip(accessor.player, TooltipContext.Default.NORMAL)
                        .forEach(tooltip::addLine)
                }

                tooltip.addLine()
                    .with(CrateStorage.contentText(data.amount, data.capacity))
            }

            if (config.showController && data.networkName.isNotEmpty()) {
                tooltip.addLine()
                    .with(
                        PairComponent(
                            TranslatableText("tooltip.adicrate.crate.network_info"),
                            LiteralText(data.networkName)
                        )
                    )
            }

            if (config.showContent && data.isJammed) {
                tooltip.addLine(TranslatableText("tooltip.adicrate.crate.jammed"))
            }
        }
    }

    override fun appendTail(tooltip: ITooltip, accessor: IBlockAccessor, config: IPluginConfig) {
        if (!config.showInteractionHints) return

        fun addHint(id: String, vararg args: Any) {
            tooltip.addLine(TranslatableText("tooltip.adicrate.crate.interaction_hints.$id", *args))
        }

        with(MinecraftClient.getInstance().options) {
            val useKey = keyUse.boundKeyLocalizedText
            val attackKey = keyAttack.boundKeyLocalizedText
            val sneakKey = keySneak.boundKeyLocalizedText

            if (accessor.withServerData { it.isFacing(accessor.side) }.orElse(false)) {
                addHint("pull_item", attackKey)
                addHint("pull_one_item", sneakKey, attackKey)
                addHint("push_item", useKey)
                addHint("push_inventory", sneakKey, useKey)
            } else {
                addHint("open_gui", useKey)
            }
        }
    }

    private data class ServerData(
        val resource: ItemVariant,
        val amount: Long,
        val capacity: Long,
        val isJammed: Boolean,
        val facing: Direction,
        var networkName: String
    ) {

        companion object {

            fun of(blockEntity: CrateBlockEntity) =
                ServerData(
                    blockEntity.storage.resource,
                    blockEntity.storage.amount,
                    blockEntity.storage.realCapacity,
                    blockEntity.storage.isJammed(),
                    blockEntity.facing,
                    blockEntity.networkInfo?.name ?: ""
                )

            fun fromNbt(nbt: NbtCompound) =
                with(nbt) {
                    ServerData(
                        ItemVariant.fromNbt(getCompound("resource")),
                        getLong("amount"),
                        getLong("capacity"),
                        getBoolean("jammed"),
                        Direction.fromHorizontal(getInt("facing")),
                        getString("networkName")
                    )
                }
        }

        fun isFacing(direction: Direction): Boolean =
            direction == facing

        fun writeNbt(nbt: NbtCompound) = nbt.also {
            it["resource"] = resource
            it["amount"] = amount
            it["capacity"] = capacity
            it["jammed"] = isJammed
            it["facing"] = facing.horizontal
            it["networkName"] = networkName
        }
    }
}
