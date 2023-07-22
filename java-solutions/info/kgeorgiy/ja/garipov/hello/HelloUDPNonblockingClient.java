package info.kgeorgiy.ja.garipov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class HelloUDPNonblockingClient implements HelloClient {
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        InetSocketAddress serverSocketAddress;
        try {
            serverSocketAddress = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + e.getMessage());
            return;
        }

        try (Selector selector = Selector.open()) {
            int bufferSize = 0;
            try {
                for (int i = 0; i < threads; i++) {
                    DatagramChannel channel = DatagramChannel.open();
                    channel.configureBlocking(false);
                    channel.connect(serverSocketAddress);
                    channel.register(selector, SelectionKey.OP_WRITE, new ThreadStat(i));
                    bufferSize = channel.socket().getReceiveBufferSize();
                }
            } catch (IOException e) {
                for (SelectionKey key : selector.keys()) {
                    key.channel().close();
                }
                return;
            }
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            int remainingChannels = threads;
            while (remainingChannels > 0) {
                int timeout = HelloUDPUtil.CLIENT_SOCKET_TIMEOUT;
                selector.select(timeout);

                if (selector.selectedKeys().isEmpty()) {
                    for (SelectionKey key : selector.keys()) {
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                }

                for (Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator(); keyIterator.hasNext();) {
                    SelectionKey key = keyIterator.next();
                    if (key.isWritable()) {
                        send(key, buffer, prefix);
                    }
                    if (key.isReadable()) {
                        boolean decreaseRemaining = receive(key, buffer, requests);
                        if (decreaseRemaining) {
                            remainingChannels--;
                        } else {
                            key.interestOps(SelectionKey.OP_WRITE);
                        }
                    }
                    keyIterator.remove();
                }
            }

        } catch (IOException e) {
            System.err.println("I/O error occurs: " + e.getMessage());
        }
    }

    private void send(SelectionKey key, ByteBuffer buffer, String prefix) throws IOException {
        DatagramChannel channel = (DatagramChannel) key.channel();
        buffer.clear();

        ThreadStat threadStat = (ThreadStat) key.attachment();
        String request = prefix + threadStat.thread + "_" + threadStat.processed;
        System.out.println("Request = " + request);
        buffer.put(request.getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        channel.send(buffer, channel.getRemoteAddress());
        key.interestOps(SelectionKey.OP_READ);
    }

    private boolean receive(SelectionKey key, ByteBuffer buffer, int requests) throws IOException {
        DatagramChannel channel = (DatagramChannel) key.channel();
        buffer.clear();

        ThreadStat threadStat = (ThreadStat) key.attachment();
        HelloUDPUtil.receiveViaChannel(buffer, channel);
        String response = String.valueOf(StandardCharsets.UTF_8.decode(buffer));
        System.out.println("Response = " + response);
        if (HelloUDPUtil.validateResponse(response, threadStat.thread, threadStat.processed)) {
            key.interestOps(SelectionKey.OP_WRITE);
            threadStat.increment();
            if (threadStat.processed == requests) {
                key.channel().close();
                return true;
            }
        }
        return false;
    }

    public static class ThreadStat {
        private final int thread;
        private int processed = 0;
        public ThreadStat(int thread) {
            this.thread = thread;
        }

        public void increment() {
            processed++;
        }
    }

    public static void main(String[] args) {
        HelloUDPUtil.startClient(args, HelloUDPNonblockingClient::new);
    }
}
