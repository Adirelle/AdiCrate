@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.block

import dev.adirelle.adicrate.Controller
import dev.adirelle.adicrate.block.entity.ControllerBlockEntity
import dev.adirelle.adicrate.misc.Network
import dev.adirelle.adicrate.utils.extensions.withBlockEntity
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil
import net.minecraft.block.BlockState
import net.minecraft.block.Material
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResult.PASS
import net.minecraft.util.ActionResult.SUCCESS
import net.minecraft.util.Hand
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

    override fun pullItems(world: World, pos: BlockPos, player: PlayerEntity) {
        world.withBlockEntity(pos, Controller.BLOCK_ENTITY_TYPE) { controller ->
            ContainerItemContext.ofPlayerHand(player, player.preferredHand)
                .mainSlot.let { playerHand ->
                    when {
                        !playerHand.isResourceBlank ->
                            StorageUtil.move(
                                controller.storage,
                                playerHand,
                                { it == playerHand.resource },
                                playerHand.capacity - playerHand.amount,
                                null
                            )
                        player.isSneaking           ->
                            PlayerInventoryStorage.of(player)
                                .slots
                                .filterNot { it.isResourceBlank || it.amount >= it.capacity }
                                .forEach { slot ->
                                    StorageUtil.move(
                                        controller.storage,
                                        slot,
                                        { it == slot.resource },
                                        slot.capacity - slot.amount,
                                        null
                                    )
                                }
                        else                        ->
                            Unit
                    }
                }
        }
    }

    override fun pushItems(world: World, pos: BlockPos, player: PlayerEntity, hand: Hand): ActionResult {
        world.withBlockEntity(pos, Controller.BLOCK_ENTITY_TYPE) { controller ->
            ContainerItemContext.ofPlayerHand(player, player.preferredHand)
                .mainSlot.let { playerHand ->
                    when {
                        !playerHand.isResourceBlank -> StorageUtil.move(
                            playerHand,
                            controller.storage,
                            { true },
                            playerHand.amount,
                            null
                        )
                        player.isSneaking           ->
                            StorageUtil.move(
                                PlayerInventoryStorage.of(player),
                                controller.storage,
                                { true },
                                Long.MAX_VALUE,
                                null
                            )
                        else                        ->
                            Unit
                    }
                }
        }
        return SUCCESS
    }

    override fun onUseInternal(world: World, pos: BlockPos, player: PlayerEntity) = PASS
}
