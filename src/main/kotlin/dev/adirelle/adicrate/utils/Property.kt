package dev.adirelle.adicrate.utils

import net.minecraft.screen.Property
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

inline fun property(crossinline getter: () -> Int): Property =
    object : Property() {
        override fun get() = getter()
        override fun set(value: Int) {}
    }

inline fun property(crossinline getter: () -> Int, crossinline setter: (Int) -> Unit): Property =
    object : Property() {
        override fun get() = getter()
        override fun set(value: Int) {
            setter(value)
        }
    }

operator fun <T> Property.provideDelegate(thisRef: T, prop: KProperty<*>): ReadWriteProperty<T, Int> =
    object : ReadWriteProperty<T, Int> {
        override fun getValue(thisRef: T, property: KProperty<*>) =
            get()

        override fun setValue(thisRef: T, property: KProperty<*>, value: Int) =
            set(value)
    }

fun <T, V> Property.mapped(transform: (Int) -> V, reverse: (V) -> Int): ReadWriteProperty<T, V> =
    object : ReadWriteProperty<T, V> {
        override fun getValue(thisRef: T, property: KProperty<*>): V =
            transform(get())

        override fun setValue(thisRef: T, property: KProperty<*>, value: V) =
            set(reverse(value))
    }
