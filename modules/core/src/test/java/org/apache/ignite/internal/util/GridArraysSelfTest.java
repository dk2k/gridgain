/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ignite.internal.util;

import java.util.Arrays;

import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;

import static org.apache.ignite.internal.util.GridArrays.clearTail;
import static org.apache.ignite.internal.util.GridArrays.remove;
import static org.apache.ignite.internal.util.GridArrays.set;
import static org.apache.ignite.internal.util.GridArrays.subtract;

/**
 */
public class GridArraysSelfTest extends GridCommonAbstractTest {
    /** */
    private static final String[] EMPTY = {};

    /**
     */
    @Test
    public void testSet() {
        String[] arr = set(EMPTY, 4, "aa");

        assertNotSame(EMPTY, arr);
        assertEquals("aa", arr[4]);

        for (int i = 0; i < arr.length; i++) {
            if (i != 4)
                assertNull(arr[i]);
        }

        String[] oldArr = arr;

        arr = set(arr, 1, "bb");

        assertSame(oldArr, arr);
        assertEquals("aa", arr[4]);
        assertEquals("bb", arr[1]);

        for (int i = 0; i < arr.length; i++) {
            if (i != 1 && i != 4)
                assertNull(arr[i]);
        }

        arr = set(arr, 100, "cc");

        assertNotSame(oldArr, arr);
        assertEquals("aa", arr[4]);
        assertEquals("bb", arr[1]);
        assertEquals("cc", arr[100]);

        for (int i = 0; i < arr.length; i++) {
            if (i != 1 && i != 4 && i != 100)
                assertNull(arr[i]);
        }
    }

    /**
     */
    @Test
    public void testClearTail() {
        String[] arr = new String[10];

        Arrays.fill(arr, "zz");

        clearTail(arr, 11);

        for (String s : arr)
            assertEquals("zz", s);

        clearTail(arr, 10);

        for (String s : arr)
            assertEquals("zz", s);

        clearTail(arr, 9);

        assertNull(arr[9]);

        for (int i = 0; i < 9; i++)
            assertEquals("zz", arr[i]);

        clearTail(arr, 7);

        assertNull(arr[7]);
        assertNull(arr[8]);
        assertNull(arr[9]);

        for (int i = 0; i < 7; i++)
            assertEquals("zz", arr[i]);
    }

    /**
     */
    @Test
    public void testRemoveLong() {
        long[] arr = {0,1,2,3,4,5,6};

        assertTrue(Arrays.equals(new long[]{1,2,3,4,5,6}, remove(arr, 0)));
        assertTrue(Arrays.equals(new long[]{0,2,3,4,5,6}, remove(arr, 1)));
        assertTrue(Arrays.equals(new long[]{0,1,2,3,5,6}, remove(arr, 4)));
        assertTrue(Arrays.equals(new long[]{0,1,2,3,4,5}, remove(arr, 6)));
        assertTrue(Arrays.equals(new long[0], remove(new long[]{1}, 0)));
    }

    /**
     */
    @Test
    public void testRemove() {
        Integer[] arr = {0,1,2,3,4,5,6};

        assertTrue(Arrays.equals(new Integer[]{1,2,3,4,5,6}, remove(arr, 0)));
        assertTrue(Arrays.equals(new Integer[]{0,2,3,4,5,6}, remove(arr, 1)));
        assertTrue(Arrays.equals(new Integer[]{0,1,2,3,5,6}, remove(arr, 4)));
        assertTrue(Arrays.equals(new Integer[]{0,1,2,3,4,5}, remove(arr, 6)));
        assertTrue(Arrays.equals(new Integer[0], remove(new Integer[]{1}, 0)));
    }

    /**
     * Test array intersection.
     *
     * 1) Subtract empty array.
     * 2) Subtract all elements.
     * 3) Subtract some elements.
     */
    @Test
    public void subtractTest() {
        int[] a123 = new int[]{1, 2, 3};
        int[] a13 = new int[]{1, 3};
        int[] b123 = new int[]{1, 2, 3};
        int[] b2 = new int[]{2};
        int[] z = new int[0];

        // 1) Subtract empty array.
        assertTrue(Arrays.equals(a123, subtract(a123, z)));

        // 2) Subtract all elements.
        assertEquals(0, subtract(a123, b123).length);

        // 3) Subtract some elements.
        assertTrue(Arrays.equals(a13, subtract(a123, b2)));
    }
}
