@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.block.entity.internal

import com.google.common.math.LongMath
import dev.adirelle.adicrate.utils.extensions.stackSize
import dev.adirelle.adicrate.utils.logger
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleViewIterator
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import kotlin.math.min

class CrateStorage(private val listener: Listener) :
    SingleSlotStorage<ItemVariant>,
    SnapshotParticipant<ResourceAmount<ItemVariant>>() {

    private val LOGGER by logger

    var upgrade = Upgrade()

    private var resourceInternal: ItemVariant = ItemVariant.blank()
    private var amountInternal: Long = 0L

    private val capacityInternal: Long
        get() = resourceInternal.stackSize * LongMath.pow(2, 4 + upgrade.capacity)

    private fun isOverflowed() = amountInternal > capacityInternal

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
        val inserted = min(maxAmount, capacityInternal - amountInternal)
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
        LOGGER.info("extracted {} {}s", extracted, resourceInternal.item.toString())
        if (amountInternal == 0L && !upgrade.lock) {
            resourceInternal = ItemVariant.blank()
        }
        return extracted
    }

    fun extract(maxAmount: Long, tx: TransactionContext) =
        extract(resourceInternal, maxAmount, tx)

    fun toStack(): ItemStack =
        toStack(amountInternal)

    fun toStack(count: Long): ItemStack =
        toStack(count.toInt())

    fun toStack(count: Int): ItemStack =
        resource.toStack(min(min(count, amountInternal.toInt()), resourceInternal.item.maxCount))

    override fun iterator(tx: TransactionContext): MutableIterator<StorageView<ItemVariant>> =
        SingleViewIterator.create(this, tx)

    override fun isResourceBlank() =
        resource.isBlank

    override fun getResource() =
        resourceInternal

    override fun getAmount() =
        amountInternal

    override fun getCapacity() =
        if (upgrade.void) Long.MAX_VALUE else capacityInternal

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
        resourceInternal = ItemVariant.fromNbt(nbt.getCompound("resource"))
        amountInternal = nbt.getLong("amount")
        LOGGER.info("readFromNbt: {}, {}", resourceInternal, amountInternal)
    }

    fun writeNbt(nbt: NbtCompound) {
        nbt.put("resource", resourceInternal.toNbt())
        nbt.putLong("amount", amountInternal)
    }

    fun interface Listener {

        fun onContentUpdated()
    }
}
