package com.example.osmandtesttask.common

inline fun <T> MutableList<T>.removeAndReturnIf(predicate: (T) -> Boolean): List<T> {
    val removed = mutableListOf<T>()
    val iterator = this.iterator()
    while (iterator.hasNext()) {
        val item = iterator.next()
        if (predicate(item)) {
            removed.add(item)
            iterator.remove()
        }
    }
    return removed
}