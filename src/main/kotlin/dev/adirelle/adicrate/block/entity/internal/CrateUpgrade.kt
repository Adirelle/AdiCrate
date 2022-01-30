package dev.adirelle.adicrate.block.entity.internal

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

data class CrateUpgrade(
    val capacity: Int = 0,
    val lock: Boolean = false,
    val void: Boolean = false
) {

    companion object {

        private val upgradeItems = mapOf<Item, CrateUpgrade>(
            Items.LAVA_BUCKET to CrateUpgrade(void = true),
            Items.SLIME_BALL to CrateUpgrade(lock = true),
            Items.IRON_INGOT to CrateUpgrade(capacity = 1),
            Items.DIAMOND to CrateUpgrade(capacity = 2),
            Items.NETHERITE_INGOT to CrateUpgrade(capacity = 3)
        )

        val EMPTY = CrateUpgrade()

        fun of(stack: ItemStack): CrateUpgrade? =
            upgradeItems[stack.item]?.times(stack.count)

        fun isValid(stack: ItemStack) =
            stack.item in upgradeItems
    }

    operator fun plus(other: CrateUpgrade) =
        CrateUpgrade(capacity + other.capacity, lock || other.lock, void || other.void)

    operator fun times(factor: Int) =
        CrateUpgrade(capacity * factor, lock, void)

}
