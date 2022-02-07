package dev.adirelle.adicrate.client.screen

import dev.adirelle.adicrate.screen.CrateScreenHandler
import io.github.cottonmc.cotton.gui.client.CottonInventoryScreen
import net.fabricmc.api.EnvType.CLIENT
import net.fabricmc.api.Environment
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@Environment(CLIENT)
class CrateScreen(gui: CrateScreenHandler, playerInventory: PlayerInventory, title: Text) :
    CottonInventoryScreen<CrateScreenHandler>(gui, playerInventory, title)
