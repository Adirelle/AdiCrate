package dev.adirelle.adicrate.abstraction

import dev.adirelle.adicrate.network.PullItemC2SPacket
import net.fabricmc.api.EnvType.CLIENT
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult.FAIL
import net.minecraft.util.ActionResult.PASS
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

interface FrontInteractionHandler {

    val facing: Direction;

    fun pullItems(player: PlayerEntity)

    fun pushItems(player: PlayerEntity, hand: Hand)

    companion object : AttackBlockCallback {

        @Environment(CLIENT)
        override fun interact(player: PlayerEntity, world: World, hand: Hand, pos: BlockPos, direction: Direction) =
            (world.getBlockEntity(pos) as? FrontInteractionHandler)
                ?.takeIf { direction == it.facing }
                ?.let {
                    if (!player.handSwinging) {
                        PullItemC2SPacket.send(player, pos)
                        player.swingHand(hand)
                    }
                    // Do not let the client send an "attack" packet
                    FAIL
                }
                ?: PASS
    }
}
