@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.utils.extensions

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant

val ItemVariant.stackSize: Long
    get() = item.maxCount.toLong()
