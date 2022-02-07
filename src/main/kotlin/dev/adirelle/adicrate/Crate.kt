@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate

import dev.adirelle.adicrate.block.CrateBlock
import dev.adirelle.adicrate.block.entity.CrateBlockEntity
import dev.adirelle.adicrate.client.renderer.CrateRenderer
import dev.adirelle.adicrate.client.screen.CrateScreen
import dev.adirelle.adicrate.screen.CrateScreenHandler
import dev.adirelle.adicrate.utils.extensions.register
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType.CLIENT
import net.fabricmc.api.Environment
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemGroup

@Suppress("unused", "MemberVisibilityCanBePrivate")
object Crate : ModInitializer, ClientModInitializer {

    private val LOGGER = AdiCrate.LOGGER

    val ID = AdiCrate.id("crate")

    val BLOCK = CrateBlock()

    val ITEM = BlockItem(BLOCK, FabricItemSettings().group(ItemGroup.INVENTORY))

    val BLOCK_ENTITY_TYPE = BlockEntityType(::CrateBlockEntity, setOf(BLOCK), null)

    val SCREEN_HANDLER_TYPE = ScreenHandlerRegistry.registerSimple(ID, ::CrateScreenHandler)

    override fun onInitialize() {
        BLOCK.register(ID)
        ITEM.register(ID)
        BLOCK_ENTITY_TYPE.register(ID)

        ItemStorage.SIDED.registerForBlockEntity({ blockEntity, _ -> blockEntity.storage }, BLOCK_ENTITY_TYPE)
    }

    @Environment(CLIENT)
    override fun onInitializeClient() {
        BlockEntityRendererRegistry.register(BLOCK_ENTITY_TYPE, ::CrateRenderer)
        ScreenRegistry.register(SCREEN_HANDLER_TYPE, ::CrateScreen)

        AttackBlockCallback.EVENT.register(BLOCK::onAttackedByPlayer)
    }
}
