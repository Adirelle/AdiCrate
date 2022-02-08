@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate

import dev.adirelle.adicrate.block.ControllerBlock
import dev.adirelle.adicrate.block.entity.ControllerBlockEntity
import dev.adirelle.adicrate.utils.extensions.register
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemGroup

@Suppress("unused", "MemberVisibilityCanBePrivate")
object Controller : ModInitializer {

    private val LOGGER = AdiCrate.LOGGER

    val ID = AdiCrate.id("controller")

    val BLOCK = ControllerBlock()

    val ITEM = BlockItem(BLOCK, FabricItemSettings().group(ItemGroup.INVENTORY))

    val BLOCK_ENTITY_TYPE = BlockEntityType(::ControllerBlockEntity, setOf(BLOCK), null)

    override fun onInitialize() {
        BLOCK.register(ID)
        ITEM.register(ID)
        BLOCK_ENTITY_TYPE.register(ID)

        ItemStorage.SIDED.registerForBlockEntity({ blockEntity, _ -> blockEntity.storage }, BLOCK_ENTITY_TYPE)
    }
}
