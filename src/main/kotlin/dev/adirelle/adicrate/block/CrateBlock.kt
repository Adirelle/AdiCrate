package dev.adirelle.adicrate.block

import dev.adirelle.adicrate.block.entity.CrateBlockEntity
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager.Builder
import net.minecraft.state.property.DirectionProperty
import net.minecraft.util.BlockMirror
import net.minecraft.util.BlockRotation
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction.NORTH

class CrateBlock : BlockWithEntity(FabricBlockSettings.of(Material.WOOD)) {

    companion object {

        val FACING: DirectionProperty = DirectionProperty.of("facing")
    }

    init {
        defaultState = stateManager.defaultState.with(FACING, NORTH)
    }

    override fun appendProperties(builder: Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CrateBlockEntity(pos, state)

    override fun getRenderType(state: BlockState) = BlockRenderType.MODEL

    override fun rotate(state: BlockState, rotation: BlockRotation): BlockState =
        state.with(FACING, rotation.rotate(state.get(FACING)))

    override fun mirror(state: BlockState, mirror: BlockMirror): BlockState =
        state.rotate(mirror.getRotation(state.get(FACING)))

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState =
        defaultState.with(FACING, ctx.playerLookDirection.opposite)
}
