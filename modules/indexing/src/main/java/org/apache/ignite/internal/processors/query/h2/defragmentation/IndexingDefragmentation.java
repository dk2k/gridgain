/*
 * Copyright 2020 GridGain Systems, Inc. and Contributors.
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

package org.apache.ignite.internal.processors.query.h2.defragmentation;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.internal.pagemem.PageMemory;
import org.apache.ignite.internal.pagemem.PageUtils;
import org.apache.ignite.internal.processors.cache.CacheGroupContext;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.cache.persistence.CacheDataRow;
import org.apache.ignite.internal.processors.cache.persistence.CacheDataRowAdapter;
import org.apache.ignite.internal.processors.cache.persistence.checkpoint.CheckpointTimeoutLock;
import org.apache.ignite.internal.processors.cache.persistence.defragmentation.LinkMap;
import org.apache.ignite.internal.processors.cache.persistence.defragmentation.TreeIterator;
import org.apache.ignite.internal.processors.cache.persistence.pagemem.PageMemoryEx;
import org.apache.ignite.internal.processors.cache.persistence.tree.BPlusTree;
import org.apache.ignite.internal.processors.cache.persistence.tree.io.BPlusIO;
import org.apache.ignite.internal.processors.cache.persistence.tree.io.BPlusInnerIO;
import org.apache.ignite.internal.processors.cache.persistence.tree.io.BPlusLeafIO;
import org.apache.ignite.internal.processors.cache.persistence.tree.io.BPlusMetaIO;
import org.apache.ignite.internal.processors.cache.persistence.tree.io.IOVersions;
import org.apache.ignite.internal.processors.cache.persistence.tree.io.PageIO;
import org.apache.ignite.internal.processors.cache.persistence.tree.io.PageIoResolver;
import org.apache.ignite.internal.processors.cache.tree.mvcc.data.MvccDataRow;
import org.apache.ignite.internal.processors.query.h2.IgniteH2Indexing;
import org.apache.ignite.internal.processors.query.h2.database.H2Tree;
import org.apache.ignite.internal.processors.query.h2.database.H2TreeIndex;
import org.apache.ignite.internal.processors.query.h2.database.InlineIndexColumn;
import org.apache.ignite.internal.processors.query.h2.database.inlinecolumn.InlineIndexColumnFactory;
import org.apache.ignite.internal.processors.query.h2.database.io.AbstractH2ExtrasInnerIO;
import org.apache.ignite.internal.processors.query.h2.database.io.AbstractH2ExtrasLeafIO;
import org.apache.ignite.internal.processors.query.h2.database.io.AbstractH2InnerIO;
import org.apache.ignite.internal.processors.query.h2.database.io.AbstractH2LeafIO;
import org.apache.ignite.internal.processors.query.h2.database.io.H2IOUtils;
import org.apache.ignite.internal.processors.query.h2.database.io.H2RowLinkIO;
import org.apache.ignite.internal.processors.query.h2.opt.GridH2RowDescriptor;
import org.apache.ignite.internal.processors.query.h2.opt.GridH2Table;
import org.apache.ignite.internal.processors.query.h2.opt.H2CacheRow;
import org.apache.ignite.internal.processors.query.h2.opt.H2Row;
import org.apache.ignite.internal.util.IgniteUtils;
import org.apache.ignite.internal.util.collection.IntMap;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.thread.IgniteThreadPoolExecutor;
import org.gridgain.internal.h2.index.Index;
import org.gridgain.internal.h2.util.Utils;
import org.gridgain.internal.h2.value.Value;

/**
 *
 */
public class IndexingDefragmentation {
    /** Indexing. */
    private final IgniteH2Indexing indexing;

    /** Constructor. */
    public IndexingDefragmentation(IgniteH2Indexing indexing) {
        this.indexing = indexing;
    }

