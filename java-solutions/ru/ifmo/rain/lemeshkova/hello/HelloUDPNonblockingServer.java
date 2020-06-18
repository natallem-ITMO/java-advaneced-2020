package ru.ifmo.rain.lemeshkova.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class HelloUDPNonblockingServer extends AbstractHelloServer implements HelloServer {

    private static final String ERROR_PREFIX = "HelloUDPNonblockingServer";
    private DatagramChannel channel;
    private Selector selector;
    private static final byte[] PREFIX = SERVER_PREFIX.getBytes(HelloUtils.CHARSET);

    public static void main(String[] args) {
        launch(args, new HelloUDPNonblockingServer(), "HelloUDPNonblockingServer");
    }

    @Override
    public void close() {
        try {
            if (channel != null) channel.close();
            if (selector != null) selector.close();
        } catch (IOException e) {
            throw error("Cannot close channel or selector", e);
        }
        stopWorkersAndListener();
    }

    @Override
    protected void setUp(int port, int threadCount) {
        try {
            setUpChannel(port);
        } catch (IOException e) {
            throw error("Cannot setUp channel in HelloUDPNonblockingServer", e);
        }
        try {
            setUpSelector(threadCount);
        } catch (IOException e) {
            close();
            throw error("Cannot setUp selector in HelloUDPNonblockingServer", e);
        }
    }

    private void setUpChannel(int port) throws IOException {
        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        channel.socket().bind(new InetSocketAddress(port));
    }

    private void setUpSelector(int threadCount) throws IOException {
        selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ, new Context(threadCount, channel.socket().getSendBufferSize()));
    }

    @Override
    protected void listen() {
        try {
            while (!channel.isConnected() && !Thread.currentThread().isInterrupted()) {
                selector.select();
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
            }
        } catch (IOException e) {
            throw error("I/O Error while listening", e);
        }
    }

    private RuntimeException error(String s, IOException e) {
        throw HelloUtils.error(ERROR_PREFIX, s, e);
    }

    private void read(SelectionKey key) throws IOException {
        Context context = (Context) key.attachment();
        AddressByteBuffer buffer = context.getFreeBuffer(key);
        buffer.readFrom((DatagramChannel) key.channel());
        workers.submit(() -> {
            buffer.processMessage();
            context.addToFullBufferQueue(buffer, key);
        });
    }

    private void write(SelectionKey key) throws IOException {
        Context context = (Context) key.attachment();
        AddressByteBuffer buffer = context.getFullBuffer(key);
        buffer.sendTo(((DatagramChannel) key.channel()));
        context.addToFreeBufferQueue(buffer, key);
    }


    private static class AddressByteBuffer {

        private final ByteBuffer buffer;
        private SocketAddress address = null;

        public AddressByteBuffer(int bufferSize) {
            this.buffer = ByteBuffer.allocate(bufferSize);
        }

        private void sendTo(DatagramChannel channel) throws IOException {
            channel.send(buffer, address);
        }

        private void readFrom(DatagramChannel datagramChannel) throws IOException {
            buffer.clear();
            SocketAddress remoteAddress = datagramChannel.receive(buffer);
            buffer.flip();
            address = remoteAddress;
        }

        private void processMessage() {
            final byte[] bufferArray = buffer.array();
            HelloUtils.shiftData(bufferArray, PREFIX.length, buffer.limit());
            HelloUtils.addPrefix(bufferArray, PREFIX);
            buffer.limit(buffer.limit() + PREFIX.length);
        }
    }

    private class Context {

        private final Queue<AddressByteBuffer> fullBufferQueue = new LinkedList<>();
        private final Queue<AddressByteBuffer> freeBufferQueue = new LinkedList<>();

        public Context(int bufferCount, int bufferSize) {
            for (int i = 0; i < bufferCount; i++) {
                freeBufferQueue.add(new AddressByteBuffer(bufferSize));
            }
        }

        private synchronized void addToFullBufferQueue(AddressByteBuffer buffer, SelectionKey key) {
            addTo(fullBufferQueue, buffer, SelectionKey.OP_WRITE, key);
        }

        private synchronized void addToFreeBufferQueue(AddressByteBuffer buffer, SelectionKey key) {
            addTo(freeBufferQueue, buffer, SelectionKey.OP_READ, key);
        }

        private synchronized AddressByteBuffer getFreeBuffer(SelectionKey key) {
            return getFrom(freeBufferQueue, SelectionKey.OP_READ, key);
        }

        private synchronized AddressByteBuffer getFullBuffer(SelectionKey key) {
            return getFrom(fullBufferQueue, SelectionKey.OP_WRITE, key);
        }

        private AddressByteBuffer getFrom(Queue<AddressByteBuffer> queue, int operation, SelectionKey key) {
            if (queue.size() == 1 && ((key.interestOps() & operation) != 0)) {
                key.interestOpsAnd(~operation);
                selector.wakeup();
            }
            return queue.poll();
        }

        private void addTo(Queue<AddressByteBuffer> queue, AddressByteBuffer buffer, int operation, SelectionKey key) {
            if (queue.isEmpty() && (key.interestOps() & operation) == 0) {
                key.interestOpsOr(operation);
                selector.wakeup();
            }
            queue.add(buffer);
        }
    }
}
