package ru.skillbranch.kotlinexample.extensions

object Iterable {
    fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> {
        for ((index, element) in withIndex()) {
            if (element == predicate) {
                return subList(0, index)
            }
        }
        return emptyList()
    }
}