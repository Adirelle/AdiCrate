package dev.adirelle.adicrate

import dev.adirelle.adicrate.block.CrateBlock
import dev.adirelle.adicrate.block.entity.CrateBlockEntity
import dev.adirelle.adicrate.utils.ModFeature
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemGroup

object Crate : ModFeature(AdiCrate) {

    val BLOCK = CrateBlock()

    val ITEM = BlockItem(BLOCK, FabricItemSettings().group(ItemGroup.INVENTORY))

    val BLOCK_ENTITY = BlockEntityType(::CrateBlockEntity, setOf(BLOCK), null)

    override fun onInitialize() {
        val id = id("crate")
        registerBlock(id, BLOCK)
        registerItem(id, ITEM)
        registerBlockEntity(id, BLOCK_ENTITY)
    }

}
