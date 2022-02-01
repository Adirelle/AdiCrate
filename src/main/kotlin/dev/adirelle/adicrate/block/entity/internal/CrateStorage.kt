@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.block.entity.internal

import dev.adirelle.adicrate.utils.extensions.set
import dev.adirelle.adicrate.utils.extensions.stackSize
import dev.adirelle.adicrate.utils.logger
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleViewIterator
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*
import kotlin.math.min

class CrateStorage(private val listener: Listener) :
    SingleSlotStorage<ItemVariant>,
    SnapshotParticipant<ResourceAmount<ItemVariant>>() {

    private val LOGGER by logger

    companion object {

        private const val RESOURCE_NBT_KEY = "resource"
        private const val AMOUNT_NBT_KEY = "amount"
        private const val CAPACITY_NBT_KEY = "capacity"

        fun contentText(amount: Long, capacity: Long): Text {
            val text = LiteralText("")
            val amountText = LiteralText(amount.toString())
            if (amount > capacity) {
                text.append(amountText.formatted(Formatting.RED))
            } else {
                text.append(amountText)
            }
            text.append("/${capacity}")
            return text
        }

        fun contentText(amount: Int, capacity: Int) =
            contentText(amount.toLong(), capacity.toLong())

        fun contentText(nbt: NbtCompound): Text =
            contentText(nbt.getLong(AMOUNT_NBT_KEY), nbt.getLong(CAPACITY_NBT_KEY))

        fun itemText(nbt: NbtCompound): Optional<Text> =
            Optional.of(nbt.getCompound(RESOURCE_NBT_KEY))
                .filter { !it.isEmpty }
                .map(ItemVariant::fromNbt)
                .filter { !it.isBlank }
                .map { it.item.name }
    }

    var upgrade = Upgrade()

    private var resourceInternal: ItemVariant = ItemVariant.blank()
    private var amountInternal: Long = 0L

    val realCapacity: Long
        get() = resourceInternal.stackSize * (16 * (1 + upgrade.capacity))

    private fun isOverflowed() = amountInternal > realCapacity

    override fun insert(resource: ItemVariant, maxAmount: Long, tx: TransactionContext) =
        when {
            maxAmount <= 0    -> 0
            isOverflowed()    -> 0
            !accept(resource) -> 0
            upgrade.void      -> insertOrDestroy(resource, maxAmount, tx)
            else              -> insertAtMost(resource, maxAmount, tx)
        }

    private fun insertOrDestroy(resource: ItemVariant, maxAmount: Long, tx: TransactionContext): Long {
        insertAtMost(resource, maxAmount, tx)
        return maxAmount
    }

    private fun insertAtMost(resource: ItemVariant, maxAmount: Long, tx: TransactionContext): Long {
        val inserted = min(maxAmount, realCapacity - amountInternal)
        if (inserted > 0) {
            updateSnapshots(tx)
            amountInternal += inserted
            resourceInternal = resource
        }
        return inserted
    }

    fun accept(resource: ItemVariant) =
        !resource.isBlank && (
            resourceInternal.isBlank || (
                resource.isOf(resourceInternal.`object`) && resource.nbtMatches(resourceInternal.nbt) && resource.item.maxCount > 1
                )
            )

    override fun extract(resource: ItemVariant, maxAmount: Long, tx: TransactionContext): Long {
        val extracted = min(amountInternal, maxAmount)
        if (resource.isBlank || resourceInternal.isBlank || resource != resourceInternal || extracted <= 0 || isOverflowed()) return 0L
        updateSnapshots(tx)
        amountInternal -= extracted
        if (amountInternal == 0L && !upgrade.lock) {
            resourceInternal = ItemVariant.blank()
        }
        return extracted
    }

    override fun iterator(tx: TransactionContext): MutableIterator<StorageView<ItemVariant>> =
        SingleViewIterator.create(this, tx)

    override fun isResourceBlank() =
        resource.isBlank

    override fun getResource() =
        resourceInternal

    override fun getAmount() =
        amountInternal

    override fun getCapacity() =
        if (upgrade.void) Long.MAX_VALUE else realCapacity

    override fun createSnapshot() =
        ResourceAmount(resourceInternal, amountInternal)

    override fun readSnapshot(snapshot: ResourceAmount<ItemVariant>) {
        resourceInternal = snapshot.resource
        amountInternal = snapshot.amount
    }

    override fun onFinalCommit() {
        listener.onContentUpdated()
    }

    fun readNbt(nbt: NbtCompound) {
        resourceInternal = ItemVariant.fromNbt(nbt.getCompound(RESOURCE_NBT_KEY))
        amountInternal = nbt.getLong(AMOUNT_NBT_KEY)
    }

    fun writeNbt(nbt: NbtCompound) {
        nbt[RESOURCE_NBT_KEY] = resourceInternal
        nbt[AMOUNT_NBT_KEY] = amountInternal
        nbt[CAPACITY_NBT_KEY] = realCapacity // used for tooltip/display
    }

    fun toText(): Text =
        contentText(amountInternal, realCapacity)

    fun interface Listener {

        fun onContentUpdated()
    }
}
