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

package org.apache.ignite.internal.managers.systemview.walker;

import org.apache.ignite.spi.systemview.view.SystemViewRowAttributeWalker;
import org.apache.ignite.spi.systemview.view.datastructures.ReentrantLockView;

/**
 * Generated by {@code org.apache.ignite.codegen.SystemViewRowAttributeWalkerGenerator}.
 * {@link ReentrantLockView} attributes walker.
 * 
 * @see ReentrantLockView
 */
public class ReentrantLockViewWalker implements SystemViewRowAttributeWalker<ReentrantLockView> {
    /** {@inheritDoc} */
    @Override public void visitAll(AttributeVisitor v) {
        v.accept(0, "name", String.class);
        v.accept(1, "locked", boolean.class);
        v.accept(2, "hasQueuedThreads", boolean.class);
        v.accept(3, "failoverSafe", boolean.class);
        v.accept(4, "fair", boolean.class);
        v.accept(5, "broken", boolean.class);
        v.accept(6, "groupName", String.class);
        v.accept(7, "groupId", int.class);
        v.accept(8, "removed", boolean.class);
    }

    /** {@inheritDoc} */
    @Override public void visitAll(ReentrantLockView row, AttributeWithValueVisitor v) {
        v.accept(0, "name", String.class, row.name());
        v.acceptBoolean(1, "locked", row.locked());
        v.acceptBoolean(2, "hasQueuedThreads", row.hasQueuedThreads());
        v.acceptBoolean(3, "failoverSafe", row.failoverSafe());
        v.acceptBoolean(4, "fair", row.fair());
        v.acceptBoolean(5, "broken", row.broken());
        v.accept(6, "groupName", String.class, row.groupName());
        v.acceptInt(7, "groupId", row.groupId());
        v.acceptBoolean(8, "removed", row.removed());
    }

    /** {@inheritDoc} */
    @Override public int count() {
        return 9;
    }
}
