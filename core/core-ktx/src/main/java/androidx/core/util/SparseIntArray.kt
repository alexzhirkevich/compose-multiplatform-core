/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("NOTHING_TO_INLINE") // Aliases to public API.

package androidx.core.util

import android.util.SparseIntArray

/** Returns the number of key/value pairs in the collection. */
public inline val SparseIntArray.size: Int
    get() = size()

/** Returns true if the collection contains [key]. */
public inline operator fun SparseIntArray.contains(key: Int): Boolean = indexOfKey(key) >= 0

/** Allows the use of the index operator for storing values in the collection. */
public inline operator fun SparseIntArray.set(key: Int, value: Int): Unit = put(key, value)

/** Creates a new collection by adding or replacing entries from [other]. */
public operator fun SparseIntArray.plus(other: SparseIntArray): SparseIntArray {
    val new = SparseIntArray(size() + other.size())
    new.putAll(this)
    new.putAll(other)
    return new
}

/** Returns true if the collection contains [key]. */
public inline fun SparseIntArray.containsKey(key: Int): Boolean = indexOfKey(key) >= 0

/** Returns true if the collection contains [value]. */
public inline fun SparseIntArray.containsValue(value: Int): Boolean = indexOfValue(value) >= 0

/** Return the value corresponding to [key], or [defaultValue] when not present. */
public inline fun SparseIntArray.getOrDefault(key: Int, defaultValue: Int): Int =
    get(key, defaultValue)

/** Return the value corresponding to [key], or from [defaultValue] when not present. */
public inline fun SparseIntArray.getOrElse(key: Int, defaultValue: () -> Int): Int =
    indexOfKey(key).let { if (it >= 0) valueAt(it) else defaultValue() }

/** Return true when the collection contains no elements. */
public inline fun SparseIntArray.isEmpty(): Boolean = size() == 0

/** Return true when the collection contains elements. */
public inline fun SparseIntArray.isNotEmpty(): Boolean = size() != 0

/** Removes the entry for [key] only if it is mapped to [value]. */
public fun SparseIntArray.remove(key: Int, value: Int): Boolean {
    val index = indexOfKey(key)
    if (index >= 0 && value == valueAt(index)) {
        removeAt(index)
        return true
    }
    return false
}

/** Update this collection by adding or replacing entries from [other]. */
public fun SparseIntArray.putAll(other: SparseIntArray): Unit = other.forEach(::put)

/** Performs the given [action] for each key/value entry. */
public inline fun SparseIntArray.forEach(action: (key: Int, value: Int) -> Unit) {
    for (index in 0 until size()) {
        action(keyAt(index), valueAt(index))
    }
}

/** Return an iterator over the collection's keys. */
public fun SparseIntArray.keyIterator(): IntIterator =
    object : IntIterator() {
        var index = 0

        override fun hasNext() = index < size()

        override fun nextInt() = keyAt(index++)
    }

/** Return an iterator over the collection's values. */
public fun SparseIntArray.valueIterator(): IntIterator =
    object : IntIterator() {
        var index = 0

        override fun hasNext() = index < size()

        override fun nextInt() = valueAt(index++)
    }
