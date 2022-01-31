@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.block.entity.internal

import dev.adirelle.adicrate.utils.logger
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.math.Direction
import kotlin.math.min

class InventoryAdapter(
    private val backing: SingleSlotStorage<ItemVariant>
) : SidedInventory {

    companion object {

        const val SIZE = 2

        const val INPUT_SLOT = 0
        const val OUTPUT_SLOT = 1

        private val availableSlots = intArrayOf(INPUT_SLOT, OUTPUT_SLOT)
    }

    private val LOGGER by logger

    private val resource by backing::resource
    private val amount by backing::amount

    override fun clear() {
        Transaction.openOuter().use { tx ->
            backing.extract(resource, amount, tx)
            tx.commit()
        }
    }

    override fun size() = 1

    override fun isEmpty() = amount == 0L

    override fun getStack(slot: Int): ItemStack =
        when (slot) {
            INPUT_SLOT  -> ItemStack.EMPTY
            OUTPUT_SLOT -> resource.toStack(min(amount.toInt(), resource.item.maxCount))
            else        -> throw IndexOutOfBoundsException()
        }

    override fun removeStack(slot: Int, amount: Int): ItemStack =
        when (slot) {
            INPUT_SLOT  -> ItemStack.EMPTY
            OUTPUT_SLOT -> removeStackInternal(amount)
            else        -> throw IndexOutOfBoundsException()
        }

    private fun removeStackInternal(amount: Int) =
        Transaction.openOuter().use { tx ->
            val stack = resource.toStack()
            stack.count = backing.extract(resource, amount.toLong(), tx).toInt()
            tx.commit()
            LOGGER.info("removed {}", stack)
            stack
        }

    override fun removeStack(slot: Int) =
        removeStack(slot, resource.item.maxCount)

    override fun setStack(slot: Int, stack: ItemStack) {
        when (slot) {
            INPUT_SLOT  -> setStackInternal(stack)
            OUTPUT_SLOT -> Unit
            else        -> throw IndexOutOfBoundsException()
        }
    }

    private fun setStackInternal(stack: ItemStack) {
        Transaction.openOuter().use { tx ->
            val inserted = backing.insert(ItemVariant.of(stack.item), stack.count.toLong(), tx)
            LOGGER.info("inserted {} {}", inserted, stack.item.toString())
            stack.decrement(inserted.toInt())
            tx.commit()
        }
    }

    override fun markDirty() {}

    override fun canPlayerUse(player: PlayerEntity) =
        !player.isSpectator && player.canModifyBlocks()

    override fun isValid(slot: Int, stack: ItemStack) =
        when (slot) {
            INPUT_SLOT  -> canInsert(slot, stack, null)
            OUTPUT_SLOT -> canExtract(slot, stack, Direction.UP)
            else        -> throw IndexOutOfBoundsException()
        }

    override fun getAvailableSlots(side: Direction?) =
        Companion.availableSlots

    override fun canInsert(slot: Int, stack: ItemStack, dir: Direction?) =
        when (slot) {
            INPUT_SLOT  -> backing.simulateInsert(
                ItemVariant.of(stack),
                stack.count.toLong(),
                null
            ) == stack.count.toLong()
            OUTPUT_SLOT -> false
            else        -> throw IndexOutOfBoundsException()
        }

    override fun canExtract(slot: Int, stack: ItemStack, dir: Direction) =
        when (slot) {
            INPUT_SLOT  -> false
            OUTPUT_SLOT -> backing.simulateExtract(
                ItemVariant.of(stack),
                stack.count.toLong(),
                null
            ) == stack.count.toLong()
            else        -> throw IndexOutOfBoundsException()
        }
}
