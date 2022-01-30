package dev.adirelle.adicrate.network

import dev.adirelle.adicrate.AdiCrate
import dev.adirelle.adicrate.Crate
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

data class PlayerExtractPacket(
    val player: PlayerEntity,
    val pos: BlockPos,
    val fullStack: Boolean
) {

    companion object {

        val ID = Identifier(AdiCrate.MOD_ID, "player_extract")

        fun fromPacket(player: ServerPlayerEntity, buf: PacketByteBuf) =
            with(buf) {
                PlayerExtractPacket(
                    player,
                    readBlockPos(),
                    readBoolean()
                )
            }
    }

    fun send() {
        val buf = toPacket(PacketByteBufs.create())
        ClientPlayNetworking.send(ID, buf)
    }

    private fun toPacket(buf: PacketByteBuf): PacketByteBuf =
        buf.apply {
            writeBlockPos(pos)
            writeBoolean(fullStack)
        }

    fun dispatch() {
        player.world
            .getBlockEntity(pos, Crate.BLOCK_ENTITY_TYPE)
            .ifPresent { blockEntity ->
                blockEntity.extractFor(player, fullStack)
            }
    }

    fun interface Receiver {

        fun extractFor(player: PlayerEntity, fullStack: Boolean)
    }
}
