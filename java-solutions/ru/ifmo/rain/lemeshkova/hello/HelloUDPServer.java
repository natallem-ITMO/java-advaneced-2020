package ru.ifmo.rain.lemeshkova.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.*;

public class HelloUDPServer implements HelloServer {
    private boolean closed = false;
    private DatagramSocket socket = null;
    private ExecutorService senders;
    private ExecutorService listener;

    public static void main(String[] args) {
        if (args.length < 2 || args[0] == null || args[1] == null) {
            System.err.format("Incorrect number of arguments.%nUsage: HelloUDPServer <port-count> <thread-count>%n");
            return;
        }
        int[] argsInt = new int[2];
        for (int i = 0; i < 2; i++) {
            try {
                argsInt[i] = Integer.parseInt(args[i]);
            } catch (NumberFormatException ex) {
                System.err.format("%d argument is not a number. Cannot parse %s as integer%n", i, args[i]);
            }
        }
        new HelloUDPServer().start(argsInt[0], argsInt[1]);
    }

    @Override
    public void start(int port, int threads) {
        try {
            if (socket != null) {
                System.out.println("This HelloServer is already in use, connected to port " + socket.getPort());
                return;
            }
            socket = new DatagramSocket(port);
            senders = new ThreadPoolExecutor(
                    threads,
                    threads,
                    0,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    new ThreadPoolExecutor.DiscardPolicy());
            listener = Executors.newSingleThreadExecutor();
            listener.submit(this::listenerThreadFunction);
        } catch (SocketException e) {
            showExceptionMessageIfNotClosed("Cannot open server socket on port " + port, e);
        }
    }

    @Override
    public void close() {
        closed = true;
        listener.shutdown();
        senders.shutdown();
        while (true) {
            try {
                senders.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                break;
            } catch (InterruptedException ignored) {
            }
        }
        socket.close();
    }

    private void listenerThreadFunction() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] buf = new byte[socket.getReceiveBufferSize()];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                senders.submit(() -> processRequestFunction(packet));
            } catch (SocketException e) {
                showExceptionMessageIfNotClosed("Cannot get default buffer size: ", e);
            } catch (IOException e) {
                showExceptionMessageIfNotClosed("Error while receiving message", e);
            }
        }
    }

    private void processRequestFunction(DatagramPacket packet) {
        if (!closed) {
            try {
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                byte[] answer = ("Hello, " + receivedMessage).getBytes();
                packet = new DatagramPacket(answer, answer.length, packet.getAddress(), packet.getPort());
                socket.send(packet);
            } catch (IOException ex) {
                showExceptionMessageIfNotClosed("Error while sending answer: ", ex);
            }
        }
    }

    private void showExceptionMessageIfNotClosed(String message, Exception ex) {
        if (!closed) {
            System.out.println(message + ex.getMessage());
        }
    }
}
