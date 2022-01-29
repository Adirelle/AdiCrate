package dev.adirelle.adicrate.utils

import java.lang.ref.WeakReference
import java.util.*
import java.util.function.Function

class Memoizer<A : Any, B : Any>(
    private val computation: (A) -> B
) : Function<A, B> {

    private val instances = WeakHashMap<A, WeakReference<B>>()

    override fun apply(t: A): B =
        synchronized(instances) {
            instances[t]?.get() ?: (computation(t).also {
                instances[t] = WeakReference(it)
            })
        }

    operator fun get(input: A): B = apply(input)
    operator fun invoke(input: A) = apply(input)

    fun getOrNull(input: A): B? =
        synchronized(instances) { instances[input]?.get() }

    operator fun contains(input: A) =
        synchronized(instances) {
            instances[input]?.get() != null
        }
}

/**
 * Memoize a one-argument lambda
 */
fun <A : Any, B : Any> memoize(computation: (A) -> B) =
    Memoizer(computation)
