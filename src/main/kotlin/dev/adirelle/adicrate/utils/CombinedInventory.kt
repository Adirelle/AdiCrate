package dev.adirelle.adicrate.utils

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

class CombinedInventory(private val inventories: Collection<Inventory>) : Inventory {

    private val slotToInventory = buildList {
        inventories.forEach { inventory ->
            for (i in 0 until inventory.size()) {
                add(Pair(inventory, i))
            }
        }
    }

    private inline fun <T> withSlot(index: Int, block: Inventory.(Int) -> T): T {
        val (inv, invIndex) = slotToInventory[index]
        return inv.block(invIndex)
    }

    override fun clear() {
        inventories.forEach(Inventory::clear)
    }

    override fun size() = slotToInventory.size
    override fun isEmpty() = inventories.all(Inventory::isEmpty)
    override fun getStack(slot: Int): ItemStack = withSlot(slot, Inventory::getStack)
    override fun removeStack(slot: Int): ItemStack = withSlot(slot, Inventory::removeStack)

    override fun removeStack(slot: Int, amount: Int): ItemStack =
        withSlot(slot) { removeStack(it, amount) }

    override fun setStack(slot: Int, stack: ItemStack) {
        withSlot(slot) { setStack(it, stack) }
    }

    override fun markDirty() {
        inventories.forEach(Inventory::markDirty)
    }

    override fun canPlayerUse(player: PlayerEntity): Boolean =
        inventories.any { it.canPlayerUse(player) }
}
