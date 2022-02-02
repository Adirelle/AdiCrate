# AdiCrate

A bad clone of [Storage Drawer](https://www.curseforge.com/minecraft/mc-mods/storage-drawers) for Fabric.

## Recipes

I am too lazy to copy them here, please check the recipe book
or [Roughly Enough Items](https://www.curseforge.com/minecraft/mc-mods/roughly-enough-items). Sorry.

## Blocks

### Crate

A crate can hold several stacks, 16 by default, of a single item. When broken, the crate retains its content.

You can interact directly with the front of the crate (the face showing the item and its count):

* Left-click (attack) to extract a stack of the contained item,
* Shift-left-click (sneak-attack) to extract one item,
* Right-click (use) with an item in hand to insert it into the crate, if possible,
* Shift-right-click (sneak-use) with an empty hand to insert all matching items from your inventory in the crate.

Crate can be improved by inserting up to 5 upgrades. Right-click (use) any face but the front to open its GUI. At the
bottom of the screen, just over the player inventory, there are 5 slots available for upgrades. Each slot can contain
one item, and you can insert several upgrade of the same type (e.g. 5 iron ingots).

Possible upgrades are:

* An iron ingot to increase the maximum capacity by 16 stacks.
* A diamond to increase the maximum capacity by 32 stacks.
* A netherite ingot to increase the maximum capacity by 48 stacks.
* A cobweb to lock the crate to the contained item, i.e. if the crate is emptied, it remembers the item and will not
  accept other items.
* A bucket of lava to have the crate accepts more items than its maximum capacity but destroy overflow items.

Beware: if you remove a capacity increase and the resulting capacity is lower than the current amount of items, the
crate gets jammed and cannot be used until you increase the capacity again.

Crates can also be accessed by hoppers and any mod that supports the Fabric Transfer API.

### Controller

The controller should be placed next to existing crate and allows to access remotely all connected crates.

## License

Unless explicitly stated otherwise all files in this repository are licensed under the MIT License (MIT).
