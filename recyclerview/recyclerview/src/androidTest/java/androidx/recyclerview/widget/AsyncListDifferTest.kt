/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.recyclerview.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.TestExecutor
import java.util.Collections.emptyList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.verifyZeroInteractions

@MediumTest
@RunWith(AndroidJUnit4::class)
class AsyncListDifferTest {
    private val mMainThread = TestExecutor()
    private val mBackgroundThread = TestExecutor()

    private fun createDiffer(
        listUpdateCallback: ListUpdateCallback = IGNORE_CALLBACK
    ): AsyncListDiffer<String> {
        return AsyncListDiffer(
            listUpdateCallback,
            AsyncDifferConfig.Builder(STRING_DIFF_CALLBACK)
                .setMainThreadExecutor(mMainThread)
                .setBackgroundThreadExecutor(mBackgroundThread)
                .build()
        )
    }

    @Test
    fun initialState() {
        val callback = mock(ListUpdateCallback::class.java)
        val differ = createDiffer(callback)
        assertEquals(0, differ.currentList.size)
        verifyZeroInteractions(callback)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun getEmpty() {
        val differ = createDiffer()
        differ.currentList[0]
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun getNegative() {
        val differ = createDiffer()
        differ.submitList(listOf("a", "b"))
        differ.currentList[-1]
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun getPastEnd() {
        val differ = createDiffer()
        differ.submitList(listOf("a", "b"))
        differ.currentList[2]
    }

    @Test
    fun getCurrentList() {
        val differ = createDiffer()

        // null is emptyList
        assertSame(emptyList<String>(), differ.currentList)

        // other list is wrapped
        val list = listOf("a", "b")
        differ.submitList(list)
        assertEquals(list, differ.currentList)
        assertNotSame(list, differ.currentList)

        // null again, empty again
        differ.submitList(null)
        assertSame(emptyList<String>(), differ.currentList)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun mutateCurrentListEmpty() {
        val differ = createDiffer()
        differ.currentList[0] = ""
    }

    @Test(expected = UnsupportedOperationException::class)
    fun mutateCurrentListNonEmpty() {
        val differ = createDiffer()
        differ.submitList(listOf("a"))
        differ.currentList[0] = ""
    }

    @Test
    fun submitListSimple() {
        val callback = mock(ListUpdateCallback::class.java)
        val differ = createDiffer(callback)

        differ.submitList(listOf("a", "b"))

        assertEquals(2, differ.currentList.size)
        assertEquals("a", differ.currentList[0])
        assertEquals("b", differ.currentList[1])

        verify(callback).onInserted(0, 2)
        verifyNoMoreInteractions(callback)
        drain()
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun submitListReuse() {
        val callback = mock(ListUpdateCallback::class.java)
        val differ = createDiffer(callback)
        val origList = listOf("a", "b")

        // set up original list
        differ.submitList(origList)
        verify(callback).onInserted(0, 2)
        verifyNoMoreInteractions(callback)
        drain()
        verifyNoMoreInteractions(callback)

        // submit new list, but don't let it finish
        differ.submitList(listOf("c", "d"))
        mMainThread.executeAll()
        verifyNoMoreInteractions(callback)

        // resubmit original list, which should be final observable state
        differ.submitList(origList)
        drain()
        assertEquals(origList, differ.currentList)
    }

    @Test
    fun nullsSkipCallback() {
        val callback = mock(ListUpdateCallback::class.java)
        // Note: by virtue of being written in Kotlin, the item callback includes explicit null
        // checks on its parameters which assert that it is not invoked with a null value.
        val helper = createDiffer(callback)

        helper.submitList(listOf("a", "b"))
        drain()
        verify(callback).onInserted(0, 2)

        helper.submitList(listOf("a", null))
        drain()
        verify(callback).onRemoved(1, 1)
        verify(callback).onInserted(1, 1)

        helper.submitList(listOf("b", null))
        drain()
        verify(callback).onRemoved(0, 1)
        verify(callback).onInserted(0, 1)

        verifyNoMoreInteractions(callback)
    }

    @Test
    fun submitListUpdate() {
        val callback = mock(ListUpdateCallback::class.java)
        val differ = createDiffer(callback)

        // initial list (immediate)
        differ.submitList(listOf("a", "b"))
        verify(callback).onInserted(0, 2)
        verifyNoMoreInteractions(callback)
        drain()
        verifyNoMoreInteractions(callback)

        // update (deferred)
        differ.submitList(listOf("a", "b", "c"))
        verifyNoMoreInteractions(callback)
        drain()
        verify(callback).onInserted(2, 1)
        verifyNoMoreInteractions(callback)

        // clear (immediate)
        differ.submitList(null)
        verify(callback).onRemoved(0, 3)
        verifyNoMoreInteractions(callback)
        drain()
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun singleChangePayload() {
        val callback = mock(ListUpdateCallback::class.java)
        val differ = createDiffer(callback)

        differ.submitList(listOf("a", "b"))
        verify(callback).onInserted(0, 2)
        verifyNoMoreInteractions(callback)
        drain()
        verifyNoMoreInteractions(callback)

        differ.submitList(listOf("a", "beta"))
        verifyNoMoreInteractions(callback)
        drain()
        verify(callback).onChanged(1, 1, "eta")
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun multiChangePayload() {
        val callback = mock(ListUpdateCallback::class.java)
        val differ = createDiffer(callback)

        differ.submitList(listOf("a", "b"))
        verify(callback).onInserted(0, 2)
        verifyNoMoreInteractions(callback)
        drain()
        verifyNoMoreInteractions(callback)

        differ.submitList(listOf("alpha", "beta"))
        verifyNoMoreInteractions(callback)
        drain()
        verify(callback).onChanged(1, 1, "eta")
        verify(callback).onChanged(0, 1, "lpha")
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun listUpdatedBeforeListUpdateCallbacks() {
        // verify that itemCount is updated in the differ before dispatching ListUpdateCallbacks

        val expectedCount = intArrayOf(0)
        // provides access to differ, which must be constructed after callback
        val differAccessor = arrayOf<AsyncListDiffer<*>?>(null)

        val callback =
            object : ListUpdateCallback {

                override fun onInserted(position: Int, count: Int) {
                    assertEquals(expectedCount[0], differAccessor[0]!!.currentList.size)
                }

                override fun onRemoved(position: Int, count: Int) {
                    assertEquals(expectedCount[0], differAccessor[0]!!.currentList.size)
                }

                override fun onMoved(fromPosition: Int, toPosition: Int) {
                    fail("not expected")
                }

                override fun onChanged(position: Int, count: Int, payload: Any?) {
                    fail("not expected")
                }
            }

        val differ = createDiffer(callback)
        differAccessor[0] = differ

        // in the fast-add case...
        expectedCount[0] = 3
        assertEquals(0, differ.currentList.size)
        differ.submitList(listOf("a", "b", "c"))
        assertEquals(3, differ.currentList.size)

        // in the slow, diff on BG thread case...
        expectedCount[0] = 6
        assertEquals(3, differ.currentList.size)
        differ.submitList(listOf("a", "b", "c", "d", "e", "f"))
        drain()
        assertEquals(6, differ.currentList.size)

        // and in the fast-remove case
        expectedCount[0] = 0
        assertEquals(6, differ.currentList.size)
        differ.submitList(null)
        assertEquals(0, differ.currentList.size)
    }

    @Test
    fun listListener() {
        val differ = createDiffer()

        @Suppress("UNCHECKED_CAST")
        val listener =
            mock(AsyncListDiffer.ListListener::class.java) as AsyncListDiffer.ListListener<String>
        differ.addListListener(listener)

        val callback = mock(Runnable::class.java)

        // first - simple insert
        val first = listOf("a", "b")
        verifyZeroInteractions(listener)
        differ.submitList(first, callback)
        verify(listener).onCurrentListChanged(emptyList<String>(), first)
        verifyNoMoreInteractions(listener)
        verify(callback).run()
        verifyNoMoreInteractions(callback)
        reset(callback)

        // second - async update
        val second = listOf("c", "d")
        differ.submitList(second, callback)
        verifyNoMoreInteractions(listener)
        verifyNoMoreInteractions(callback)
        drain()
        verify(listener).onCurrentListChanged(first, second)
        verifyNoMoreInteractions(listener)
        verify(callback).run()
        verifyNoMoreInteractions(callback)
        reset(callback)

        // third - same list - only triggers callback
        differ.submitList(second, callback)
        verifyNoMoreInteractions(listener)
        verify(callback).run()
        verifyNoMoreInteractions(callback)
        drain()
        verifyNoMoreInteractions(listener)
        verifyNoMoreInteractions(callback)
        reset(callback)

        // fourth - null
        differ.submitList(null, callback)
        verify(listener).onCurrentListChanged(second, emptyList<String>())
        verifyNoMoreInteractions(listener)
        verify(callback).run()
        verifyNoMoreInteractions(callback)
        reset(callback)

        // remove listener, see nothing
        differ.removeListListener(listener)
        differ.submitList(first)
        drain()
        verifyNoMoreInteractions(listener)
    }

    private fun drain() {
        var executed: Boolean
        do {
            executed = mBackgroundThread.executeAll()
            executed = mMainThread.executeAll() or executed
        } while (executed)
    }

    companion object {
        private val STRING_DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<String>() {
                override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                    // items are the same if first char is the same
                    return oldItem[0] == newItem[0]
                }

                override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                    return oldItem == newItem
                }

                override fun getChangePayload(oldItem: String, newItem: String): Any? {
                    if (newItem.startsWith(oldItem)) {
                        // new string is appended, return added portion on the end
                        return newItem.subSequence(oldItem.length, newItem.length)
                    }
                    return null
                }
            }

        private val IGNORE_CALLBACK =
            object : ListUpdateCallback {
                override fun onInserted(position: Int, count: Int) {}

                override fun onRemoved(position: Int, count: Int) {}

                override fun onMoved(fromPosition: Int, toPosition: Int) {}

                override fun onChanged(position: Int, count: Int, payload: Any?) {}
            }
    }
}
