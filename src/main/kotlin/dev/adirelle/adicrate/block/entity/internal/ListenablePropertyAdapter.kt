package dev.adirelle.adicrate.block.entity.internal

import net.minecraft.screen.ArrayPropertyDelegate

class ListenablePropertyAdapter(size: Int) : ArrayPropertyDelegate(size) {

    var callback: (Int, Int, Int) -> Unit = { _, _, _ -> }

    override fun set(index: Int, value: Int) {
        val oldValue = get(index)
        if (oldValue != value) {
            super.set(index, value)
            callback(index, oldValue, value)
        }
    }
}
