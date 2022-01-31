package dev.adirelle.adicrate.block.entity.internal

import dev.adirelle.adicrate.block.entity.CrateBlockEntity
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack

class UpgradeInventory : SimpleInventory(CrateBlockEntity.UPGRADE_COUNT) {

    override fun getMaxCountPerStack() =
        1

    override fun isValid(slot: Int, stack: ItemStack) =
        Upgrade.isValid(stack)
}
