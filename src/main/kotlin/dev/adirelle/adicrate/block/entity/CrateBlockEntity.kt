@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.block.entity

import dev.adirelle.adicrate.Crate
import dev.adirelle.adicrate.block.entity.internal.CrateStorage
import dev.adirelle.adicrate.block.entity.internal.CrateUpgrade
import dev.adirelle.adicrate.network.ContentUpdatePacket
import dev.adirelle.adicrate.network.PlayerExtractPacket
import dev.adirelle.adicrate.utils.extensions.iterator
import dev.adirelle.adicrate.utils.extensions.stackSize
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

class CrateBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(Crate.BLOCK_ENTITY_TYPE, pos, state),
    ContentUpdatePacket.Receiver,
    PlayerExtractPacket.Receiver,
    CrateStorage.Listener {

    companion object {

        const val UPGRADE_COUNT = 5
    }

    private var _storage = CrateStorage(this)

    val storage: SingleSlotStorage<ItemVariant> by ::_storage

    val facing: Direction
        get() = cachedState.get(Properties.FACING)

    val upgradeInventory: SimpleInventory = object : SimpleInventory(UPGRADE_COUNT) {
        override fun getMaxCountPerStack() = 1
        override fun isValid(slot: Int, stack: ItemStack) = CrateUpgrade.isValid(stack)
    }.also {
        it.addListener {
            updateUpgrades()
            markDirty()
        }
    }

    private fun updateUpgrades() {
        upgradeInventory.iterator().asSequence()
            .mapNotNull(CrateUpgrade::of)
            .fold(CrateUpgrade.EMPTY, CrateUpgrade::plus)
    }

    override fun readNbt(nbt: NbtCompound) {
        _storage.readNbt(nbt)
        upgradeInventory.readNbtList(nbt.getList("upgrades", NbtType.COMPOUND))
        updateUpgrades()
    }

    override fun writeNbt(nbt: NbtCompound) {
        _storage.writeNbt(nbt)
        nbt.put("upgrades", upgradeInventory.toNbtList())
    }

    override fun onContentUpdated() {
        sendContentUpdate()
    }

    override fun setWorld(world: World) {
        super.setWorld(world)
        sendContentUpdate()
    }

    override fun onContentUpdateReceived(resource: ItemVariant, amount: Long) {
        _storage.onContentUpdateReceived(resource, amount)
    }

    private fun sendContentUpdate() {
        (world as? ServerWorld)?.let { world ->
            _storage.createContentUpdatePacket(world, pos)
                .send(world)
        }
    }

    override fun extractFor(player: PlayerEntity, fullStack: Boolean) {
        Transaction.openOuter().use { tx ->
            val playerStorage = PlayerInventoryStorage.of(player)
            val resource = _storage.resource
            val amount = if (fullStack) resource.stackSize else 1L
            val extracted = _storage.extract(resource, amount, tx)
            playerStorage.offerOrDrop(resource, extracted, tx)
            tx.commit()
        }
    }
}
