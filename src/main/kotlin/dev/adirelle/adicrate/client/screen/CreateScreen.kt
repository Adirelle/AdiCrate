package dev.adirelle.adicrate.client.screen

import dev.adirelle.adicrate.screen.CrateScreenHandler
import io.github.cottonmc.cotton.gui.client.CottonInventoryScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

class CreateScreen(gui: CrateScreenHandler, playerInventory: PlayerInventory, title: Text) :
    CottonInventoryScreen<CrateScreenHandler>(gui, playerInventory, title)
