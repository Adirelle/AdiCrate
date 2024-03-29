@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.block

import dev.adirelle.adicrate.Controller
import dev.adirelle.adicrate.abstraction.Network
import dev.adirelle.adicrate.block.entity.ControllerBlockEntity
import dev.adirelle.adicrate.utils.extensions.withBlockEntity
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.BlockState
import net.minecraft.block.Material
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContext
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class ControllerBlock :
    AbstractContainerBlock(FabricBlockSettings.of(Material.STONE).strength(1.0f)) {

    override fun createBlockEntity(pos: BlockPos, state: BlockState) =
        ControllerBlockEntity(pos, state)

    override fun <T : BlockEntity?> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        checkType(type, Controller.BLOCK_ENTITY_TYPE) { wrld, _, _, blockEntity ->
            blockEntity.tick(wrld)
        }

    override fun onRemoved(world: World, pos: BlockPos) {
        world.withBlockEntity(pos, Controller.BLOCK_ENTITY_TYPE, Network::destroy)
    }

    override fun getDroppedStacks(state: BlockState, builder: LootContext.Builder) =
        mutableListOf(ItemStack(Item.BLOCK_ITEMS[this]))

    override fun onPlaced(
        world: World,
        pos: BlockPos,
        state: BlockState,
        placer: LivingEntity?,
        itemStack: ItemStack
    ) {
        if (itemStack.hasCustomName()) {
            LOGGER.info("custom name: {}", itemStack.name)
            world.withBlockEntity(pos, Controller.BLOCK_ENTITY_TYPE) {
                it.name = itemStack.name.asString()
                LOGGER.info("network name: {}", it.name)
            }
        }
    }
}
