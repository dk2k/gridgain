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

package org.apache.ignite.internal.client.thin;

import java.util.BitSet;
import java.util.Collection;
import java.util.EnumSet;
import org.apache.ignite.client.ClientServices;

/**
 * Defines supported bitmask features for thin client.
 */
public enum ProtocolBitmaskFeature {
    /** Feature for user attributes. */
    USER_ATTRIBUTES(0),

    /** Compute tasks (execute by task name). */
    EXECUTE_TASK_BY_NAME(1),

    /**
     * Cluster operations (state and WAL). In addition to {@link ProtocolVersionFeature#CLUSTER_API} this feature
     * also allows to use other cluster states beside ACTIVE and INACTIVE.
     */
    CLUSTER_API(2),

    /** Client discovery. */
    CLUSTER_GROUP_GET_NODES_ENDPOINTS(3),

    /** Cluster groups. */
    CLUSTER_GROUPS(4),

    /** Invoke service methods. */
    SERVICE_INVOKE(5),

    /** Feature for use default query timeout if the qry timeout isn't set explicitly. */
    DEFAULT_QRY_TIMEOUT(6),

    /** Additional SqlFieldsQuery properties: partitions, updateBatchSize */
    QRY_PARTITIONS_BATCH_SIZE(7),

    /** Binary configuration retrieval. */
    BINARY_CONFIGURATION(8),

    /** Handle of {@link ClientServices#serviceDescriptors()}. */
    GET_SERVICE_DESCRIPTORS(9),

    /** Invoke service methods with caller context. */
    SERVICE_INVOKE_CALLCTX(10),

    /** Handle OP_HEARTBEAT and OP_GET_IDLE_TIMEOUT. */
    HEARTBEAT(11),

    // 12, 13 - reserved, used in Ignite.

    /** IndexQuery. */
    INDEX_QUERY(14),

    /** IndexQuery limit. */
    INDEX_QUERY_LIMIT(15);

    /** */
    private static final EnumSet<ProtocolBitmaskFeature> ALL_FEATURES_AS_ENUM_SET =
        EnumSet.allOf(ProtocolBitmaskFeature.class);

    /** Feature id. */
    private final int featureId;

    /**
     * @param id Feature ID.
     */
    ProtocolBitmaskFeature(int id) {
        featureId = id;
    }

    /**
     * @return Feature ID.
     */
    public int featureId() {
        return featureId;
    }

    /**
     * @param bytes Feature byte array.
     * @return Set of supported features.
     */
    public static EnumSet<ProtocolBitmaskFeature> enumSet(byte[] bytes) {
        EnumSet<ProtocolBitmaskFeature> set = EnumSet.noneOf(ProtocolBitmaskFeature.class);

        if (bytes == null)
            return set;

        final BitSet bSet = BitSet.valueOf(bytes);

        for (ProtocolBitmaskFeature e : ProtocolBitmaskFeature.values()) {
            if (bSet.get(e.featureId()))
                set.add(e);
        }

        return set;
    }

    /**
     * @param features Feature set.
     * @return Byte array representing all supported features.
     */
    static byte[] featuresAsBytes(Collection<ProtocolBitmaskFeature> features) {
        final BitSet set = new BitSet();

        for (ProtocolBitmaskFeature f : features)
            set.set(f.featureId());

        return set.toByteArray();
    }

    /**
     * @return All features as a set.
     */
    public static EnumSet<ProtocolBitmaskFeature> allFeaturesAsEnumSet() {
        return ALL_FEATURES_AS_ENUM_SET.clone();
    }
}
