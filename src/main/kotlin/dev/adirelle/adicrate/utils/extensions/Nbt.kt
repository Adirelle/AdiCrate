package dev.adirelle.adicrate.utils.extensions

import dev.adirelle.adicrate.utils.NbtSerializable
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.minecraft.item.Item
import net.minecraft.item.ItemConvertible
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.*
import net.minecraft.util.Identifier
import java.util.*
import kotlin.reflect.safeCast

// Item

fun Item.isEmpty() = this == Items.AIR
fun Item.isNotEmpty() = this != Items.AIR

// Identifier

fun Identifier.toNbt(): NbtString = NbtString.of(toString())

// String

fun NbtString.toIdentifier(): Optional<Identifier> =
    Optional.of(asString())
        .filter(String::isNotEmpty)
        .map(Identifier::tryParse)

fun NbtString.toItem(): Optional<Item> =
    toIdentifier()
        .map(Identifier::toItem)
        .filter(Item::isNotEmpty)

// Serializable

fun NbtSerializable.toNbt(): NbtCompound =
    NbtCompound().also(::writeToNbt)

// Compound

fun NbtCompound.putItem(key: String, item: ItemConvertible) {
    if (item.asItem() != Items.AIR) {
        putString(key, item.asItem().id.toString())
    }
}

fun NbtCompound.putStack(key: String, stack: ItemStack) {
    if (!stack.isEmpty) {
        put(key, NbtCompound().also { stack.writeNbt(it) })
    }
}

inline fun <reified T : NbtElement> NbtCompound.getTyped(key: String): Optional<T> =
    Optional.of(key)
        .filter(::contains)
        .map(::get)
        .map(T::class::safeCast)

fun NbtCompound.getItem(key: String): Optional<Item> =
    getTyped<NbtString>(key).flatMap(NbtString::toItem)

fun NbtCompound.getStack(key: String): ItemStack =
    getTyped<NbtCompound>(key)
        .map(ItemStack::fromNbt)
        .orElse(ItemStack.EMPTY)

inline operator fun <T> NbtCompound.set(
    key: String,
    value: T,
    setter: NbtCompound.(String, T) -> Any?,
    isEmpty: (T) -> Boolean
) {
    if (!isEmpty(value)) {
        setter(key, value)
    }
}

operator fun NbtCompound.set(key: String, value: Long) =
    set(key, value, NbtCompound::putLong, 0L::equals)

operator fun NbtCompound.set(key: String, value: String) =
    set(key, value, NbtCompound::putString, String::isEmpty)

operator fun NbtCompound.set(key: String, value: Int) =
    set(key, value, NbtCompound::putInt, 0::equals)

operator fun NbtCompound.set(key: String, value: Item) =
    set(key, value, NbtCompound::putItem, Items.AIR::equals)

operator fun NbtCompound.set(key: String, value: ItemStack) =
    set(key, value, NbtCompound::putStack, ItemStack::isEmpty)

@Suppress("UnstableApiUsage")
operator fun NbtCompound.set(key: String, value: TransferVariant<*>) =
    set(key, value, { k, v -> put(k, v.toNbt()) }, TransferVariant<*>::isBlank)

operator fun NbtCompound.set(key: String, value: NbtCompound) =
    set(key, value, { k, v -> put(k, v) }, NbtCompound::isEmpty)

operator fun NbtCompound.set(key: String, value: NbtList) =
    set(key, value, { k, v -> put(k, v) }, NbtList::isEmpty)

operator fun NbtCompound.set(key: String, value: NbtSerializable) =
    set(key, value.toNbt())

inline operator fun NbtCompound.set(key: String, builder: (NbtCompound) -> Unit) {
    val nbt = NbtCompound().also(builder)
    if (!nbt.isEmpty) {
        put(key, nbt)
    }
}

@JvmName("setList")
inline operator fun NbtCompound.set(key: String, builder: NbtList.() -> Unit) {
    val nbt = NbtList().apply(builder)
    if (!nbt.isEmpty()) {
        put(key, nbt)
    }
}

inline fun <reified T : NbtElement, R> NbtCompound.read(key: String, crossinline reader: (T) -> R): Optional<R> =
    getTyped<T>(key).map { reader(it) }

inline fun <reified T : NbtElement> NbtCompound.read(key: String, reader: (T) -> Unit) {
    getTyped<T>(key).let {
        if (it.isPresent) {
            reader(it.get())
        }
    }
}

fun NbtCompound.readInto(key: String, target: NbtSerializable): Boolean =
    getTyped<NbtCompound>(key)
        .map {
            target.readFromNbt(it)
            true
        }
        .orElse(false)

@JvmName("readCompound")
inline fun NbtCompound.read(key: String, reader: (NbtCompound) -> Unit) {
    read<NbtCompound>(key) { if (!it.isEmpty) reader(it) }
}

@JvmName("readList")
inline fun NbtCompound.read(key: String, reader: (NbtList) -> Unit) {
    read<NbtList>(key) { if (it.isNotEmpty()) reader(it) }
}

// List

fun NbtList.add(value: Long) = add(NbtLong.of(value))
fun NbtList.add(value: Int) = add(NbtInt.of(value))
fun NbtList.add(value: String) = add(NbtString.of(value))
fun NbtList.add(id: Identifier) = add(id.toNbt())
fun NbtList.add(item: Item) = add(item.toNbt())
fun NbtList.add(stack: ItemStack) = add(stack.toNbt())
fun NbtList.add(value: NbtSerializable) = add(value.toNbt())

inline fun <reified T : NbtElement> NbtList.getTyped(index: Int): Optional<T> =
    Optional.of(index)
        .map(::get)
        .map(T::class::safeCast)

fun NbtList.getItem(index: Int): Optional<Item> =
    getTyped<NbtString>(index).flatMap(NbtString::toItem)

fun NbtList.getStack(index: Int): ItemStack =
    getItem(index).map(::ItemStack).orElse(ItemStack.EMPTY)

fun NbtList.readInto(index: Int, target: NbtSerializable): Boolean =
    getTyped<NbtCompound>(index).map {
        target.readFromNbt(it)
        true
    }.orElse(false)

inline fun <reified T : NbtElement, R> NbtList.read(index: Int, crossinline read: (T) -> R): Optional<R> =
    getTyped<T>(index).map { read(it) }

@JvmName("readUnit")
inline fun <reified T : NbtElement> NbtList.read(index: Int, reader: (T) -> Unit) {
    val optValue = getTyped<T>(index)
    if (optValue.isPresent) {
        reader(optValue.get())
    }
}
