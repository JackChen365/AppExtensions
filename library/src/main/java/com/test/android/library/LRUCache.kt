package com.test.android.library

import java.util.*

class LRUCache<E>(
    private val capacity: Int
) {
    private val deque: Deque<E>

    /**
     * Put a new element into the collection.
     * @param e
     */
    fun push(e: E) {
        if (deque.contains(e)) {
            deque.remove(e)
        } else {
            if (deque.size == capacity) {
                deque.removeLast()
            }
        }
        deque.push(e)
    }

    fun pop(): E {
        return deque.pop()
    }

    init {
        deque = LinkedList()
    }
}