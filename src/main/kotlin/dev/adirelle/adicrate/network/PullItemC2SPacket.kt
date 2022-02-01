package dev.adirelle.adicrate.network

import dev.adirelle.adicrate.AdiCrate
import dev.adirelle.adicrate.block.AbstractContainerBlock
import dev.adirelle.adicrate.utils.logger
import net.fabricmc.api.EnvType.CLIENT
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

object PullItemC2SPacket : ServerPlayNetworking.PlayChannelHandler {

    private val LOGGER by logger

    private val ID = Identifier(AdiCrate.MOD_ID, "pull_items")

    fun registerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(ID, this)
    }

    @Environment(CLIENT)
    fun send(player: PlayerEntity, pos: BlockPos) {
        val buf = PacketByteBufs.create().apply {
            writeIdentifier(player.world.registryKey.value)
            writeBlockPos(pos)
        }
        ClientPlayNetworking.send(ID, buf)
    }

    override fun receive(
        server: MinecraftServer,
        player: ServerPlayerEntity,
        handler: ServerPlayNetworkHandler,
        buf: PacketByteBuf,
        responseSender: PacketSender
    ) {
        val world = player.world
        val worldId = buf.readIdentifier()
        if (worldId != world.registryKey.value) {
            LOGGER.warn("ignoring packet for wrong world")
            return
        }
        val pos = buf.readBlockPos()
        val block = world.getBlockState(pos).block
        if (block !is AbstractContainerBlock) {
            LOGGER.warn("ignoring packet for wrong block")
            return
        }
        server.execute {
            block.pullItems(world, pos, player)
        }
    }
}
