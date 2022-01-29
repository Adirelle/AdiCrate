package dev.adirelle.adicrate.utils.extensions

import net.minecraft.item.Item
import net.minecraft.nbt.NbtString
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

val Item.id: Identifier
    get() = Registry.ITEM.getId(this)

fun Item.toNbt(): NbtString =
    NbtString.of(id.toString())
