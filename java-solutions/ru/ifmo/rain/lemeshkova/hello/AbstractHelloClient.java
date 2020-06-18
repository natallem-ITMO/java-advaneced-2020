package ru.ifmo.rain.lemeshkova.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.util.Objects;

abstract public class AbstractHelloClient implements HelloClient {

    protected static void launch(String[] args, HelloClient helloClient, String className) {
        Objects.requireNonNull(args);
        if (args.length < 5) {
            incorrectUsage(className);
        }
        String host = args[0];
        int port = HelloUtils.parseIntArgument(args, 1, "port number");
        String prefix = args[2];
        int threadCount = HelloUtils.parseIntArgument(args, 3, "number of threads");
        int perThreadRequestCount = HelloUtils.parseIntArgument(args, 4, "number of request for one thread");
        helloClient.run(host, port, prefix, threadCount, perThreadRequestCount);
    }

    private static void incorrectUsage(String className) {
        throw HelloUtils.error("AbstractHelloClient", "Usage: " + className + " <server ip-address> <server port> <request prefix> <thread count> <request count>");
    }

}
