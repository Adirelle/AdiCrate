@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.utils.extensions

import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

fun Inventory.iterator() =
    InventoryIterator(this)

class InventoryIterator(private val inventory: Inventory) : Iterator<ItemStack> {

    private var idx = 0
    override fun hasNext() = idx < inventory.size()
    override fun next(): ItemStack = inventory.getStack(idx++)
}
