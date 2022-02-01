package dev.adirelle.adicrate.utils.extensions

import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.math.BlockPos
import net.minecraft.world.WorldAccess
import java.util.*

inline fun <T : BlockEntity> WorldAccess.withBlockEntity(
    pos: BlockPos,
    type: BlockEntityType<T>,
    block: (T) -> Unit
) {
    if (isClient) return
    val be = getBlockEntity(pos, type)
    if (be.isEmpty) return
    block(be.get())
}

inline fun <T : BlockEntity, R> WorldAccess.withBlockEntity(
    pos: BlockPos,
    type: BlockEntityType<T>,
    crossinline block: (T) -> R
): Optional<R> =
    if (isClient) Optional.empty()
    else getBlockEntity(pos, type).map { block(it) }
