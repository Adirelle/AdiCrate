package dev.adirelle.adicrate.utils.extensions

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

fun Identifier.toItem(): Item =
    Registry.ITEM[this]

fun Identifier.toStack(count: Int = 1): ItemStack =
    ItemStack(toItem(), count)
