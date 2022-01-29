@file:Suppress("MemberVisibilityCanBePrivate", "PropertyName", "unused")

package dev.adirelle.adicrate.utils

import com.mojang.datafixers.types.Type
import net.fabricmc.api.*
import net.fabricmc.api.EnvType.CLIENT
import net.fabricmc.api.EnvType.SERVER
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@EnvironmentInterface(CLIENT, itf = ClientModInitializer::class)
@EnvironmentInterface(SERVER, itf = DedicatedServerModInitializer::class)
interface SidedModInitalizer : ModInitializer, ClientModInitializer, DedicatedServerModInitializer {

    val LOGGER: Logger

    override fun onInitialize() {
    }

    @Environment(CLIENT)
    override fun onInitializeClient() {
    }

    @Environment(SERVER)
    override fun onInitializeServer() {
    }
}

open class Mod(val name: String, val MOD_ID: String) : SidedModInitalizer {

    final override val LOGGER = LogManager.getLogger(name)!!

    internal val features = mutableListOf<SidedModInitalizer>()

    private var initialized = false

    fun load(vararg newFeatures: ModFeature) {
        require(!initialized) { "Mod::load should be used before calling super.onInitialize" }
        if (!initialized) {
            features.addAll(newFeatures)
        }
    }

    override fun onInitialize() {
        initialized = true
        features.forEach(SidedModInitalizer::onInitialize)
        features.clear()
    }

    @Environment(CLIENT)
    override fun onInitializeClient() {
        features.forEach(SidedModInitalizer::onInitializeClient)
    }

    @Environment(SERVER)
    override fun onInitializeServer() {
        features.forEach(SidedModInitalizer::onInitializeServer)
    }

    override fun toString() = name
}

open class ModFeature(mod: Mod) : SidedModInitalizer {

    final override val LOGGER = mod.LOGGER

    val MOD_ID = mod.MOD_ID

    fun id(name: String) = Identifier(MOD_ID, name)

    fun <T : Block> registerBlock(id: Identifier, entry: T): T =
        entry.also { Registry.BLOCK.registerBlockEntity(id, it) }

    fun <T : Item> registerItem(id: Identifier, entry: T): T =
        entry.also { Registry.ITEM.registerBlockEntity(id, entry) }

    inline fun <T : Block> registerItemFor(
        id: Identifier,
        block: T,
        crossinline settings: FabricItemSettings.() -> FabricItemSettings
    ) =
        registerItem(id, BlockItem(block, FabricItemSettings().settings()))

    fun <T : Block> registerItemFor(id: Identifier, block: T) =
        registerItem(id, BlockItem(block, FabricItemSettings()))

    fun <T : BlockEntity, S : T> registerBlockEntity(
        id: Identifier,
        factory: (BlockPos, BlockState) -> T,
        type: Type<S>? = null,
        vararg blocks: Block,
    ): BlockEntityType<T> =
        registerBlockEntity(id, BlockEntityType(factory, setOf(*blocks), type))

    @Suppress("UNCHECKED_CAST")
    fun <T : BlockEntity> registerBlockEntity(
        id: Identifier,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityType<T> =
        Registry.BLOCK_ENTITY_TYPE.registerBlockEntity(id, blockEntityType) as BlockEntityType<T>

    fun <T : ScreenHandler> registerSimple(id: Identifier, factory: (Int, PlayerInventory) -> T): ScreenHandlerType<T> =
        ScreenHandlerRegistry.registerSimple(id, factory)

    fun <T : ScreenHandler> registerExtended(
        id: Identifier,
        factory: (Int, PlayerInventory, PacketByteBuf) -> T,
    ): ScreenHandlerType<T> =
        ScreenHandlerRegistry.registerExtended(id, factory)

    fun <T, S> registerScreen(
        type: ScreenHandlerType<T>,
        factory: (T, PlayerInventory, Text) -> S,
    ) where T : ScreenHandler, S : Screen, S : ScreenHandlerProvider<T> =
        ScreenRegistry.register(type, factory)

    private fun <T> Registry<T>.registerBlockEntity(id: Identifier, entry: T): T =
        Registry.register(this, id, entry)
}
