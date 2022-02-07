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
}
