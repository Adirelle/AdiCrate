@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.utils.extensions

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound

fun ItemStack.toNbt() =
    NbtCompound().also { writeNbt(it) }

