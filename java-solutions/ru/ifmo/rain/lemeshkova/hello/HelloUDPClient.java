package ru.ifmo.rain.lemeshkova.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPClient implements HelloClient {

    public static void main(String[] args) {
        if (args.length < 5) {
            showUsage();
            return;
        }
        try {
            String host = args[0];
            int port = parseIntArgument(args, 1, "port number");
            String prefix = args[2];
            int threadCount = parseIntArgument(args, 3, "number of threads");
            int perThreadRequestCount = parseIntArgument(args, 4, "number of request for one thread");
            new HelloUDPClient().run(host, port, prefix, threadCount, perThreadRequestCount);
        } catch (NumberFormatException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private static void showUsage() {
        System.out.printf(
                "Usage: HelloUDPClient <server ip-address> <server port> <request prefix> <thread count> <request count>%n");
    }

    private static int parseIntArgument(String[] args, int i, String expected) {
        try {
            int result = Integer.parseInt(args[i]);
            if (result <= 0) throw new NumberFormatException(String.format(
                    "Invalid argument №%d. Expected positive integer for %s, found %d", i + 1, expected, result));
            return result;
        } catch (NumberFormatException ex) {
            throw new NumberFormatException(String.format(
                    "Invalid argument №%d. Expected %s, found %s", i + 1, expected, args[i]));
        }
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        ExecutorService threadPool = Executors.newFixedThreadPool(threads);
        SocketAddress serverSocketAddress;
        try {
            serverSocketAddress = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Error. Unknown host address"+host);
        }

        for (int i = 0; i < threads; i++) {
            int finalI = i;
            threadPool.submit(() -> doRequest(prefix, finalI, requests, serverSocketAddress));
        }

        threadPool.shutdown();
        try {
            threadPool.awaitTermination(threads * requests, TimeUnit.MINUTES);
        } catch (InterruptedException ignored) {
        }
    }

    private void doRequest(String requestMessage, int threadNumber, int requestCount, SocketAddress serverSocketAddress) {
        try (DatagramSocket socket = new DatagramSocket()) {
            final int TIMEOUT_MILLIS = 100;
            socket.setSoTimeout(TIMEOUT_MILLIS);
            for (int i = 0; i < requestCount; i++) {
                String requestString = String.format("%s%d_%d", requestMessage, threadNumber, i);
                byte[] requestBuffer = requestString.getBytes(StandardCharsets.UTF_8);
                DatagramPacket requestPacket = new DatagramPacket(requestBuffer, requestBuffer.length, serverSocketAddress);

                byte[] responseBuffer = new byte[socket.getReceiveBufferSize()];
                DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);

                while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                    try {
                        socket.send(requestPacket);
                        try {
                            socket.receive(responsePacket);
                            String response = getString(responsePacket);
                            boolean valid = validateAndShowSentResult(requestString, response, threadNumber, i);
                            if (valid) {
                                break;
                            }
                        } catch (IOException ex) {
                            System.err.printf("Error while receiving response for request \"%s\": %s%n", requestString, ex.getMessage());
                        }
                    } catch (IOException ex) {
                        System.err.printf("Error while sending request: %s%nRetrying", ex.getMessage());
                    }
                }
            }
        } catch (SocketException ex) {
            System.err.println("Cannot open socket to send requests: " + ex.getMessage());
        }
    }

    private boolean validateAndShowSentResult(String request, String response, int threadNumber, int requestNumber) {
        boolean valid = response.matches("[\\D]*" + threadNumber + "[\\D]*" + requestNumber + "[\\D]*");
        String action = (valid) ? "accepted" : "rejected";
        System.err.printf("Send request: \"%s\"%n" +
                "Received and %s response: \"%s\"%n" +
                "-----------%n", request, action, response);
        return valid;
    }

    private static String getString(DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }
}