    /**
     * Defragment index partition.
     *
     * @param grpCtx Old group context.
     * @param newCtx New group context.
     * @param partPageMem Partition page memory.
     * @param mappingByPartition Mapping page memory.
     * @param cpLock Defragmentation checkpoint read lock.
     * @param cancellationChecker Cancellation checker.
     * @param log Log.
     * @param defragmentationThreadPool Thread pool for defragmentation.
     *
     * @throws IgniteCheckedException If failed.
     */
    public void defragment(
        CacheGroupContext grpCtx,
        CacheGroupContext newCtx,
        PageMemoryEx partPageMem,
        IntMap<LinkMap> mappingByPartition,
        CheckpointTimeoutLock cpLock,
        Runnable cancellationChecker,
        IgniteLogger log,
        IgniteThreadPoolExecutor defragmentationThreadPool
    ) throws IgniteCheckedException {
        int pageSize = grpCtx.cacheObjectContext().kernalContext().grid().configuration().getDataStorageConfiguration().getPageSize();

        PageMemoryEx oldCachePageMem = (PageMemoryEx)grpCtx.dataRegion().pageMemory();

        PageMemory newCachePageMemory = partPageMem;

        Collection<GridH2Table> tables = indexing.schemaManager().dataTables();

        long cpLockThreshold = 150L;

        AtomicLong lastCpLockTs = new AtomicLong(System.currentTimeMillis());

        IgniteUtils.doInParallel(
            defragmentationThreadPool,
            tables,
            table -> defragmentTable(
                grpCtx,
                newCtx,
                mappingByPartition,
                cpLock,
                cancellationChecker,
                log,
                pageSize,
                oldCachePageMem,
                newCachePageMemory,
                cpLockThreshold,
                lastCpLockTs,
                table
            )
        );

        if (log.isInfoEnabled())
            log.info("Defragmentation indexes completed for group '" + grpCtx.groupId() + "'");
    }

