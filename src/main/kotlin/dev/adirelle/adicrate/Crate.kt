package dev.adirelle.adicrate

import dev.adirelle.adicrate.block.CrateBlock
import dev.adirelle.adicrate.block.entity.CrateBlockEntity
import dev.adirelle.adicrate.client.renderer.CrateRenderer
import dev.adirelle.adicrate.utils.extensions.register
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType.CLIENT
import net.fabricmc.api.Environment
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier

@Suppress("unused", "MemberVisibilityCanBePrivate")
object Crate : ModInitializer, ClientModInitializer {

    private val LOGGER = AdiCrate.LOGGER

    val ID = Identifier(AdiCrate.MOD_ID, "crate")

    val BLOCK = CrateBlock()

    val ITEM = BlockItem(BLOCK, FabricItemSettings().group(ItemGroup.INVENTORY))

    val BLOCK_ENTITY_TYPE = BlockEntityType(::CrateBlockEntity, setOf(BLOCK), null)

    override fun onInitialize() {
        BLOCK.register(ID)
        ITEM.register(ID)
        BLOCK_ENTITY_TYPE.register(ID)
    }

    @Environment(CLIENT)
    override fun onInitializeClient() {
        BlockEntityRendererRegistry.register(BLOCK_ENTITY_TYPE, ::CrateRenderer)
    }
}
