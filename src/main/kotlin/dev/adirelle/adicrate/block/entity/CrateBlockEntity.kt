@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.block.entity

import dev.adirelle.adicrate.Crate
import dev.adirelle.adicrate.block.entity.internal.CrateStorage
import dev.adirelle.adicrate.block.entity.internal.CrateUpgrade
import dev.adirelle.adicrate.utils.extensions.iterator
import dev.adirelle.adicrate.utils.extensions.stackSize
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.Packet
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.state.property.Properties
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

class CrateBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(Crate.BLOCK_ENTITY_TYPE, pos, state),
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
            markDirty()
            updateUpgrades(false)
        }
    }

    private fun updateUpgrades(onLoad: Boolean) {
        val newUpgrade = upgradeInventory.iterator().asSequence()
            .mapNotNull(CrateUpgrade::of)
            .fold(CrateUpgrade.EMPTY, CrateUpgrade::plus)
        if (newUpgrade != _storage.upgrade) {
            _storage.upgrade = newUpgrade
            if (!onLoad) {
                markDirty()
            }
        }
    }

    override fun toInitialChunkDataNbt() =
        createNbt()

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener>? =
        BlockEntityUpdateS2CPacket.create(this)

    override fun readNbt(nbt: NbtCompound) {
        _storage.readNbt(nbt)
        upgradeInventory.readNbtList(nbt.getList("upgrades", NbtType.COMPOUND))
        updateUpgrades(true)
    }

    override fun writeNbt(nbt: NbtCompound) {
        _storage.writeNbt(nbt)
        nbt.put("upgrades", upgradeInventory.toNbtList())
    }

    override fun onContentUpdated() {
        markDirty()
        sendContentUpdate()
    }

    private fun sendContentUpdate() {
        if (world?.isClient != false) return
        val players = PlayerLookup.tracking(this)
        if (players.isEmpty()) return
        val packet = this.toUpdatePacket()
        players.forEach { player ->
            player.networkHandler.sendPacket(packet)
        }
    }

    fun extractFor(player: PlayerEntity, fullStack: Boolean) {
        Transaction.openOuter().use { tx ->
            val playerStorage = PlayerInventoryStorage.of(player)
            val resource = _storage.resource
            val amount = if (fullStack) resource.stackSize else 1L
            val extracted = _storage.extract(resource, amount, tx)
            playerStorage.offerOrDrop(resource, extracted, tx)
            tx.commit()
        }
    }

    fun insertFrom(player: PlayerEntity, hand: Hand): Boolean {
        if (player.getStackInHand(hand).isEmpty) {
            if (storage.isResourceBlank) {
                return false
            }
            val playerInventory = PlayerInventoryStorage.of(player)
            return 0 < StorageUtil.move(
                playerInventory,
                storage,
                { it == storage.resource },
                storage.capacity - storage.amount,
                null
            )
        }

        val playerHand = ContainerItemContext.ofPlayerHand(player, hand).mainSlot
        return 0 < StorageUtil.move(playerHand, storage, { true }, playerHand.amount, null)
    }
}
