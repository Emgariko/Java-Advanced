package info.kgeorgiy.ja.garipov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IterativeParallelism implements AdvancedIP {

    private final ParallelMapper parallelMapper;

    public IterativeParallelism() {
        parallelMapper = null;
    }

    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    private <T> List<Stream<? extends T>> buildBuckets(int threads, List<? extends T> values) {
        if (threads < 1) {
            throw new IllegalArgumentException("Thread's count should be positive");
        }
        // :NOTE: DBZ
        final int size = values.size();
        if (size == 0) {
            return Collections.emptyList();
        }

        final int threadsExactly = Math.min(threads, size);
        final int valuesPerThread = size / threadsExactly;
        final int remainingObjects = size % threadsExactly;

        return IntStream.range(0, threadsExactly).mapToObj(i -> {
            final int l = valuesPerThread * i + Math.min(remainingObjects, i);
            final int r = l + valuesPerThread + ((i < (size % threadsExactly)) ? 1 : 0);
            return values.subList(l, r).stream(); })
                .collect(Collectors.toList());
    }

    private <T, R> List<R> executeIterative(final int threads, final List<? extends T> values,
                                            final List<Stream<? extends T>> buckets,
                                            final Function<Stream<? extends T>, R> executor) throws InterruptedException {
        final int threadsExactly = Math.min(threads, values.size());
        final List<Thread> myThreads = new ArrayList<>();

        final List<R> threadsResults = new ArrayList<>(Collections.nCopies(threadsExactly, null));
        for (int i = 0; i < buckets.size(); i++) {
            int finalI = i;
            Thread thread = new Thread(() -> threadsResults.set(finalI, executor.apply(buckets.get(finalI))));
            myThreads.add(thread);
            thread.start();
        }
        join(myThreads);
        return threadsResults;
    }

    private <T, R> R executeParallel(final int threads, final List<? extends T> values,
                                     final Function<Stream<? extends T>, R> executor,
                                     final Function<Stream<R>, R> resultCalculator) throws InterruptedException {
        List <R> res;
        final List<Stream<? extends T>> buckets = buildBuckets(threads, values);
        if (parallelMapper == null) {
            res = executeIterative(threads, values, buckets, executor);
        } else {
            res = parallelMapper.map(executor, buckets);
        }
        return resultCalculator.apply(res.stream());
    }

    public static void join(final List<Thread> myThreads) throws InterruptedException {
        for (int i = 0; i < myThreads.size(); i++) {
            try {
                myThreads.get(i).join();
            } catch (final InterruptedException e1) {
                for (int j = i; j < myThreads.size(); j++) {
                    myThreads.get(j).interrupt();
                }
                for (int j = i; j < myThreads.size(); ) {
                    try {
                        myThreads.get(j).join();
                        j++;
                    } catch (final InterruptedException e2) {
                        e1.addSuppressed(e2);
                    }
                }
                throw e1;
            }
        }
    }

    @Override
    public <T> T reduce(final int threads, final List<T> values, final Monoid<T> monoid) throws InterruptedException {
        // :NOTE: Дубли
        return mapReduce(threads, values, Function.identity(), monoid);
    }

    @Override
    public <T, R> R mapReduce(final int threads, final List<T> values, final Function<T, R> lift, final Monoid<R> monoid) throws InterruptedException {
        Function<Stream<R>, R> reduce = stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator(), monoid.getOperator());
        return executeParallel(threads,
                values,
                stream -> reduce.apply(stream.map(lift)),
                reduce);
    }

    @Override
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return executeParallel(threads,
                values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    private <T, R> List<R> applyAndCollectToList(
            final int threads,
            final List<? extends T> values,
            final Function<Stream<? extends T>, Stream<? extends R>> func
    ) throws InterruptedException {
        return executeParallel(threads,
                values,
                stream -> func.apply(stream).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return applyAndCollectToList(threads, values, stream -> stream.filter(predicate));
    }

    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f) throws InterruptedException {
        return applyAndCollectToList(threads, values, stream -> stream.map(f));
    }

    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return executeParallel(threads,
                values,
                stream -> stream.max(comparator).orElse(null),
                stream -> stream.max(comparator).orElse(null));
    }

    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return !any(threads, values, predicate.negate());
    }

    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return executeParallel(threads, values,
                stream -> stream.anyMatch(predicate),
                stream -> stream.reduce(false, Boolean::logicalOr));
    }
}
