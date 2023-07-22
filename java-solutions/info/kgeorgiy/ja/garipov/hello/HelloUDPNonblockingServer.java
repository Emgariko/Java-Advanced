package info.kgeorgiy.ja.garipov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPNonblockingServer implements HelloServer {
    private Selector selector;
    private ExecutorService executors;
    private DatagramChannel channel;
    private final ConcurrentLinkedQueue<Packet> freeBuffers = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Packet> sendQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void start(int port, int threads) {
        executors = Executors.newFixedThreadPool(threads + 1);
        try {
            selector = Selector.open();
            channel = HelloUDPUtil.setUpChannel(port, selector);
            for (int i = 0; i < threads; i++) {
                freeBuffers.add(new Packet(ByteBuffer.allocate(channel.socket().getReceiveBufferSize()), null));
            }
            executors.submit(this::listenChannel);
        } catch (IOException e) {
            System.err.println("I/O error occurs");
        }
    }

    private void listenChannel() {
        while (!Thread.interrupted() && !selector.keys().isEmpty()) {
            try {
                selector.select();
                for (Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator(); keyIterator.hasNext();) {
                    SelectionKey key = keyIterator.next();
                    try {
                        if (key.isReadable()) {
                            receive(key);
                        }
                        if (key.isWritable()) {
                            send(key);
                        }
                    } catch (IOException e) {
                        System.err.println("Error occurs: " + e.getMessage());
                    }
                    keyIterator.remove();
                }
            } catch (IOException e) {
                System.err.println("Error occurs: " + e.getMessage());
            }
        }
    }

    private void receive(SelectionKey key) throws IOException {
        if (freeBuffers.isEmpty()) {
            key.interestOpsAnd(SelectionKey.OP_WRITE);
        } else {
            Packet packet = freeBuffers.remove();
            ByteBuffer buffer = packet.buffer;
            SocketAddress sender = HelloUDPUtil.receiveViaChannel(buffer, channel);
            executors.submit(() -> {
                String response = "Hello, " + StandardCharsets.UTF_8.decode(buffer.slice());
                buffer.clear();
                freeBuffers.add(packet);
                sendQueue.add(new Packet(ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8)), sender));
                key.interestOpsOr(SelectionKey.OP_WRITE);
                selector.wakeup();
            });
        }
    }

    private void send(SelectionKey key) throws IOException {
        if (sendQueue.isEmpty()) {
            key.interestOps(SelectionKey.OP_READ);
        } else {
            Packet packet = sendQueue.remove();
            channel.send(packet.buffer, packet.sender);
            key.interestOpsOr(SelectionKey.OP_READ);
        }
    }

    @Override
    public void close() {
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (IOException e) {
            System.err.println("Error occurs: " + e.getMessage());
        }
        try {
            if (selector != null) {
                selector.close();
            }
        } catch (IOException e) {
            System.err.println("Error occurs: " + e.getMessage());
        }

        HelloUDPUtil.closeService(executors, HelloUDPUtil.SERVER_AWAIT_TERMINATION_TIMEOUT);
    }

    private static class Packet {
        private final ByteBuffer buffer;
        private final SocketAddress sender;

        private Packet(ByteBuffer buffer, SocketAddress sender) {
            this.buffer = buffer;
            this.sender = sender;
        }
    }

    public static void main(String[] args) {
        HelloUDPUtil.startServer(args, HelloUDPNonblockingServer::new);
    }
}
