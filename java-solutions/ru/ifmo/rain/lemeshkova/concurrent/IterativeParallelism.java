
package ru.ifmo.rain.lemeshkova.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of the {@code AdvancedIP} interface, providing methods for parallel data processing.
 *
 * @author Natalia Lemeshkova
 * @see AdvancedIP
 * @see info.kgeorgiy.java.advanced.concurrent.ListIP
 * @see info.kgeorgiy.java.advanced.concurrent.ScalarIP
 * @see ParallelMapper
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class IterativeParallelism implements AdvancedIP {
    private ParallelMapper mapper;

    /**
     * Default constructor. Creates an instance of {@code IterativeParallelism} that not using {@code ParallelMapper}.
     */
    public IterativeParallelism() {
        mapper = null;
    }

    /**
     * Constructor which ceates an instance of {@code IterativeParallelism}
     * with provided {@code ParallelMapper} using for parallel processing.
     *
     * @param mapper the {@link ParallelMapper}
     */
    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return parallelOperation(threads, values, maxStreamFunction(comparator), maxStreamFunction(comparator));
    }

    /**
     * Return function that get maximum element in stream, using comparator.
     *
     * @param comparator to compare elements in stream
     * @param <T>        type of elements
     * @return function for get max by comparator element in stream
     */
    private <T> Function<Stream<T>, T> maxStreamFunction(Comparator<? super T> comparator) {
        return stream -> stream.max(comparator).get();
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelOperation(threads, values, stream -> stream.allMatch(predicate), stream -> stream.allMatch(Boolean::booleanValue));
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return parallelOperation(threads, values, stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelOperation(threads, values, stream -> stream.filter(predicate),
                stream -> stream.flatMap(Function.identity()).collect(Collectors.toList()));
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return parallelOperation(threads, values, stream -> stream.map(f),
                stream -> stream.flatMap(Function.identity()).collect(Collectors.toList()));
    }

    @Override
    public <T> T reduce(int threads, List<T> values, Monoid<T> monoid) throws InterruptedException {
        return mapReduce(threads, values, Function.identity(), monoid);
    }

    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, Monoid<R> monoid) throws InterruptedException {
        return parallelOperation(threads, values, stream -> stream.map(lift).reduce(monoid.getOperator()).get(),
                stream -> stream.reduce(monoid.getOperator()).get());
    }

    /**
     * Divides the data into blocks for parallel processing, creates and run data processing threads and collect thread execution result.
     * Uses passed functions to process data and collect results.
     * Operations executing using {@link Stream}.
     * If ParallelMapper field {@code mapper} is not null, then using ParallelMapper to execute processes.
     *
     * @param threadCount                     number of threads to separate task. If {@param threads} greater then {@code values.size()}, then create {@code values.size()} threads number
     * @param values                      data for parallel processing
     * @param singleThreadFunction        function to process block of data in single thread
     * @param collectThreadResultFunction function collection results of
     * @param <T>                         type of income data
     * @param <M>                         intermediate type after processing data in one thread
     * @param <R>                         result type (after collect single thread processing data results)
     * @return result of processing data
     * @throws InterruptedException if executing thread was interrupted.
     */
    private <T, M, R> R parallelOperation(int threadCount, List<T> values, Function<Stream<T>, M> singleThreadFunction, Function<Stream<M>, R> collectThreadResultFunction) throws InterruptedException {
        System.out.println("parallelOperation "+ threadCount);
        if (values.size() < threadCount && !values.isEmpty()) threadCount = values.size();
        int averageElementNumber = values.size() / threadCount;
        List<M> results;
        List<Stream<T>> chunks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            chunks.add(values.subList(i * averageElementNumber, (i == threadCount - 1) ? values.size() : (i + 1) * averageElementNumber).stream());
        }
        if (mapper == null) {
            List<Thread> threadsList = new ArrayList<>();
            results = new ArrayList<>(Collections.nCopies(threadCount, null));
            for (int i = 0; i < chunks.size(); i++) {
                int finalI = i;
                threadsList.add(new Thread(() -> {
                    results.set(finalI, singleThreadFunction.apply(chunks.get(finalI)));
                }));
                threadsList.get(i).start();
            }
            joinThreads(threadsList);
        } else {
            results = mapper.map(singleThreadFunction, chunks);
        }
        return collectThreadResultFunction.apply(results.stream());
    }

    /**
     * Join threads produced by  {@link #parallelOperation(int, List, Function, Function)}.
     * If thread was interrupted while joining for the first time, interrupt all threads in {@code threadsList}, and throw {@link InterruptedException}.
     * Following interruptions don't affect joining process.
     *
     * @param threadsList list of threads to join
     * @throws InterruptedException if thread was interrupted while joining
     */
    private void joinThreads(List<Thread> threadsList) throws InterruptedException {
        InterruptedException interruptedException = null;
        for (int i = 0; i < threadsList.size(); ) {
            try {
                threadsList.get(i).join();
                i++;
            } catch (InterruptedException e) {
                if (interruptedException == null) {
                    interruptedException = new InterruptedException("Interrupt thread while joining parallel threadsList");
                    threadsList.forEach(Thread::interrupt);
                }
                interruptedException.addSuppressed(e);
            }
        }
        if (interruptedException != null) {
            throw interruptedException;
        }
    }
}

