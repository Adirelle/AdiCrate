@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.utils.extensions

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.minecraft.item.ItemStack

val StorageView<ItemVariant>.count: Int
    get() = amount.toInt()

fun StorageView<ItemVariant>.toStack(): ItemStack =
    if (isResourceBlank || amount == 0L)
        ItemStack.EMPTY
    else
        resource.toStack(count)
