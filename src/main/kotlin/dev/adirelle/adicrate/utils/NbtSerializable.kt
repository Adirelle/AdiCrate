package dev.adirelle.adicrate.utils

import net.minecraft.nbt.NbtCompound

interface NbtSerializable {

    fun readFromNbt(nbt: NbtCompound) {}
    fun writeToNbt(nbt: NbtCompound) {}
}
