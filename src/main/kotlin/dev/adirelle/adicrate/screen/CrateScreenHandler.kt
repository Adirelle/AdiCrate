@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.screen

import dev.adirelle.adicrate.Crate
import dev.adirelle.adicrate.block.entity.internal.InventoryAdapter
import dev.adirelle.adicrate.block.entity.internal.InventoryAdapter.Companion.INPUT_SLOT
import dev.adirelle.adicrate.block.entity.internal.InventoryAdapter.Companion.OUTPUT_SLOT
import dev.adirelle.adicrate.block.entity.internal.SidedInventoryAdapter
import dev.adirelle.adicrate.block.entity.internal.UpgradeInventory
import dev.adirelle.adicrate.utils.logger
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SidedInventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.math.Direction.NORTH
import java.util.function.Predicate

class CrateScreenHandler private constructor(
    syncId: Int,
    playerInventory: PlayerInventory,
    contentInventory: SidedInventory,
    upgradeInventory: Inventory,
    private val storage: SingleSlotStorage<ItemVariant>?
) :
    SyncedGuiDescription(
        Crate.SCREEN_HANDLER_TYPE,
        syncId,
        playerInventory,
        contentInventory,
        null
    ) {

    // Client-side constructor
    constructor(
        syncId: Int,
        playerInventory: PlayerInventory,
    ) : this(
        syncId,
        playerInventory,
        SidedInventoryAdapter(SimpleInventory(InventoryAdapter.SIZE)),
        UpgradeInventory(),
        null
    )

    // Server-side constructor
    constructor(
        syncId: Int,
        playerInventory: PlayerInventory,
        storage: SingleSlotStorage<ItemVariant>,
        upgradeInventory: Inventory
    ) : this(
        syncId,
        playerInventory,
        InventoryAdapter(storage),
        upgradeInventory,
        storage
    )

    private val LOGGER by logger

    init {
        val root = rootPanel as? WGridPanel
            ?: throw IllegalStateException("unsupported panel class: ${rootPanel::class.java.name}")

        val inputSlot = WItemSlot.of(blockInventory, INPUT_SLOT)
        inputSlot.isTakingAllowed = false
        inputSlot.filter = Predicate { contentInventory.canInsert(INPUT_SLOT, it, NORTH) }
        root.add(inputSlot, 3, 1)

        val outputSlot = WItemSlot.of(blockInventory, OUTPUT_SLOT)
        outputSlot.filter = Predicate { contentInventory.canExtract(OUTPUT_SLOT, it, NORTH) }
        outputSlot.isInsertingAllowed = false
        root.add(outputSlot, 5, 1)

        val upgradeSlots = WItemSlot.of(upgradeInventory, 0, upgradeInventory.size(), 1)
        upgradeSlots.filter = Predicate { upgradeInventory.isValid(0, it) }
        root.add(upgradeSlots, 2, 3)

        val playerSlots = createPlayerInventoryPanel(true)
        root.add(playerSlots, 0, 4)

        root.validate(this)
    }

    override fun transferSlot(player: PlayerEntity, index: Int): ItemStack {
        val slot = slots[index]
        return if (storage != null && slot.inventory === blockInventory && slot.index == OUTPUT_SLOT)
            transferToPlayerInventory(storage)
        else
            super.transferSlot(player, index)
    }

    private fun transferToPlayerInventory(storage: SingleSlotStorage<ItemVariant>): ItemStack {
        Transaction.openOuter().use { tx ->
            val inserted = PlayerInventoryStorage
                .of(playerInventory)
                .offer(storage.resource, storage.amount, tx)
            val extracted = storage.extract(storage.resource, inserted, tx)
            if (extracted == inserted) {
                tx.commit()
                sendContentUpdates()
            } else {
                tx.abort()
            }
        }
        return ItemStack.EMPTY
    }
}
