package dev.adirelle.adicrate.utils

import com.google.common.base.Strings

inline fun <T> validated(value: T, isValid: Boolean, message: () -> String): T {
    require(isValid, message)
    return value
}

fun <T : List<*>> requireExactSize(list: T, size: Int, desc: String = "list"): T =
    validated(list, size == list.size) {
        Strings.lenientFormat(
            "expected a %s of exactly %s item(s) but got a list of %s items",
            desc,
            size,
            list.size
        )
    }
