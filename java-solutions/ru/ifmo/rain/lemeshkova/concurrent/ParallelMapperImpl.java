package ru.ifmo.rain.lemeshkova.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class implementing ParallelMapper interface, using to separate data process execution in threads.
 *
 * @author Natalia Lemeshkova
 */
public class ParallelMapperImpl implements ParallelMapper {

    private final Queue<Task> taskQueue;
    private final List<Thread> threadList;
    private boolean closed;

    /**
     * Constructor for {@code ParallelMapperImpl} creates class and run threads, which are ready to execute tasks.
     *
     * @param threadCount number of threads to execute tasks
     */
    public ParallelMapperImpl(int threadCount) {
        this.taskQueue = new LinkedList<>();
        threadList = Stream.generate(() -> new Thread(this::threadRunning)).limit(threadCount).collect(Collectors.toList());
        threadList.forEach(Thread::start);
        closed = false;
    }

    private void threadRunning() {
        try {
            while (!Thread.interrupted() && !closed) {
                takeAndDoTask();
            }
        } catch (InterruptedException ignored) {
        }
    }

    private void takeAndDoTask() throws InterruptedException {
        Task task;
        synchronized (taskQueue) {
            while (taskQueue.isEmpty()) {
                taskQueue.wait();
            }
            task = taskQueue.poll();
        }
        task.doTask();
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        if (closed) return null;
        List<R> result = new ArrayList<>(Collections.nCopies(args.size(), null));
        TaskManager manager = new TaskManager(args.size());
        RuntimeException executionException = new RuntimeException("Exception(s) during execution task");
        for (int i = 0; i < args.size(); i++) {
            int finalI = i;
            Runnable runnable = () -> {
                R apply = f.apply(args.get(finalI));
                synchronized (result) {
                    result.set(finalI, apply);
                }
            };
            Task t = new Task(runnable, manager, executionException);
            synchronized (taskQueue) {
                taskQueue.add(t);
                taskQueue.notify();
            }
        }
        manager.finishTask();
        if (executionException.getSuppressed().length != 0) {
            throw executionException;
        }
        return result;
    }

    @Override
    public void close() {
        closed = true;
        threadList.forEach(Thread::interrupt);
        for (int i = 0; i < threadList.size(); ) {
            try {
                threadList.get(i).join();
                i++;
            } catch (InterruptedException ignored) {
            }
        }
        taskQueue.forEach(Task::killTask);
    }

    private static class TaskManager {
        int value = 0;
        private final int taskSize;

        private TaskManager(int taskSize) {
            this.taskSize = taskSize;
        }

        private synchronized void finishTask() throws InterruptedException {
            if (value != taskSize) {
                wait();
            }
        }

        private synchronized void taskIsDone() {
            value++;
            if (value == taskSize) notify();
        }
    }

    private static class Task {
        Runnable runnable;
        final RuntimeException runtimeException;
        final TaskManager manager;

        private Task(Runnable runnable, TaskManager counter, RuntimeException runtimeException) {
            this.runnable = runnable;
            this.runtimeException = runtimeException;
            this.manager = counter;
        }

        private void doTask() {
            try {
                runnable.run();
            } catch (RuntimeException ex) {
                synchronized (runtimeException) {
                    runtimeException.addSuppressed(ex);
                }
            } finally {
                manager.taskIsDone();
            }
        }

        private void killTask() {
            manager.taskIsDone();
        }
    }
}
