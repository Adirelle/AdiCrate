@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate

import dev.adirelle.adicrate.block.ControllerBlock
import dev.adirelle.adicrate.block.entity.ControllerBlockEntity
import dev.adirelle.adicrate.utils.extensions.register
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType.CLIENT
import net.fabricmc.api.Environment
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier

@Suppress("unused", "MemberVisibilityCanBePrivate")
object Controller : ModInitializer, ClientModInitializer {

    private val LOGGER = AdiCrate.LOGGER

    val ID = Identifier(AdiCrate.MOD_ID, "controller")

    val BLOCK = ControllerBlock()

    val ITEM = BlockItem(BLOCK, FabricItemSettings().group(ItemGroup.INVENTORY))

    val BLOCK_ENTITY_TYPE = BlockEntityType(::ControllerBlockEntity, setOf(BLOCK), null)

    override fun onInitialize() {
        BLOCK.register(ID)
        ITEM.register(ID)
        BLOCK_ENTITY_TYPE.register(ID)

        ItemStorage.SIDED.registerForBlockEntity({ blockEntity, _ -> blockEntity.storage }, BLOCK_ENTITY_TYPE)
    }

    @Environment(CLIENT)
    override fun onInitializeClient() {
        AttackBlockCallback.EVENT.register(BLOCK::onAttackedByPlayer)
    }
}
