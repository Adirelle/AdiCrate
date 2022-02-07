@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.abstraction

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.minecraft.util.math.BlockPos

interface Network {

    val info: Info

    fun add(node: Node)
    fun remove(node: Node)
    fun destroy()

    interface Node {

        val storage: Storage
        val networkInfo: Info?

        fun getPos(): BlockPos

        fun connectTo(network: Network): Boolean
        fun connectWith(node: Node): Boolean

        fun disconnect()
    }

    interface Storage : SingleSlotStorage<ItemVariant> {

        val realCapacity: Long

        fun canInsert(resource: ItemVariant): Boolean
        fun canExtract(resource: ItemVariant): Boolean

        fun isJammed(): Boolean
    }

    interface Info {

        val name: String
    }
}
