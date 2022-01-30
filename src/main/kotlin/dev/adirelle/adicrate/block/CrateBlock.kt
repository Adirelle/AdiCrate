@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.block

import dev.adirelle.adicrate.AdiCrate
import dev.adirelle.adicrate.Crate
import dev.adirelle.adicrate.block.entity.CrateBlockEntity
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
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
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.World

class CrateBlock : BlockWithEntity(FabricBlockSettings.of(Material.WOOD).strength(2.0f)) {

    private val LOGGER = AdiCrate.LOGGER

    init {
        defaultState = stateManager.defaultState.with(FACING, NORTH)
    }

    override fun appendProperties(builder: Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CrateBlockEntity(pos, state)

    @Suppress("DEPRECATION")
    override fun getDroppedStacks(state: BlockState, builder: LootContext.Builder): MutableList<ItemStack> {
        val crate = builder.get(LootContextParameters.BLOCK_ENTITY) as? CrateBlockEntity
            ?: return super.getDroppedStacks(state, builder)
        return MutableList(1) {
            ItemStack(Item.BLOCK_ITEMS[this]).also {
                BlockItem.setBlockEntityNbt(it, crate.type, crate.createNbt())
            }
        }
    }

    override fun onBlockBreakStart(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity) {
        if (world.isClient) return
        (world.getBlockEntity(pos) as? CrateBlockEntity?)?.let { blockEntity ->
            val start = player.getCameraPosVec(0.0f)
            val end = start.add(player.getRotationVec(0.0f).multiply(5.0))
            val blockHit = world.raycastBlock(start, end, pos, VoxelShapes.fullCube(), state)
            if (blockHit?.side == state.get(FACING)) {
                blockEntity.extractFor(player, !player.isSneaking)
            }
        }
    }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        return world.getBlockEntity(pos, Crate.BLOCK_ENTITY_TYPE)
            .map { blockEntity ->
                when {
                    world.isClient                -> SUCCESS
                    hit.side == state.get(FACING) -> success(blockEntity.insertFrom(player, hand))
                    else                          -> PASS
                }
            }
            .orElse(PASS)
    }

    override fun getRenderType(state: BlockState) = BlockRenderType.MODEL

    override fun rotate(state: BlockState, rotation: BlockRotation): BlockState =
        state.with(FACING, rotation.rotate(state.get(FACING)))

    override fun mirror(state: BlockState, mirror: BlockMirror): BlockState =
        state.rotate(mirror.getRotation(state.get(FACING)))

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState =
        defaultState.with(FACING, ctx.placementDirections.first(Direction.Type.HORIZONTAL::test).opposite)
}
