@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.block.entity

import dev.adirelle.adicrate.Crate
import dev.adirelle.adicrate.abstraction.FrontInteractionHandler
import dev.adirelle.adicrate.abstraction.Network
import dev.adirelle.adicrate.abstraction.Network.Info
import dev.adirelle.adicrate.abstraction.Network.Node
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
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
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
    CrateStorage.Listener,
    Node,
    FrontInteractionHandler {

    companion object {

        private val UPGRADE_NBT_KEY = "upgrades"

        const val UPGRADE_COUNT = 5
    }

    private val LOGGER by logger

    private var internalStorage = CrateStorage(this)

    private var network: Network? = null

    override val networkInfo: Info?
        get() = network?.info

    override val storage: Network.Storage by ::internalStorage

    override val facing: Direction
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
        if (newUpgrade != internalStorage.upgrade) {
            internalStorage.upgrade = newUpgrade
            if (!onLoad) {
                markDirty()
            }
        }
    }

    override fun connectTo(network: Network): Boolean {
        if (network === this.network || !network.accept(this)) return false
        this.disconnect()
        LOGGER.debug("connecting to $network")
        this.network = network
        network.add(this)
        return true
    }

    override fun connectWith(node: Node) =
        network?.let(node::connectTo) ?: false

    override fun disconnect() {
        val oldNetwork = network ?: return
        LOGGER.debug("disconnecting from $oldNetwork")
        network = null
        oldNetwork.remove(this)
    }

    override fun toInitialChunkDataNbt() =
        createNbt()

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener>? =
        BlockEntityUpdateS2CPacket.create(this)

    override fun readNbt(nbt: NbtCompound) {
        internalStorage.readNbt(nbt)
        upgradeInventory.readNbtList(nbt.getList(UPGRADE_NBT_KEY, NbtType.COMPOUND))
        updateUpgrades(true)
    }

    override fun writeNbt(nbt: NbtCompound) {
        internalStorage.writeNbt(nbt)
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
        CrateScreenHandler(syncId, playerInventory, internalStorage, upgradeInventory)

    override fun getDisplayName() =
        TranslatableText("block.adicrate.crate")

    override fun pullItems(player: PlayerEntity) {
        if (internalStorage.isResourceBlank || internalStorage.amount == 0L) return
        Transaction.openOuter().use { tx ->
            val playerStorage = PlayerInventoryStorage.of(player)
            val resource = internalStorage.resource
            val amount = if (!player.isSneaking) resource.stackSize else 1L
            val extracted = internalStorage.extract(resource, amount, tx)
            LOGGER.debug("extracting {} {} for player", extracted, resource.item.toString())
            playerStorage.offerOrDrop(resource, extracted, tx)
            tx.commit()
        }
    }

    override fun pushItems(player: PlayerEntity, hand: Hand) {
        if (player.isSneaking || player.getStackInHand(hand).isEmpty) {
            insertAllFromInventory(PlayerInventoryStorage.of(player))
        } else {
            insertFromSlot(ContainerItemContext.ofPlayerHand(player, hand).mainSlot)
        }
    }

    private fun insertFromSlot(slot: SingleSlotStorage<ItemVariant>) {
        val moved = StorageUtil.move(slot, storage, { true }, slot.amount, null)
        LOGGER.debug("moved {} {} from player hand", moved, storage.resource.item.toString())
    }

    private fun insertAllFromInventory(inventory: Storage<ItemVariant>) {
        val maxAmount = storage.capacity - storage.amount
        val moved = StorageUtil.move(inventory, storage, { it == storage.resource }, maxAmount, null)
        LOGGER.info("moved {} {} from player inventory", moved, storage.resource.item.toString())
    }
}