    /**
     * Defragment one given table.
     */
    private boolean defragmentTable(
        CacheGroupContext grpCtx,
        CacheGroupContext newCtx,
        IntMap<LinkMap> mappingByPartition,
        CheckpointTimeoutLock cpLock,
        Runnable cancellationChecker,
        IgniteLogger log,
        int pageSize,
        PageMemoryEx oldCachePageMem,
        PageMemory newCachePageMemory,
        long cpLockThreshold,
        AtomicLong lastCpLockTs,
        GridH2Table table
    ) throws IgniteCheckedException {
        cpLock.checkpointReadLock();

        try {
            TreeIterator treeIterator = new TreeIterator(pageSize);

            GridCacheContext<?, ?> cctx = table.cacheContext();

            if (cctx.groupId() != grpCtx.groupId())
                return false;

            cancellationChecker.run();

            GridH2RowDescriptor rowDesc = table.rowDescriptor();

            List<Index> indexes = table.getIndexes();

            final List<H2TreeIndex> oldIndexes = indexes.stream()
                .filter(index -> index instanceof H2TreeIndex)
                .map(H2TreeIndex.class::cast)
                .collect(Collectors.toList());

            for (H2TreeIndex oldH2Idx : oldIndexes) {
                int segments = oldH2Idx.segmentsCount();

                H2Tree firstTree = oldH2Idx.treeForRead(0);

                PageIoResolver pageIoRslvr = pageAddr -> {
                    PageIO io = PageIoResolver.DEFAULT_PAGE_IO_RESOLVER.resolve(pageAddr);

                    if (io instanceof BPlusMetaIO)
                        return io;

                    //noinspection unchecked,rawtypes,rawtypes
                    return wrap((BPlusIO)io);
                };

                H2TreeIndex newIdx = H2TreeIndex.createIndex(
                    cctx,
                    null,
                    table,
                    oldH2Idx.getName(),
                    firstTree.getPk(),
                    firstTree.getAffinityKey(),
                    Arrays.asList(firstTree.cols()),
                    Arrays.asList(firstTree.cols()),
                    oldH2Idx.inlineSize(),
                    segments,
                    newCachePageMemory,
                    newCtx.offheap(),
                    pageIoRslvr,
                    log
                );

                for (int segment = 0; segment < segments; segment++) {
                    final int finalSegment = segment;
                    H2Tree tree = oldH2Idx.treeForRead(segment);
                    final H2Tree.MetaPageInfo oldInfo = tree.getMetaInfo();

                    final H2Tree newTree = newIdx.treeForRead(segment);

                    // Set IO wrappers for new tree.
                    BPlusInnerIO<H2Row> innerIO = (BPlusInnerIO<H2Row>) wrap(newTree.latestInnerIO());
                    BPlusLeafIO<H2Row> leafIo = (BPlusLeafIO<H2Row>) wrap(newTree.latestLeafIO());
                    newTree.setIos(new IOVersions<>(innerIO), new IOVersions<>(leafIo));

                    newTree.copyMetaInfo(oldInfo);

                    newTree.enableSequentialWriteMode();

                    AtomicBoolean warningPrinted = new AtomicBoolean();

                    treeIterator.iterate(tree, oldCachePageMem, (theTree, io, pageAddr, idx) -> {
                        cancellationChecker.run();

                        if (System.currentTimeMillis() - lastCpLockTs.get() >= cpLockThreshold) {
                            cpLock.checkpointReadUnlock();

                            cpLock.checkpointReadLock();

                            lastCpLockTs.set(System.currentTimeMillis());
                        }

                        assert 1 == io.getVersion()
                            : "IO version " + io.getVersion() + " is not supported by current defragmentation algorithm." +
                            " Please implement copying of tree in a new format.";

                        BPlusIO<H2Row> h2IO = wrap(io);

                        H2Row row = theTree.getRow(h2IO, pageAddr, idx);

                        if (row instanceof H2CacheRowWithIndex) {
                            H2CacheRowWithIndex h2CacheRow = (H2CacheRowWithIndex)row;

                            CacheDataRow cacheDataRow = h2CacheRow.getRow();

                            int partition = cacheDataRow.partition();

                            long link = h2CacheRow.link();

                            LinkMap map = mappingByPartition.get(partition);

                            long newLink = map.get(link);

                            if (newLink == 0L) {
                                if (warningPrinted.compareAndSet(false, true)) {
                                    log.warning(S.toString(
                                        "Broken link found in SQL index. Some entries will be skipped." +
                                            " Please consider rebuilding the index.",
                                        "grpId", grpCtx.groupId(), false,
                                        "name", oldH2Idx.getName(), false,
                                        "segment", finalSegment, false
                                    ));
                                }

                                return true;
                            }

                            H2CacheRowWithIndex newRow = H2CacheRowWithIndex.create(
                                rowDesc,
                                newLink,
                                h2CacheRow,
                                ((H2RowLinkIO)io).storeMvccInfo()
                            );

                            InlineIndexColumnFactory.setCurrentInlineIndexes(newTree.inlineIndexes());

                            assert cctx.shared().database().checkpointLockIsHeldByThread();

                            newTree.putx(newRow);
                        }

                        return true;
                    });
                }
            }

            return true;
        }
        finally {
            cpLock.checkpointReadUnlock();
        }
    }

