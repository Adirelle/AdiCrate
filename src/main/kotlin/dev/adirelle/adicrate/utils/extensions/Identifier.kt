package dev.adirelle.adicrate.utils.extensions

import net.minecraft.item.Item
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

fun Identifier.toItem(): Item =
    Registry.ITEM[this]
