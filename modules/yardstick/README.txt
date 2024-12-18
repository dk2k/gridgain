Yardstick GridGain Benchmarks
===========================
GridGain benchmarks are written on top of Yardstick Framework (https://github.com/gridgain/yardstick)
and allow you to measure performance of various GridGain components and modules.

The documentation below describes how to execute and configure already assembled benchmarks. If you need to add new
benchmarks or build existing one then refer to instruction from `DEVNOTES.txt` in source directory.

Visit Yardstick Repository (https://github.com/gridgain/yardstick) for detailed information in regards resulting
graphs generation and how the frameworks works.


Running GridGain Benchmarks Locally
=================================

The simplest way to start with benchmarking is to use one of the executable scripts available under `benchmarks\bin`
directory:

./bin/benchmark-run-all.sh config/benchmark-sample.properties

The command above will benchmark the cache put operation for a distributed atomic cache. The results of the
benchmark will be added to an auto-generated `output/results-{DATE-TIME}` directory.

If `./bin/benchmark-run-all.sh` command is executed as is without any parameters and modifications in configurations
files then all the available benchmarks will be executed on a local machine using `config/benchmark.properties` configuration.

In case of any issue refer to the logs that are added to an auto-generated
`output/logs-{DATE-TIME}` directory.

To get more information about available benchmarks and configuration parameters refer
to “Provided Benchmarks” and “Properties And Command Line Arguments” sections below.


Running GridGain Benchmarks Remotely
==================================

To benchmark GridGain across several remote hosts the following steps need
to be done:

1. Go to `config/ignite-remote-config.xml` and replace
`<value>127.0.0.1:47500..47509</value>` with actual IPs of all the remote hosts.
Refer to the documentation section below if you prefer to use other kind of IP finder:
https://www.gridgain.com/docs/latest/developers-guide/clustering/clustering

2. Go to `config/benchmark-remote-sample.properties` and replace the `localhost` with
actual IPs of the remote hosts in the following places:
SERVERS='localhost,localhost'
DRIVERS='localhost'

DRIVERS is the remote hosts where benchmarks execution will be and driven.

Replace `localhost` occurrences in the same places in
`config/benchmark-remote.properties` files if you plan to execute a full set of
benchmarks available.

3. Upload GridGain Yardstick Benchmarks to one of your DRIVERS host in its own working directory.

4. Log in on the remote host that will be the DRIVER and execute the following command:

./bin/benchmark-run-all.sh config/benchmark-remote-sample.properties

By default, all the necessary files will be automatically uploaded from the host in which you run the command above to
every other host to the same path. If you prefer to do it manually set the AUTO_COPY variable in property file to `false`.

The command above will benchmark the cache put operation for a distributed atomic cache. The results of the benchmark
will be added to an auto-generated `output/results-{DATE-TIME}` directory.

If you want to execute all the available benchmarks across the remote hosts then
execute the following command on the DRIVER side:
./bin/benchmark-run-all.sh config/benchmark-remote.properties

5. If you use TcpDiscoverySpi in your IgniteConfiguration use AUTOSET_DISCOVERY_VM_IP_FINDER=true property to enable replacing
addresses from SERVER_HOSTS property to IpFinder configuration. That way you can leave default values '127.0.0.1' in
Ignite configuration files as is and those values will be replaced with actual addresses. Use PORT_RANGE property to set
port range for host addresses.

Provided Benchmarks
===================
The following benchmarks are provided:

1.  `GetBenchmark` - benchmarks atomic distributed cache get operation
2.  `PutBenchmark` - benchmarks atomic distributed cache put operation
3.  `PutGetBenchmark` - benchmarks atomic distributed cache put and get operations together
4.  `PutTxBenchmark` - benchmarks transactional distributed cache put operation
5.  `PutGetTxBenchmark` - benchmarks transactional distributed cache put and get operations together
6.  `SqlQueryBenchmark` - benchmarks distributed SQL query over cached data
7.  `SqlQueryJoinBenchmark` - benchmarks distributed SQL query with a Join over cached data
8.  `SqlQueryPutBenchmark` - benchmarks distributed SQL query with simultaneous cache updates
9.  `AffinityCallBenchmark` - benchmarks affinity call operation
10. `ApplyBenchmark` - benchmarks apply operation
11. `BroadcastBenchmark` - benchmarks broadcast operations
12. `ExecuteBenchmark` - benchmarks execute operations
13. `RunBenchmark` - benchmarks running task operations
14. `PutGetOffHeapBenchmark` - benchmarks atomic distributed cache put and get operations together off heap
15. `PutGetOffHeapValuesBenchmark` - benchmarks atomic distributed cache put value operations off heap
16. `PutOffHeapBenchmark` - benchmarks atomic distributed cache put operations off heap
17. `PutOffHeapValuesBenchmark` - benchmarks atomic distributed cache get value operations off heap
18. `PutTxOffHeapBenchmark` - benchmarks transactional distributed cache put operation off heap
19. `PutTxOffHeapValuesBenchmark` - benchmarks transactional distributed cache put value operation off heap
20. `SqlQueryOffHeapBenchmark` -benchmarks distributed SQL query over cached data off heap
21. `SqlQueryJoinOffHeapBenchmark` - benchmarks distributed SQL query with a Join over cached data off heap
22. `SqlQueryPutOffHeapBenchmark` - benchmarks distributed SQL query with simultaneous cache updates off heap
23. `PutAllBenchmark` - benchmarks atomic distributed cache batch put operation
24. `PutAllTxBenchmark` - benchmarks transactional distributed cache batch put operation
25. `IgnitePutGetWithPageReplacements` - benchmark atomic cache put with active page replacement
26. `IgniteScanQueryGetAllBenchmark` - benchmarks distributed ScanQuery
27. `IgniteTextQueryGetAllBenchmark` - benchmarks distributed TextQuery
28. `IgniteIndexQueryGetAllBenchmark` - benchmarks distributed IndexQuery.

Properties And Command Line Arguments
=====================================
Note that this section only describes configuration parameters specific to GridGain benchmarks, and not for Yardstick
framework. To run GridGain benchmarks and generate graphs, you will need to run them using Yardstick framework scripts in
`bin` folder.

Refer to Yardstick Documentation (https://github.com/gridgain/yardstick) for common Yardstick properties
and command line arguments for running Yardstick scripts.

The following Ignite benchmark properties can be defined in the benchmark configuration:

* `-b <num>` or `--backups <num>` - Number of backups for every key
* `-cfg <path>` or `--Config <path>` - Path to GridGain configuration file
* `-cs` or `--cacheStore` - Enable or disable cache store readThrough, writeThrough
* `-cl` or `--client` - Client flag. Use this flag if you running more than one `DRIVER`, otherwise additional drivers
would behave like a `servers`.
* `-nc` or `--nearCache` - Near cache flag
* `-nn <num>` or `--nodeNumber <num>` - Number of nodes (automatically set in `benchmark.properties`), used to wait for
    the specified number of nodes to start
* `-sm <mode>` or `-syncMode <mode>` - Synchronization mode (defined in `CacheWriteSynchronizationMode`)
* `-r <num>` or `--range` - Range of keys that are randomly generated for cache operations
* `-rd or --restartdelay` - Restart delay in seconds
* `-rs or --restartsleep` - Restart sleep in seconds
* `-rth <host>` or `--restHost <host>` - REST TCP host
* `-rtp <num>` or `--restPort <num>` - REST TCP port, indicates that a GridGain node is ready to process GridGain Clients
* `-ss` or `--syncSend` - Flag indicating whether synchronous send is used in `TcpCommunicationSpi`
* `-txc <value>` or `--txConcurrency <value>` - Cache transaction concurrency control, either `OPTIMISTIC` or
    `PESSIMISTIC` (defined in `CacheTxConcurrency`)
* `-txi <value>` or `--txIsolation <value>` - Cache transaction isolation (defined in `CacheTxIsolation`)
* `-wb` or `--writeBehind` - Enable or disable writeBehind for cache store

For example if we need to run 2 `IgniteNode` servers on localhost with `PutBenchmark` benchmark on localhost,
with number of backups set to 1, synchronization mode set to `PRIMARY_SYNC`, then the following configuration
should be specified in `benchmark.properties` file:

```
SERVER_HOSTS=localhost,localhost
...

Note that -dn and -sn, which stand for data node and server node, are native Yardstick parameters and are documented in Yardstick framework.
===========================================================================================================================================

CONFIGS="-b 1 -sm PRIMARY_SYNC -dn PutBenchmark`IgniteNode"
```

Issues
======
Use Ignite Apache JIRA (https://issues.apache.org/jira/browse/IGNITE) to file bugs.

License
=======
Yardstick Ignite is available under Apache 2.0 (http://www.apache.org/licenses/LICENSE-2.0.html) Open Source license.
