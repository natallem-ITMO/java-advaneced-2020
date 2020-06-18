package ru.ifmo.rain.lemeshkova.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPClient extends AbstractHelloClient implements HelloClient {

    private static final String ERROR_PREFIX = "HelloUDPClient";

    public static void main(String[] args) {
        launch(args, new HelloUDPClient(), "HelloUDPClient");
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        ExecutorService threadPool = Executors.newFixedThreadPool(threads);
        SocketAddress serverSocketAddress;
        try {
            serverSocketAddress = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (UnknownHostException e) {
            throw HelloUtils.error(ERROR_PREFIX, "Error. Unknown host address" + host, e);
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
                String requestString = requestMessage + threadNumber + "_" + i;
                byte[] requestBuffer = requestString.getBytes(HelloUtils.CHARSET);
                DatagramPacket requestPacket = new DatagramPacket(requestBuffer, requestBuffer.length, serverSocketAddress);

                byte[] responseBuffer = new byte[socket.getReceiveBufferSize()];
                DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);

                while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                    try {
                        socket.send(requestPacket);
                        try {
                            socket.receive(responsePacket);
                            String response = getString(responsePacket);
                            boolean valid = validateAndShowSentResult(responsePacket, response, threadNumber, i);
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
            throw HelloUtils.error(ERROR_PREFIX, "Cannot open socket to send requests", ex);
        }
    }

    private boolean validateAndShowSentResult(DatagramPacket request, String response, int threadNumber, int requestNumber) {
        boolean valid = HelloUtils.checkValidByteArrayResponse(request.getData(), threadNumber, requestNumber, request.getLength());
        String action = (valid) ? "accepted" : "rejected";
        System.err.printf("Send request: \"%s\"%n" +
                "Received and %s response: \"%s\"%n" +
                "-----------%n", getString(request), action, response);
        return valid;
    }

    private static String getString(DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), HelloUtils.CHARSET);
    }
}
