@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.network

import dev.adirelle.adicrate.AdiCrate
import dev.adirelle.adicrate.Crate
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.client.world.ClientWorld
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

data class ContentUpdatePacket(
    val worldId: Identifier,
    val pos: BlockPos,
    val resource: ItemVariant,
    val amount: Long
) {

    companion object {

        val ID = Identifier(AdiCrate.MOD_ID, "content_update")

        fun create(world: ServerWorld, pos: BlockPos, resource: ItemVariant, amount: Long) =
            ContentUpdatePacket(world.registryKey.value, pos, resource, amount)

        fun fromPacket(buf: PacketByteBuf) =
            with(buf) {
                ContentUpdatePacket(
                    readIdentifier(),
                    readBlockPos(),
                    ItemVariant.fromPacket(buf),
                    readLong()
                )
            }
    }

    fun send(world: ServerWorld) {
        val buf = toPacket(PacketByteBufs.create())
        world.server.playerManager.playerList.forEach { player ->
            ServerPlayNetworking.send(player, ID, buf)
        }
    }

    private fun toPacket(buf: PacketByteBuf): PacketByteBuf =
        buf.apply {
            writeIdentifier(worldId)
            writeBlockPos(pos)
            resource.toPacket(this)
            writeLong(amount)
        }

    fun dispatch(world: ClientWorld) {
        if (world.registryKey.value != worldId) return
        world.getBlockEntity(pos, Crate.BLOCK_ENTITY_TYPE)
            .ifPresent { it.onContentUpdateReceived(resource, amount) }
    }

    fun interface Receiver {

        fun onContentUpdateReceived(resource: ItemVariant, amount: Long)
    }
}
