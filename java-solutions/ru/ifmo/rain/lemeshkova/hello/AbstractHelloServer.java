package ru.ifmo.rain.lemeshkova.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class AbstractHelloServer implements HelloServer {

    protected static final String SERVER_PREFIX = "Hello, ";
    private static final String ERROR_PREFIX = "AbstractHelloServer";

    protected ExecutorService listener;
    protected ExecutorService workers;

    @Override
    public void start(int port, int threadCount) {
        setUp(port, threadCount);
        setUpThreads(threadCount);
        listener.submit(this::listen);
    }

    protected static void launch(String[] args, HelloServer helloServer, String className) {
        Objects.requireNonNull(args);
        if (args.length < 2 || args[0] == null || args[1] == null) {
            throw HelloUtils.error(ERROR_PREFIX, String.format("Incorrect number of arguments.%nUsage: " + className + " <port-count> <thread-count>%n"));
        }
        helloServer.start(HelloUtils.parseIntArgument(args, 0, "port-count"), HelloUtils.parseIntArgument(args, 1, "thread-count"));
    }

    private void setUpThreads(int threadCount) {
        workers = Executors.newFixedThreadPool(threadCount);
        listener = Executors.newSingleThreadExecutor();
        listener.submit(this::listen);
    }

    abstract protected void listen();

    abstract void setUp(int port, int threadCount);

    void stopWorkersAndListener() {
        if (listener != null) stopExecutorService(listener);
        if (workers != null) stopExecutorService(workers);
    }

    void stopExecutorService(ExecutorService service) {
        service.shutdown();
        while (true) {
            try {
                service.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                break;
            } catch (InterruptedException ignored) {
            }
        }
    }
}
