package dev.adirelle.adicrate.abstraction

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Hand

interface FrontInteractionHandler {

    fun pullItems(player: PlayerEntity)

    fun pushItems(player: PlayerEntity, hand: Hand)

}
