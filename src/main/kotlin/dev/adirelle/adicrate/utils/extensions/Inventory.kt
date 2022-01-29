@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.utils.extensions

import dev.adirelle.adicrate.utils.memoize
import dev.adirelle.adicrate.utils.requireExactSize
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

fun Inventory.copyFrom(list: List<ItemStack>) {
    requireExactSize(list, size()).forEachIndexed { index, stack ->
        this[index] = stack
    }
}

fun Inventory.iterator() =
    InventoryIterator(this)

operator fun Inventory.get(index: Int): ItemStack =
    getStack(index)

operator fun Inventory.set(index: Int, stack: ItemStack) =
    setStack(index, stack)

private val storageMemoizer = memoize<Inventory, InventoryStorage> { inventory ->
    InventoryStorage.of(inventory, null)
}

fun Inventory.asStorage(): InventoryStorage =
    storageMemoizer(this)

class InventoryIterator(private val inventory: Inventory) : Iterator<ItemStack> {

    private var idx = 0
    override fun hasNext() = idx < inventory.size()
    override fun next(): ItemStack = inventory.getStack(idx++)
}

operator fun InventoryStorage.get(slot: Int): SingleSlotStorage<ItemVariant> =
    slots[slot]
