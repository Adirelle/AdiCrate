package dev.adirelle.adicrate.utils

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.properties.PropertyDelegateProvider
import kotlin.reflect.KProperty

object lazyLogger : PropertyDelegateProvider<Any, Lazy<Logger>> {

    operator fun invoke(name: String): Lazy<Logger> =
        lazy { LogManager.getLogger(name) }

    operator fun invoke(target: Any): Lazy<Logger> =
        lazy { LogManager.getLogger(target) }

    override fun provideDelegate(thisRef: Any, property: KProperty<*>): Lazy<Logger> =
        lazy { LogManager.getLogger(thisRef::class.java) }
}
