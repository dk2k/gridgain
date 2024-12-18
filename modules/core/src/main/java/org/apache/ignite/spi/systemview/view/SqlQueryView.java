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

package org.apache.ignite.spi.systemview.view;

import java.util.Date;
import java.util.UUID;
import org.apache.ignite.internal.managers.systemview.walker.Order;
import org.apache.ignite.internal.processors.query.GridRunningQueryInfo;

/**
 * SQL query representation for a {@link SystemView}.
 */
public class SqlQueryView {
    /** Query. */
    private final GridRunningQueryInfo qry;

    /**
     * @param qry Query.
     */
    public SqlQueryView(GridRunningQueryInfo qry) {
        this.qry = qry;
    }

    /** @return Origin query node. */
    @Order(2)
    public UUID originNodeId() {
        return qry.nodeId();
    }

    /** @return Query ID. */
    @Order
    public String queryId() {
        return qry.globalQueryId();
    }

    /** @return Query text. */
    @Order(1)
    public String sql() {
        return qry.query();
    }

    /** @return Query start time. */
    @Order(3)
    public Date startTime() {
        return new Date(qry.startTime());
    }

    /** @return Query duration. */
    @Order(4)
    public long duration() {
        return System.currentTimeMillis() - qry.startTime();
    }

    /**
     * Returns current allocated size of data on disk.
     *
     * @return Current allocated size of data on disk.
     */
    @Order(5)
    public long diskAllocationCurrent() {
        return qry.memoryMetricProvider().writtenOnDisk();
    }

    /**
     * Returns maximum allocated size of data on disk.
     *
     * @return Maximum allocated size of data on disk.
     */
    @Order(6)
    public long diskAllocationMax() {
        return qry.memoryMetricProvider().maxWrittenOnDisk();
    }

    /**
     * Returns total allocated size of data on disk.
     *
     * @return Total allocated size of data on disk.
     */
    @Order(7)
    public long diskAllocationTotal() {
        return qry.memoryMetricProvider().totalWrittenOnDisk();
    }

    /**
     * Returns query initiator ID.
     *
     * @return Query initiator ID.
     */
    @Order(8)
    public String initiatorId() {
        return qry.queryInitiatorId();
    }

    /** @return {@code True} if query is local. */
    @Order(9)
    public boolean local() {
        return qry.local();
    }

    /**
     * Returns current size of reserved memory.
     *
     * @return Current size of reserved memory.
     */
    @Order(10)
    public long memoryCurrent() {
        return qry.memoryMetricProvider().reserved();
    }

    /**
     * Returns maximum size of reserved memory.
     *
     * @return Maximum size of reserved memory.
     */
    @Order(11)
    public long memoryMax() {
        return qry.memoryMetricProvider().maxReserved();
    }

    /** @return Schema name. */
    @Order(12)
    public String schemaName() {
        return qry.schemaName();
    }

    /**
     * @return Distributed joins.
     */
    @Order(13)
    public boolean distributedJoins() {
        return qry.distributedJoins();
    }

    /**
     * @return Enforce join order.
     */
    @Order(14)
    public boolean enforceJoinOrder() {
        return qry.enforceJoinOrder();
    }

    /**
     * @return Lazy flag.
     */
    @Order(15)
    public boolean lazy() {
        return qry.lazy();
    }

    /**
     * @return Query label.
     */
    @Order(16)
    public String label() {
        return qry.label();
    }
}
