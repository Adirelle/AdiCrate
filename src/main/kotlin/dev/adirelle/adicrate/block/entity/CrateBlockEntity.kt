@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.block.entity

import dev.adirelle.adicrate.Crate
import dev.adirelle.adicrate.block.entity.internal.CrateStorage
import dev.adirelle.adicrate.block.entity.internal.Upgrade
import dev.adirelle.adicrate.block.entity.internal.UpgradeInventory
import dev.adirelle.adicrate.screen.CrateScreenHandler
import dev.adirelle.adicrate.utils.extensions.iterator
import dev.adirelle.adicrate.utils.extensions.set
import dev.adirelle.adicrate.utils.extensions.stackSize
import dev.adirelle.adicrate.utils.logger
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
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.Packet
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.state.property.Properties
import net.minecraft.text.TranslatableText
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

class CrateBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(Crate.BLOCK_ENTITY_TYPE, pos, state),
    NamedScreenHandlerFactory,
    CrateStorage.Listener {

    companion object {

        private val UPGRADE_NBT_KEY = "upgrades"

        const val UPGRADE_COUNT = 5
    }

    private val LOGGER by logger

    private var _storage = CrateStorage(this)

    val storage: SingleSlotStorage<ItemVariant> by ::_storage

    val facing: Direction
        get() = cachedState.get(Properties.HORIZONTAL_FACING)

    val upgradeInventory: SimpleInventory = UpgradeInventory().also {
        it.addListener {
            if (world?.isClient == false) {
                updateUpgrades(false)
                markDirty()
            }
        }
    }

    private fun updateUpgrades(onLoad: Boolean) {
        val newUpgrade = upgradeInventory.iterator().asSequence()
            .mapNotNull(Upgrade::of)
            .fold(Upgrade.EMPTY, Upgrade::plus)
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
        upgradeInventory.readNbtList(nbt.getList(UPGRADE_NBT_KEY, NbtType.COMPOUND))
        updateUpgrades(true)
    }

    override fun writeNbt(nbt: NbtCompound) {
        _storage.writeNbt(nbt)
        nbt[UPGRADE_NBT_KEY] = upgradeInventory.toNbtList()
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

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity): ScreenHandler =
        CrateScreenHandler(syncId, playerInventory, _storage, upgradeInventory)

    override fun getDisplayName() =
        TranslatableText("block.adicrate.crate")

    fun extractFor(player: PlayerEntity, fullStack: Boolean): Long =
        if (_storage.isResourceBlank || _storage.amount == 0L)
            0L
        else
            Transaction.openOuter().use { tx ->
                val playerStorage = PlayerInventoryStorage.of(player)
                val resource = _storage.resource
                val amount = if (fullStack) resource.stackSize else 1L
                val extracted = _storage.extract(resource, amount, tx)
                LOGGER.info("extracting {} {} for player", extracted, resource.item.toString())
                playerStorage.offerOrDrop(resource, extracted, tx)
                tx.commit()
                extracted
            }

    fun insertFrom(player: PlayerEntity, hand: Hand): Long =
        when {
            !player.getStackInHand(hand).isEmpty -> insertHandfulFrom(player, hand)
            storage.isResourceBlank              -> 0L
            player.isSneaking                    -> insertAllFrom(player)
            else                                 -> 0L
        }

    private fun insertHandfulFrom(player: PlayerEntity, hand: Hand): Long =
        ContainerItemContext.ofPlayerHand(player, hand).mainSlot.let { playerHand ->
            val moved = StorageUtil.move(playerHand, storage, { true }, playerHand.amount, null)
            LOGGER.info("moved {} {} from player hand", moved, storage.resource.item.toString())
            moved
        }

    private fun insertAllFrom(player: PlayerEntity): Long {
        val playerInventory = PlayerInventoryStorage.of(player)
        val moved = StorageUtil.move(
            playerInventory,
            storage,
            { it == storage.resource },
            storage.capacity - storage.amount,
            null
        )
        LOGGER.info("moved {} {} from player inventory", moved, storage.resource.item.toString())
        return moved
    }
}
