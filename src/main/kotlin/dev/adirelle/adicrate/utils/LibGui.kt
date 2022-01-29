package dev.adirelle.adicrate.utils.extensions

import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.networking.NetworkSide
import io.github.cottonmc.cotton.gui.networking.NetworkSide.CLIENT
import io.github.cottonmc.cotton.gui.networking.NetworkSide.SERVER
import io.github.cottonmc.cotton.gui.networking.ScreenNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier

fun NetworkSide.getOpposite() =
    when (this) {
        CLIENT -> SERVER
        SERVER -> CLIENT
    }

inline fun <T> NetworkSide.packetSender(
    description: SyncedGuiDescription,
    channel: Identifier,
    crossinline serialize: PacketByteBuf.(T) -> Unit,
    crossinline deserialize: PacketByteBuf.() -> T,
    crossinline receive: (T) -> Unit
): (T) -> Unit {
    val receiver = ScreenNetworking.of(description, getOpposite())
    receiver.receive(channel) { receive(it.deserialize()) }
    val sender = ScreenNetworking.of(description, this)
    return { payload -> sender.send(channel) { it.serialize(payload) } }
}
