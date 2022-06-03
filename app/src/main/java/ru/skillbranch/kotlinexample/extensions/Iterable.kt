package ru.skillbranch.kotlinexample.extensions

object Iterable {
    fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> {
        for (i in size - 1 downTo 0)  {
            if (this[i] == predicate) {
                return subList(0, i)
            }
        }
        return emptyList()
    }
}