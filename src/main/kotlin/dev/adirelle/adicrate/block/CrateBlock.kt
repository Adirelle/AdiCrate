@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.block

import dev.adirelle.adicrate.Crate
import dev.adirelle.adicrate.abstraction.Network
import dev.adirelle.adicrate.abstraction.Network.Node
import dev.adirelle.adicrate.block.entity.CrateBlockEntity
import dev.adirelle.adicrate.block.entity.internal.CrateStorage
import dev.adirelle.adicrate.utils.extensions.withBlockEntity
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.BlockState
import net.minecraft.block.Material
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContext
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResult.SUCCESS
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.BlockView
import net.minecraft.world.World

class CrateBlock :
    AbstractContainerBlock(FabricBlockSettings.of(Material.WOOD).strength(1.0f)) {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CrateBlockEntity(pos, state)

    override fun getDroppedStacks(state: BlockState, builder: LootContext.Builder): MutableList<ItemStack> =
        @Suppress("DEPRECATION")
        super.getDroppedStacks(state, builder).also { list ->
            val blockEntity = builder.get(LootContextParameters.BLOCK_ENTITY)
            if (blockEntity is CrateBlockEntity) {
                val stack = ItemStack(Item.BLOCK_ITEMS[this])
                BlockItem.setBlockEntityNbt(stack, blockEntity.type, blockEntity.createNbt())
                list.add(stack)
            }
        }

    override fun appendTooltip(
        stack: ItemStack,
        world: BlockView?,
        tooltip: MutableList<Text>,
        options: TooltipContext
    ) {
        if (!stack.isOf(Crate.ITEM)) return
        val nbt = BlockItem.getBlockEntityNbt(stack) ?: return
        CrateStorage.itemText(nbt).ifPresent(tooltip::add)
        tooltip.add(CrateStorage.contentText(nbt))
    }

    override fun onUseInternal(world: World, pos: BlockPos, player: PlayerEntity): ActionResult {
        world.withBlockEntity(pos, Crate.BLOCK_ENTITY_TYPE) {
            player.openHandledScreen(it)
        }
        return SUCCESS
    }

    override fun onPlaced(world: World, pos: BlockPos, state: BlockState, placer: LivingEntity?, itemStack: ItemStack) {
        world.withBlockEntity(pos, Crate.BLOCK_ENTITY_TYPE) { node ->
            LOGGER.debug("placed, looking for network")
            for (direction in Direction.values()) {
                val neighbor = world.getBlockEntity(pos.offset(direction))
                val connected = when (neighbor) {
                    is Network -> node.connectTo(neighbor)
                    is Node    -> neighbor.connectWith(node)
                    else       -> false
                }
                if (connected) break
            }
        }
    }

    override fun onRemoved(world: World, pos: BlockPos) {
        world.withBlockEntity(pos, Crate.BLOCK_ENTITY_TYPE) {
            it.disconnect()
        }
    }
}
