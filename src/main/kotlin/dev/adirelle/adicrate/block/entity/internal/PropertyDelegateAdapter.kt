package dev.adirelle.adicrate.block.entity.internal

import dev.adirelle.adicrate.abstraction.Network
import net.minecraft.screen.PropertyDelegate

class PropertyDelegateAdapter(private val backing: Network.Storage) : PropertyDelegate {

    companion object {

        const val AMOUNT_INDEX = 0
        const val CAPACITY_INDEX = 1

        const val SIZE = 2
    }

    override fun size() = SIZE
    override fun set(index: Int, value: Int) {} // NOOP

    override fun get(index: Int) =
        when (index) {
            AMOUNT_INDEX   -> backing.amount.toInt()
            CAPACITY_INDEX -> backing.realCapacity.toInt()
            else           -> throw IndexOutOfBoundsException("no property at index ${index}")
        }
}
