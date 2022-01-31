@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.block

import dev.adirelle.adicrate.Crate
import dev.adirelle.adicrate.block.entity.CrateBlockEntity
import dev.adirelle.adicrate.block.entity.internal.InventoryAdapter
import dev.adirelle.adicrate.network.PullItemC2SPacket
import net.fabricmc.api.EnvType.CLIENT
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContext
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.state.StateManager.Builder
import net.minecraft.state.property.Properties.HORIZONTAL_FACING
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
import net.minecraft.world.WorldAccess

class CrateBlock :
    BlockWithEntity(FabricBlockSettings.of(Material.WOOD).strength(2.0f)),
    InventoryProvider {

    init {
        defaultState = stateManager.defaultState.with(HORIZONTAL_FACING, NORTH)
    }

    override fun appendProperties(builder: Builder<Block, BlockState>) {
        builder.add(HORIZONTAL_FACING)
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
            if (state.isOf(this) && direction == state.get(HORIZONTAL_FACING)) {
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

    fun pullItems(world: World, pos: BlockPos, player: PlayerEntity) {
        world.getBlockEntity(pos, Crate.BLOCK_ENTITY_TYPE).ifPresent { blockEntity ->
            blockEntity.extractFor(player, !player.isSneaking)
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
                    world.isClient                           -> Unit
                    hit.side == state.get(HORIZONTAL_FACING) -> blockEntity.insertFrom(player, hand)
                    else                                     -> player.openHandledScreen(blockEntity)
                }
                SUCCESS
            }
            .orElse(PASS)
    }

    override fun getRenderType(state: BlockState) = BlockRenderType.MODEL

    override fun getInventory(state: BlockState, world: WorldAccess, pos: BlockPos): SidedInventory =
        world.getBlockEntity(pos, Crate.BLOCK_ENTITY_TYPE).map {
            InventoryAdapter(it.storage)
        }.orElseThrow { IllegalStateException("no crate at %s".format(pos.toShortString())) }

    override fun rotate(state: BlockState, rotation: BlockRotation): BlockState =
        state.with(HORIZONTAL_FACING, rotation.rotate(state.get(HORIZONTAL_FACING)))

    override fun mirror(state: BlockState, mirror: BlockMirror): BlockState =
        state.rotate(mirror.getRotation(state.get(HORIZONTAL_FACING)))

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState =
        defaultState.with(HORIZONTAL_FACING, ctx.placementDirections.first(Direction.Type.HORIZONTAL::test).opposite)
}
