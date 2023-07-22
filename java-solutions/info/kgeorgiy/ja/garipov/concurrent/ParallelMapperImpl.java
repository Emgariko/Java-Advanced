package info.kgeorgiy.ja.garipov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> threads = new ArrayList<>();
    private final TaskQueue tasks = new TaskQueue();

    private static class RefillableResultList<R> {
        private final List<R> result;
        private int remaining;
        private RuntimeException catchedException = null;

        public RefillableResultList(final int size) {
            result = new ArrayList<>(Collections.nCopies(size, null));
            remaining = size;
        }

        public synchronized void set(final int index, final R value) {
            result.set(index, value);
            finished();
        }

        public synchronized void catchException(final RuntimeException e) {
            if (catchedException != null) {
                catchedException.addSuppressed(e);
            } else {
                catchedException = e;
            }
            finished();
        }

        public synchronized List<R> getResultList() throws InterruptedException {
            while (remaining != 0) {
                wait();
            }
            if (catchedException != null) {
                throw catchedException;
            } else {
                return result;
            }
        }

        private <T> List<Runnable> getTasks(final Function<? super T, ? extends R> f, final List<? extends T> args) {
            return IntStream.range(0, args.size()).<Runnable>mapToObj(i -> () -> {
                try {
                    set(i, f.apply(args.get(i)));
                } catch (final RuntimeException e) {
                    catchException(e);
                }
            }).collect(Collectors.toList());
        }

        private synchronized void finished() {
            if (--remaining == 0) {
                notify();
            }
        }
    }

    private static final class TaskQueue {
        private final Queue<Runnable> queue = new ArrayDeque<>();

        public synchronized Runnable poll() throws InterruptedException {
            while (queue.isEmpty()) {
                wait();
            }
            return queue.poll();
        }

        public synchronized void addAll(final List<Runnable> tasks) {
            queue.addAll(tasks);
            notifyAll();
        }
    }

    public ParallelMapperImpl(final int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("Thread's count should be positive");
        }

        final Runnable task = () -> {
            while (!Thread.interrupted()) {
                try {
                    tasks.poll().run();
                } catch (final InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
        };

        // :NOTE: Stream
        for (int i = 0; i < threads; i++) {
            final Thread thread = new Thread(task);
            this.threads.add(thread);
            this.threads.get(i).start();
        }
    }

    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args) throws InterruptedException {
        final RefillableResultList<R> result = new RefillableResultList<>(args.size());
        tasks.addAll(result.getTasks(f, args));
        return result.getResultList();
    }


    // :NOTE: "Подвисшие" клиенты
    @Override
    public void close() {
        threads.forEach(Thread::interrupt);
        try {
            IterativeParallelism.join(threads);
        } catch (final InterruptedException ignored) {
        }
    }
}
