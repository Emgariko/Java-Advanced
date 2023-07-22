# java-advanced
ITMO CT Java Advanced course

List of HW's:
1. [**File walker**](java-solutions/info/kgeorgiy/ja/garipov/walk/)
   - File walker that calculates the hash sums of files in directories using PJW hash function
2. [**ArraySet**](java-solutions/info/kgeorgiy/ja/garipov/arrayset/)
   - Class implements NavigableSet interface using ArrayList to store elements
   - All operations on sets are performed with the highest possible asymptotic efficiency
3. [**StudentDB**](java-solutions/info/kgeorgiy/ja/garipov/student/)
   - Implemented a StudentDB class that performs search in students database using ``Java Stream API``
4. [**Implementor**](java-solutions/info/kgeorgiy/ja/garipov/implementor/)
   - Implementor class that generates implementations of classes and interfaces using ``Java Reflection``
5. [**Iterative Parallelism**](java-solutions/info/kgeorgiy/ja/garipov/concurrent/IterativeParallelism.java)
   - IterativeParallelism class processes lists in multiple threads.
   - Supported opeations:
     - ``minimum(threads, list, comparator)``, ``maximum(threads, list, comparator)``,  ``all(threads, list, predicate)``, ``any(threads, list, predicate)``,  ``filter(threads, list, predicate)``, ``map(threads, list, function)``, ``join(threads, list)``
6. [**Parallel Mapper**](java-solutions/info/kgeorgiy/ja/garipov/concurrent/ParallelMapperImpl.java)
   - ParallelMapper class allows to run function ``f`` call in parallel on every specified argument
   - ParallelMapper creates ``n`` threads to perform runs in parallel 
   - Tasks are stored in queue and performed in FIFO order
7. [**HelloUDP client and server**](java-solutions/info/kgeorgiy/ja/garipov/hello/)
   - Simple client and server that communicate over UDP
   - ``HelloUDPClient`` simultaneously sends requests using specified count of threads
   - ``HelloUDPServer`` accepts requests sent by client and responds to them
8. [**Nonblocking HelloUDP client and server**](java-solutions/info/kgeorgiy/ja/garipov/hello/)
   - Same as HelloUDP, but only using nonblocking input/output
   - Implemented using ``java.nio.*``
