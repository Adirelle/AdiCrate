package dev.adirelle.adicrate.block

import dev.adirelle.adicrate.network.PullItemC2SPacket
import net.fabricmc.api.EnvType.CLIENT
import net.fabricmc.api.Environment
import net.minecraft.block.Block
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager.Builder
import net.minecraft.state.property.Properties
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

abstract class AbstractContainerBlock(settings: Settings) : BlockWithEntity(settings), ModBlock {

    init {
        defaultState = stateManager.defaultState.with(Properties.HORIZONTAL_FACING, NORTH)
    }

    override fun appendProperties(builder: Builder<Block, BlockState>) {
        builder.add(Properties.HORIZONTAL_FACING)
    }

    override fun rotate(state: BlockState, rotation: BlockRotation): BlockState =
        state.with(Properties.HORIZONTAL_FACING, rotation.rotate(state.get(Properties.HORIZONTAL_FACING)))

    override fun mirror(state: BlockState, mirror: BlockMirror): BlockState =
        state.rotate(mirror.getRotation(state.get(Properties.HORIZONTAL_FACING)))

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState =
        defaultState.with(
            Properties.HORIZONTAL_FACING,
            ctx.placementDirections.first(Direction.Type.HORIZONTAL::test).opposite
        )

    override fun getRenderType(state: BlockState) = BlockRenderType.MODEL

    @Environment(CLIENT)
    @Suppress("UNUSED_PARAMETER")
    fun onAttackedByPlayer(
        player: PlayerEntity,
        world: World,
        hand: Hand,
        pos: BlockPos,
        direction: Direction
    ): ActionResult =
        world.getBlockState(pos).let { state ->
            if (state.isOf(this) && direction == state.get(Properties.HORIZONTAL_FACING)) {
                if (!player.handSwinging) {
                    PullItemC2SPacket.send(player, pos)
                    player.swingHand(hand)
                }
                // Do not let the client send an "attack" packet
                FAIL
            } else
            // Not ours or not in face, do not care
                PASS
        }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult =
        when {
            !state.isOf(this)                                   -> PASS
            world.isClient                                      -> SUCCESS
            hit.side == state.get(Properties.HORIZONTAL_FACING) -> pushItems(world, pos, player, hand)
            else                                                -> onUseInternal(world, pos, player)
        }

    override fun onStateReplaced(
        state: BlockState,
        world: World,
        pos: BlockPos,
        newState: BlockState,
        moved: Boolean
    ) {
        if (!world.isClient && !newState.isOf(this)) {
            onRemoved(world, pos)
        }

        @Suppress("DEPRECATION")
        super.onStateReplaced(state, world, pos, newState, moved)
    }

    internal abstract fun pullItems(world: World, pos: BlockPos, player: PlayerEntity)

    protected abstract fun pushItems(world: World, pos: BlockPos, player: PlayerEntity, hand: Hand): ActionResult

    protected abstract fun onUseInternal(world: World, pos: BlockPos, player: PlayerEntity): ActionResult

    protected abstract fun onRemoved(world: World, pos: BlockPos)
}
