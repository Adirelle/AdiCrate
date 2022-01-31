package dev.adirelle.adicrate.block.entity.internal

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

data class Upgrade(
    val capacity: Int = 0,
    val lock: Boolean = false,
    val void: Boolean = false
) {

    companion object {

        private val upgradeItems = mapOf<Item, Upgrade>(
            Items.LAVA_BUCKET to Upgrade(void = true),
            Items.SLIME_BALL to Upgrade(lock = true),
            Items.IRON_INGOT to Upgrade(capacity = 1),
            Items.DIAMOND to Upgrade(capacity = 2),
            Items.NETHERITE_INGOT to Upgrade(capacity = 3)
        )

        val EMPTY = Upgrade()

        fun of(stack: ItemStack): Upgrade? =
            upgradeItems[stack.item]?.times(stack.count)

        fun isValid(stack: ItemStack) =
            stack.item in upgradeItems
    }

    operator fun plus(other: Upgrade) =
        Upgrade(capacity + other.capacity, lock || other.lock, void || other.void)

    operator fun times(factor: Int) =
        Upgrade(capacity * factor, lock, void)
}
