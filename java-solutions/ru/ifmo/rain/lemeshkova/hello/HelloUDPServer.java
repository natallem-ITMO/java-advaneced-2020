package ru.ifmo.rain.lemeshkova.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class HelloUDPServer extends AbstractHelloServer implements HelloServer {

    private boolean closed;
    private DatagramSocket socket;

    public static void main(String[] args) {
        launch(args, new HelloUDPServer(), "HelloUDPServer");
    }

    @Override
    protected void listen() {
        while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
            try {
                byte[] buf = new byte[socket.getReceiveBufferSize()];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                workers.submit(() -> processRequest(packet));
            } catch (SocketException e) {
                showExceptionMessageIfNotClosed("Cannot get default buffer size: ", e);
            } catch (IOException e) {
                showExceptionMessageIfNotClosed("Error while receiving message", e);
            }
        }
    }

    @Override
    void setUp(int port, int threadCount) {
        try {
            if (socket != null) {
                System.out.println("This HelloServer is already in use, connected to port " + socket.getPort());
                return;
            }
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw HelloUtils.error("HelloUDPServer", "Cannot open server socket on port " + port, e);
        }
    }

    @Override
    public void close() {
        if (socket == null) {
            return;
        }
        closed = true;
        socket.close();
        stopWorkersAndListener();
    }

    private void processRequest(DatagramPacket packet) {
        if (!closed) {
            try {
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                byte[] answer = (SERVER_PREFIX + receivedMessage).getBytes(HelloUtils.CHARSET);
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