    /** */
    private static <T extends BPlusIO<H2Row> & H2RowLinkIO> H2Row lookupRow(
        BPlusTree<H2Row, ?> tree,
        long pageAddr,
        int idx,
        T io
    ) throws IgniteCheckedException {
        long link = io.getLink(pageAddr, idx);

        List<InlineIndexColumn> inlineIdxs = ((H2Tree) tree).inlineIndexes();

        int off = io.offset(idx);

        int payloadSize = io.getPayloadSize();

        byte[] values;

        if (inlineIdxs != null)
            values = PageUtils.getBytes(pageAddr, off, payloadSize);
        else
            values = Utils.EMPTY_BYTES;

        if (io.storeMvccInfo()) {
            long mvccCrdVer = io.getMvccCoordinatorVersion(pageAddr, idx);
            long mvccCntr = io.getMvccCounter(pageAddr, idx);
            int mvccOpCntr = io.getMvccOperationCounter(pageAddr, idx);

            H2CacheRow row = (H2CacheRow) ((H2Tree) tree).createMvccRow(link, mvccCrdVer, mvccCntr, mvccOpCntr, CacheDataRowAdapter.RowData.LINK_ONLY);

            return new H2CacheRowWithIndex(row.getDesc(), row.getRow(), values);
        }

        H2CacheRow row = (H2CacheRow) ((H2Tree) tree).createRow(link, false);

        return new H2CacheRowWithIndex(row.getDesc(), row.getRow(), values);
    }

    /** */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BPlusIO<H2Row> wrap(BPlusIO<H2Row> io) {
        assert io instanceof H2RowLinkIO;

        if (io instanceof BPlusInnerIO) {
            assert io instanceof AbstractH2ExtrasInnerIO
                || io instanceof AbstractH2InnerIO;

            return new BPlusInnerIoDelegate((BPlusInnerIO<H2Row>)io);
        }
        else {
            assert io instanceof AbstractH2ExtrasLeafIO
                || io instanceof AbstractH2LeafIO;

            return new BPlusLeafIoDelegate((BPlusLeafIO<H2Row>)io);
        }
    }

    /**
     * Stores the needed info about the row in the page. Overrides {@link BPlusIO#storeByOffset(long, int, Object)}.
     *
     * @param io Page io.
     * @param pageAddr Page address.
     * @param off Data offset.
     * @param row H2 cache row.
     * @param <IO> Type of the Page io.
     */
    private static <IO extends BPlusIO<?> & H2RowLinkIO> void storeByOffset(
        IO io,
        long pageAddr,
        int off,
        H2CacheRowWithIndex row
    ) {
        int payloadSize = io.getPayloadSize();

        assert row.link() != 0;

        PageUtils.putBytes(pageAddr, off, row.values);

        H2IOUtils.storeRow(row, pageAddr, off + payloadSize, io.storeMvccInfo());
    }

    /** */
    private static class BPlusInnerIoDelegate<IO extends BPlusInnerIO<H2Row> & H2RowLinkIO>
        extends BPlusInnerIO<H2Row> implements H2RowLinkIO {
        /** */
        private final IO io;

        /** */
        public BPlusInnerIoDelegate(IO io) {
            super(io.getType(), io.getVersion(), io.canGetRow(), io.getItemSize());
            this.io = io;
        }

        /** {@inheritDoc} */
        @Override public void storeByOffset(long pageAddr, int off, H2Row row) throws IgniteCheckedException {
            assertPageType(pageAddr);

            IndexingDefragmentation.storeByOffset(io, pageAddr, off, (H2CacheRowWithIndex) row);
        }

        /** {@inheritDoc} */
        @Override public void store(long dstPageAddr, int dstIdx, BPlusIO<H2Row> srcIo, long srcPageAddr, int srcIdx)
            throws IgniteCheckedException {
            assertPageType(dstPageAddr);

            io.store(dstPageAddr, dstIdx, srcIo, srcPageAddr, srcIdx);
        }

        /** {@inheritDoc} */
        @Override public H2Row getLookupRow(BPlusTree<H2Row, ?> tree, long pageAddr, int idx)
            throws IgniteCheckedException {
            return lookupRow(tree, pageAddr, idx, this);
        }

        /** {@inheritDoc} */
        @Override public long getLink(long pageAddr, int idx) {
            return io.getLink(pageAddr, idx);
        }

        /** {@inheritDoc} */
        @Override public long getMvccCoordinatorVersion(long pageAddr, int idx) {
            return io.getMvccCoordinatorVersion(pageAddr, idx);
        }

        /** {@inheritDoc} */
        @Override public long getMvccCounter(long pageAddr, int idx) {
            return io.getMvccCounter(pageAddr, idx);
        }

        /** {@inheritDoc} */
        @Override public int getMvccOperationCounter(long pageAddr, int idx) {
            return io.getMvccOperationCounter(pageAddr, idx);
        }

        /** {@inheritDoc} */
        @Override public boolean storeMvccInfo() {
            return io.storeMvccInfo();
        }

        /** {@inheritDoc} */
        @Override public int getPayloadSize() {
            return io.getPayloadSize();
        }
    }

