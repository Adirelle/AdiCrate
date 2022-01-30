@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.block

import dev.adirelle.adicrate.AdiCrate
import dev.adirelle.adicrate.block.entity.CrateBlockEntity
import dev.adirelle.adicrate.network.PlayerExtractPacket
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.block.BlockAttackInteractionAware
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContext
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.state.StateManager.Builder
import net.minecraft.state.property.Properties.FACING
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResult.*
import net.minecraft.util.BlockMirror
import net.minecraft.util.BlockRotation
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Direction.NORTH
import net.minecraft.world.World

class CrateBlock :
    BlockWithEntity(FabricBlockSettings.of(Material.WOOD)), BlockAttackInteractionAware {

    private val LOGGER = AdiCrate.LOGGER

    init {
        defaultState = stateManager.defaultState.with(FACING, NORTH)
    }

    override fun appendProperties(builder: Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CrateBlockEntity(pos, state)

    override fun getDroppedStacks(state: BlockState, builder: LootContext.Builder): MutableList<ItemStack> {
        val crate = builder.get(LootContextParameters.BLOCK_ENTITY) as? CrateBlockEntity
            ?: return super.getDroppedStacks(state, builder)
        return MutableList(1) {
            ItemStack(Item.BLOCK_ITEMS[this]).also {
                BlockItem.setBlockEntityNbt(it, crate.type, crate.createNbt())
            }
        }
    }

    override fun onAttackInteraction(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        direction: Direction
    ): Boolean {
        if (!state.isOf(this) || direction != state.get(FACING)) return false
        if (world.isClient) {
            PlayerExtractPacket(player, pos, !player.isSneaking).send()
        }
        return true
    }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        val crate = (world.getBlockEntity(pos) as? CrateBlockEntity?)?.storage ?: return PASS
        if (player.getStackInHand(hand).isEmpty || hit.side != state.get(FACING)) return PASS
        if (world.isClient) return SUCCESS
        val playerHand = ContainerItemContext.ofPlayerHand(player, hand).mainSlot
        val moved = StorageUtil.move(playerHand, crate, { true }, playerHand.amount, null)
        return success(moved > 0)

    }

    override fun getRenderType(state: BlockState) = BlockRenderType.MODEL

    override fun rotate(state: BlockState, rotation: BlockRotation): BlockState =
        state.with(FACING, rotation.rotate(state.get(FACING)))

    override fun mirror(state: BlockState, mirror: BlockMirror): BlockState =
        state.rotate(mirror.getRotation(state.get(FACING)))

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState =
        defaultState.with(FACING, ctx.placementDirections.first(Direction.Type.HORIZONTAL::test).opposite)
}
