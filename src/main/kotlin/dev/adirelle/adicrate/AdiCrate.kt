package dev.adirelle.adicrate

import dev.adirelle.adicrate.utils.Mod

@Suppress("unused")
object AdiCrate : Mod("AdiCrate", "adicrate") {

    override fun onInitialize() {
        load(Crate)
        super.onInitialize()
    }
}
