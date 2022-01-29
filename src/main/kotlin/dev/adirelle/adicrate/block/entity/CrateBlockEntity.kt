@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.block.entity

import com.google.common.math.LongMath
import dev.adirelle.adicrate.Crate
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleViewIterator
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.BlockPos
import kotlin.math.max
import kotlin.math.min

class CrateBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(Crate.BLOCK_ENTITY, pos, state) {

    companion object {

        const val UPGRADE_COUNT = 5

        private val upgradingItems = mapOf<Item, Upgrade>(
            Items.LAVA_BUCKET to Upgrade(void = true),
            Items.SLIME_BALL to Upgrade(lock = true),
            Items.IRON_INGOT to Upgrade(capacity = 1),
            Items.DIAMOND to Upgrade(capacity = 2),
            Items.NETHERITE_INGOT to Upgrade(capacity = 3)
        )
    }

    private val _storage = StorageAdapter()
    val storage: Storage<ItemVariant> by ::_storage

    private var upgrade = Upgrade()

    val upgradeInventory: SimpleInventory = object : SimpleInventory(UPGRADE_COUNT) {
        override fun getMaxCountPerStack() = 1
        override fun isValid(slot: Int, stack: ItemStack) = stack.item in upgradingItems
    }.also {
        it.addListener {
            updateUpgrades()
            markDirty()
        }
    }

    private fun updateUpgrades() {
        upgrade = (0 until upgradeInventory.size())
            .map(upgradeInventory::getStack)
            .mapNotNull { upgradingItems[it.item] }
            .fold(Upgrade(), Upgrade::plus)
    }

    override fun readNbt(nbt: NbtCompound) {
        _storage._resource = ItemVariant.fromNbt(nbt.getCompound("resource"))
        _storage._amount = nbt.getLong("amount")
        upgradeInventory.readNbtList(nbt.getList("upgrades", NbtType.COMPOUND))
        updateUpgrades()
    }

    override fun writeNbt(nbt: NbtCompound) {
        nbt.put("resource", _storage._resource.toNbt())
        nbt.putLong("amount", _storage._amount)
        nbt.put("upgrades", upgradeInventory.toNbtList())
    }

    data class Upgrade(
        val capacity: Int = 0,
        val lock: Boolean = false,
        val void: Boolean = false
    ) {

        operator fun plus(other: Upgrade) =
            Upgrade(capacity + other.capacity, lock || other.lock, void || other.void)
    }

    private inner class StorageAdapter :
        Storage<ItemVariant>,
        StorageView<ItemVariant>,
        SnapshotParticipant<ResourceAmount<ItemVariant>>() {

        var _resource: ItemVariant = ItemVariant.blank()
        var _amount: Long = 0L

        private fun isOverflowed() = _amount > capacity

        override fun insert(resource: ItemVariant, maxAmount: Long, tx: TransactionContext): Long {
            if (resource.isBlank || maxAmount <= 0 || isOverflowed() || !canCombineInto(resource, _resource)) return 0L
            val destroyed = if (upgrade.void) max(0, _amount + maxAmount - capacity) else 0L
            val inserted = min(maxAmount, _amount + maxAmount)
            if (inserted <= 0) return destroyed
            updateSnapshots(tx)
            _amount += inserted
            return inserted + destroyed
        }

        private fun canCombineInto(inserted: ItemVariant, existing: ItemVariant) =
            existing.isBlank || inserted.isOf(existing.`object`) && inserted.nbtMatches(existing.nbt) && inserted.item.maxCount > 1

        override fun extract(resource: ItemVariant, maxAmount: Long, tx: TransactionContext): Long {
            val extracted = min(_amount, maxAmount)
            if (resource.isBlank || _resource.isBlank || resource != _resource || extracted <= 0 || isOverflowed()) return 0L
            updateSnapshots(tx)
            _amount -= extracted
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
            markDirty()
        }
    }
}
