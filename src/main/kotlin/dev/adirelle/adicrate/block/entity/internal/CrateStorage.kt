@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.block.entity.internal

import com.google.common.math.LongMath
import dev.adirelle.adicrate.AdiCrate
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleViewIterator
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.nbt.NbtCompound
import kotlin.math.max
import kotlin.math.min

class CrateStorage(private val listener: Listener) :
    SingleSlotStorage<ItemVariant>,
    SnapshotParticipant<ResourceAmount<ItemVariant>>() {

    private val LOGGER = AdiCrate.LOGGER

    var upgrade = CrateUpgrade()

    @Suppress("PropertyName")
    private var _resource: ItemVariant = ItemVariant.blank()

    @Suppress("PropertyName")
    private var _amount: Long = 0L

    private fun isOverflowed() = _amount > capacity

    override fun insert(resource: ItemVariant, maxAmount: Long, tx: TransactionContext): Long {
        if (resource.isBlank || maxAmount <= 0 || isOverflowed() || !canCombineInto(resource, _resource)) return 0L
        val destroyed =
            if (upgrade.void) max(0, _amount + maxAmount - capacity) else 0L
        val inserted = min(maxAmount, _amount + maxAmount)
        if (inserted <= 0) return destroyed
        updateSnapshots(tx)
        if (_resource.isBlank) {
            _resource = resource
        }
        _amount += inserted
        LOGGER.info(
            "inserted {} {}s, destroyed {}",
            inserted,
            _resource.item.toString(),
            destroyed
        )
        return inserted + destroyed
    }

    private fun canCombineInto(inserted: ItemVariant, existing: ItemVariant) =
        existing.isBlank || inserted.isOf(existing.`object`) && inserted.nbtMatches(existing.nbt) && inserted.item.maxCount > 1

    override fun extract(resource: ItemVariant, maxAmount: Long, tx: TransactionContext): Long {
        val extracted = min(_amount, maxAmount)
        if (resource.isBlank || _resource.isBlank || resource != _resource || extracted <= 0 || isOverflowed()) return 0L
        updateSnapshots(tx)
        _amount -= extracted
        LOGGER.info("extracted {} {}s", extracted, _resource.item.toString())
        if (_amount == 0L && !upgrade.lock) {
            _resource = ItemVariant.blank()
        }
        return extracted
    }

    override fun iterator(tx: TransactionContext): MutableIterator<StorageView<ItemVariant>> =
        SingleViewIterator.create(this, tx)

    override fun isResourceBlank() =
        resource.isBlank

    override fun getResource() =
        _resource

    override fun getAmount() =
        _amount

    override fun getCapacity() =
        _resource.item.maxCount.toLong() * LongMath.pow(2, 4 + upgrade.capacity)

    override fun createSnapshot() =
        ResourceAmount(_resource, _amount)

    override fun readSnapshot(snapshot: ResourceAmount<ItemVariant>) {
        _resource = snapshot.resource
        _amount = snapshot.amount
    }

    override fun onFinalCommit() {
        listener.onContentUpdated()
    }

    fun readNbt(nbt: NbtCompound) {
        _resource = ItemVariant.fromNbt(nbt.getCompound("resource"))
        _amount = nbt.getLong("amount")
        LOGGER.info("readFromNbt: {}, {}", _resource, _amount)
    }

    fun writeNbt(nbt: NbtCompound) {
        nbt.put("resource", _resource.toNbt())
        nbt.putLong("amount", _amount)
    }

    fun interface Listener {

        fun onContentUpdated()
    }
}
