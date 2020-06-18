package ru.ifmo.rain.lemeshkova.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.io.PrintStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class HelloUDPNonblockingClient extends AbstractHelloClient implements HelloClient {

    static final String ERROR_PREFIX = "HelloUDPNonblockingClient";
    private static final int SELECTOR_TIMEOUT = 250;
    private Selector selector;

    public static void main(String[] args) {
        launch(args, new HelloUDPNonblockingClient(), "HelloUDPNonblockingClient");
    }

    @Override
    public void run(String host, int port, String prefix, int threadCount, int requestCount) {
        setUp(host, port, prefix, threadCount, requestCount);
        execute();
        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                throw error("Cannot close selector", e);
            }
        }
    }

    private void setUp(String host, int port, String prefix, int threadCount, int requestCount) {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            throw error("Cannot setUp selector in HelloUDPNonblockingClient", e);
        }
        try {
            setUpChannels(host, port, prefix, threadCount, requestCount);
        } catch (IOException e) {
            throw error("Cannot setUp channels in HelloUDPNonblockingClient", e);
        }
    }

    private void setUpChannels(String host, int port, String prefix, int threadCount, int requestCount) throws IOException {
        SocketAddress serverSocketAddress;
        try {
            serverSocketAddress = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (UnknownHostException e) {
            throw error("Unknown host address " + host, e);
        }
        for (int i = 0; i < threadCount; i++) {
            DatagramChannel channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            channel.connect(serverSocketAddress);
            HelloClientChannelAttachment attachment = new HelloClientChannelAttachment(prefix.getBytes(HelloUtils.CHARSET), i, requestCount,
                    ByteBuffer.allocate(channel.socket().getReceiveBufferSize()),
                    ByteBuffer.allocate(channel.socket().getSendBufferSize()));
            channel.register(selector, SelectionKey.OP_WRITE, attachment);
        }
    }

    private void execute() {
        try {
            while (!selector.keys().isEmpty() && !Thread.interrupted()) {
                selector.select(SELECTOR_TIMEOUT);
                Set<SelectionKey> keys = selector.selectedKeys();
                if (!keys.isEmpty()) {
                    for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                        final SelectionKey key = i.next();
                        try {
                            if (key.isReadable()) {
                                read(key);
                            }
                            if (key.isValid() && key.isWritable()) {
                                write(key);
                            }
                        } finally {
                            i.remove();
                        }
                    }
                } else {
                    for (SelectionKey key : selector.keys()) {
                        if (key.isWritable()) {
                            write(key);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw error("Error occurs while sending client responses", e);
        }
    }

    private void read(SelectionKey key) throws IOException {
        HelloClientChannelAttachment attachment = (HelloClientChannelAttachment) key.attachment();
        attachment.readResponse((DatagramChannel) key.channel());
        if (!attachment.checkResponseAndCloseChannel(key)) {
            switchKeyOperation(key, SelectionKey.OP_READ, SelectionKey.OP_WRITE);
        }
    }

    private void write(SelectionKey key) throws IOException {
        final HelloClientChannelAttachment attachment = (HelloClientChannelAttachment) key.attachment();
        final DatagramChannel channel = (DatagramChannel) key.channel();
        attachment.sendRequest(channel);
        switchKeyOperation(key, SelectionKey.OP_WRITE, SelectionKey.OP_READ);
    }

    private void switchKeyOperation(SelectionKey key, int operationToRemove, int operationToAdd) {
        key.interestOpsAnd(~operationToRemove);
        key.interestOpsOr(operationToAdd);
    }

    private RuntimeException error(String message, Exception e) {
        return HelloUtils.error(ERROR_PREFIX, message, e);
    }

    private static class HelloClientChannelAttachment {

        private static final byte UNDERSCORE = (byte) '_';
        private static final String ACCEPTED = "\nAccepted";
        private static final String REJECTED = "\nRejected";
        private static final List<String> CONSOLE_MESSAGES = List.of(
                "Send request:",
                " received response: \"",
                "\"\n-----------\n");

        private final byte[] prefixBytes;
        private int requestNum;
        private final int threadNum;
        private final int requestCount;
        private final ByteBuffer receiveBuffer;
        private final ByteBuffer sendBuffer;

        public HelloClientChannelAttachment(byte[] prefixBytes, int threadNum, int requestCount, ByteBuffer receiveBuffer,
                                            ByteBuffer sendBuffer) {
            this.prefixBytes = prefixBytes;
            this.threadNum = threadNum;
            this.requestCount = requestCount;
            this.receiveBuffer = receiveBuffer;
            this.sendBuffer = sendBuffer;
            this.requestNum = 0;
        }

        private void readResponse(DatagramChannel channel) throws IOException {
            receiveBuffer.clear();
            channel.receive(receiveBuffer);
            receiveBuffer.flip();
        }

        private void sendRequest(DatagramChannel channel) throws IOException {
            sendBuffer.clear();
            sendBuffer.put(prefixBytes);
            HelloUtils.putNumber(threadNum, sendBuffer);
            sendBuffer.put(UNDERSCORE);
            HelloUtils.putNumber(requestNum, sendBuffer);
            sendBuffer.flip();
            channel.send(sendBuffer, channel.getRemoteAddress());
        }

        public boolean checkResponseAndCloseChannel(SelectionKey key) throws IOException {
            boolean valid = validateAndDisplayReceivedResult();
            if (valid) {
                requestNum++;
                if (requestNum >= requestCount) {
                    key.channel().close();
                    return true;
                }
            }
            return false;
        }

        private boolean validateAndDisplayReceivedResult() {
            boolean valid = HelloUtils.checkValidByteArrayResponse(receiveBuffer.array(), threadNum, requestNum, receiveBuffer.limit());
            displayResult((valid) ? ACCEPTED : REJECTED);
            return valid;
        }

        private void displayResult(final String result) {
            final PrintStream printStream = System.err;
            printStream.print(CONSOLE_MESSAGES.get(0));
            HelloUtils.printBuffer(printStream, sendBuffer);
            printStream.print(result + CONSOLE_MESSAGES.get(1));
            HelloUtils.printBuffer(printStream, receiveBuffer);
            printStream.print(CONSOLE_MESSAGES.get(2));
        }
    }
}
