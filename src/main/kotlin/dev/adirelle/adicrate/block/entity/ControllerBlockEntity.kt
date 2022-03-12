@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.block.entity

import dev.adirelle.adicrate.Controller
import dev.adirelle.adicrate.Crate
import dev.adirelle.adicrate.abstraction.FrontInteractionHandler
import dev.adirelle.adicrate.abstraction.Network
import dev.adirelle.adicrate.abstraction.Network.Info
import dev.adirelle.adicrate.abstraction.Network.Node
import dev.adirelle.adicrate.utils.extensions.set
import dev.adirelle.adicrate.utils.logger
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.state.property.Properties
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

class ControllerBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(Controller.BLOCK_ENTITY_TYPE, pos, state),
    Network,
    Info,
    FrontInteractionHandler {

    companion object {

        private const val MAX_DISTANCE = 16
        private const val MAX_NODE_COUNT = 256

        private val amountComparator = Comparator.comparing<Network.Storage, Long> { it.amount }

    }

    private val LOGGER by logger

    private val nodes = HashSet<Node>()

    val storage: Storage<ItemVariant> = StorageAdapter()

    override val facing: Direction
        get() = cachedState.get(Properties.HORIZONTAL_FACING)

    override val info: Info
        get() = this

    override var name: String = pos.toShortString()

    private var dirty = true
    private var destroyed = false

    override fun accept(node: Node) =
        nodes.size < MAX_NODE_COUNT && node.getPos().getManhattanDistance(this.pos) <= MAX_DISTANCE

    override fun add(node: Node) {
        if (!destroyed && accept(node) && nodes.add(node)) {
            LOGGER.debug("added ${node.getPos()}")
            dirty = true
        }
    }

    override fun remove(node: Node) {
        if (!destroyed && nodes.remove(node)) {
            LOGGER.debug("removed ${node.getPos()}")
            dirty = true
        }
    }

    override fun destroy() {
        if (destroyed) return
        destroyed = true
        val nodeList = nodes.toList()
        nodes.clear()
        nodeList.forEach(Node::disconnect)
    }

    fun tick(world: World) {
        if (!dirty) return
        dirty = false
        rebuild(world)
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        nbt["name"] = name
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.getString("name")
            .takeUnless { it.isEmpty() }
            ?.let { name ->
                this.name = name
            }
    }

    private fun rebuild(world: World) {
        val visitor = NetworkVisitor(world)
        visitor.visitNeighbors(pos)
        visitor.unvisited.values.forEach(Node::disconnect)

        LOGGER.debug("nodes: ${nodes.map(Node::getPos).joinToString()}")
    }

    override fun pullItems(player: PlayerEntity) {
        if (player.isSneaking) {
            PlayerInventoryStorage.of(player).slots.forEach(::restock)
        } else {
            restock(ContainerItemContext.ofPlayerHand(player, player.preferredHand).mainSlot)
        }
    }

    private fun restock(slot: SingleSlotStorage<ItemVariant>) {
        if (slot.isResourceBlank || slot.amount >= slot.capacity) return
        StorageUtil.move(storage, slot, { it == slot.resource }, slot.capacity - slot.amount, null)
    }

    override fun pushItems(player: PlayerEntity, hand: Hand) {
        val source =
            if (player.isSneaking || player.getStackInHand(hand).isEmpty) PlayerInventoryStorage.of(player)
            else ContainerItemContext.ofPlayerHand(player, player.preferredHand).mainSlot
        StorageUtil.move(source, storage, { true }, Long.MAX_VALUE, null)
    }

    private inner class NetworkVisitor(private val world: World) {

        val unvisited = HashMap<BlockPos, Node>(nodes.size)

        private val visited = HashSet<BlockPos>(nodes.size)

        init {
            nodes.associateBy(Node::getPos).toMap(unvisited)
        }

        fun visitNeighbors(pos: BlockPos) =
            Direction.values()
                .map(pos::offset)
                .forEach(::visit)

        fun visit(pos: BlockPos) {
            if (!visited.add(pos)) return

            if (pos in unvisited) {
                unvisited.remove(pos)
            } else {
                if (!world.getBlockState(pos).isOf(Crate.BLOCK)) {
                    return
                }
                val node = world.getBlockEntity(pos, Crate.BLOCK_ENTITY_TYPE)
                    .orElseThrow { IllegalArgumentException("expected a CrateBlockEntityType") }

                node.connectTo(this@ControllerBlockEntity)
                nodes.add(node)
            }

            visitNeighbors(pos)
        }
    }

    private inner class StorageAdapter : Storage<ItemVariant> {

        private val storages
            get() = nodes.stream()
                .map(Node::storage)

        override fun insert(resource: ItemVariant, maxAmount: Long, tx: TransactionContext): Long {
            val views = storages
                .filter { !it.isResourceBlank && it.canInsert(resource) }
                .toList()
                .sortedBy { it.capacity - it.amount }
            var inserted = 0L
            for (view in views) {
                inserted += view.insert(resource, maxAmount - inserted, tx)
                if (inserted >= maxAmount) break
            }
            return inserted
        }

        override fun extract(resource: ItemVariant, maxAmount: Long, tx: TransactionContext): Long {
            val views = storages
                .filter { it.canExtract(resource) }
                .toList()
                .sortedBy { it.amount }
            var extracted = 0L
            for (view in views) {
                extracted += view.extract(resource, maxAmount - extracted, tx)
                if (extracted >= maxAmount) break
            }
            return extracted
        }

        override fun iterator(tx: TransactionContext): MutableIterator<StorageView<ItemVariant>> =
            storages.iterator()

        override fun exactView(tx: TransactionContext, resource: ItemVariant): StorageView<ItemVariant>? =
            storages
                .filter { it.resource == resource }
                .max(amountComparator)
                .orElse(null)
    }
}
