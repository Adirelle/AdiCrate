package dev.adirelle.adicrate.block.entity.internal

import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.math.Direction

class SidedInventoryAdapter(private val backing: Inventory) : SidedInventory, Inventory by backing {

    private val cachedAvailableSlots = IntArray(backing.size()) { it }

    override fun getAvailableSlots(side: Direction) = cachedAvailableSlots

    override fun canInsert(slot: Int, stack: ItemStack, dir: Direction?) =
        isValid(slot, stack)

    override fun canExtract(slot: Int, stack: ItemStack, dir: Direction) =
        isValid(slot, stack)
}
