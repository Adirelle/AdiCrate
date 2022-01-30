package dev.adirelle.adicrate.utils.extensions

import net.minecraft.block.Block
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.Item
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

fun <T, R : Registry<T>> R.register(id: Identifier, entry: T): T =
    Registry.register(this, id, entry)

fun Item.register(id: Identifier): Item =
    Registry.ITEM.register(id, this)

fun Block.register(id: Identifier): Block =
    Registry.BLOCK.register(id, this)

fun <T : BlockEntity> BlockEntityType<T>.register(id: Identifier): BlockEntityType<T> =
    this.also { Registry.BLOCK_ENTITY_TYPE.register(id, this) }