    /** */
    private static class BPlusLeafIoDelegate<IO extends BPlusLeafIO<H2Row> & H2RowLinkIO>
        extends BPlusLeafIO<H2Row> implements H2RowLinkIO {
        /** */
        private final IO io;

        /** */
        public BPlusLeafIoDelegate(IO io) {
            super(io.getType(), io.getVersion(), io.getItemSize());
            this.io = io;
        }

        /** {@inheritDoc} */
        @Override public void storeByOffset(long pageAddr, int off, H2Row row) throws IgniteCheckedException {
            assertPageType(pageAddr);

            IndexingDefragmentation.storeByOffset(io, pageAddr, off, (H2CacheRowWithIndex) row);
        }

        /** {@inheritDoc} */
        @Override public void store(long dstPageAddr, int dstIdx, BPlusIO<H2Row> srcIo, long srcPageAddr, int srcIdx)
            throws IgniteCheckedException {
            assertPageType(dstPageAddr);

            io.store(dstPageAddr, dstIdx, srcIo, srcPageAddr, srcIdx);
        }

        /** {@inheritDoc} */
        @Override public H2Row getLookupRow(BPlusTree<H2Row, ?> tree, long pageAddr, int idx)
            throws IgniteCheckedException {
            return lookupRow(tree, pageAddr, idx, this);
        }

        /** {@inheritDoc} */
        @Override public long getLink(long pageAddr, int idx) {
            return io.getLink(pageAddr, idx);
        }

        /** {@inheritDoc} */
        @Override public long getMvccCoordinatorVersion(long pageAddr, int idx) {
            return io.getMvccCoordinatorVersion(pageAddr, idx);
        }

        /** {@inheritDoc} */
        @Override public long getMvccCounter(long pageAddr, int idx) {
            return io.getMvccCounter(pageAddr, idx);
        }

        /** {@inheritDoc} */
        @Override public int getMvccOperationCounter(long pageAddr, int idx) {
            return io.getMvccOperationCounter(pageAddr, idx);
        }

        /** {@inheritDoc} */
        @Override public boolean storeMvccInfo() {
            return io.storeMvccInfo();
        }

        /** {@inheritDoc} */
        @Override public int getPayloadSize() {
            return io.getPayloadSize();
        }
    }

    /**
     * H2CacheRow with stored index values
     */
    private static class H2CacheRowWithIndex extends H2CacheRow {
        /** Byte array of index values. */
        private final byte[] values;

        /** Constructor. */
        public H2CacheRowWithIndex(GridH2RowDescriptor desc, CacheDataRow row, byte[] values) {
            super(desc, row);
            this.values = values;
        }

        /** */
        public static H2CacheRowWithIndex create(
            GridH2RowDescriptor desc,
            long newLink,
            H2CacheRowWithIndex oldValue,
            boolean storeMvcc
        ) {
            CacheDataRow row = oldValue.getRow();

            CacheDataRow newDataRow;

            if (storeMvcc) {
                newDataRow = new MvccDataRow(newLink);
                newDataRow.mvccVersion(row);
            } else
                newDataRow = new CacheDataRowAdapter(newLink);

            return new H2CacheRowWithIndex(desc, newDataRow, oldValue.values);
        }

        /** {@inheritDoc} */
        @Override public Value getValue(int col) {
            // Value is not needed, byte array of serialized values will be used
            return null;
        }
    }
}
