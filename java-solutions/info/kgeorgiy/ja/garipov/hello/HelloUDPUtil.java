package info.kgeorgiy.ja.garipov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelloUDPUtil {
    public static final Pattern validResponsePattern = Pattern.compile("\\D*(?<thread>\\d+)\\D+(?<request>\\d+)\\D*");
    public static final int CLIENT_SOCKET_TIMEOUT = 500;
    public static final int SERVER_AWAIT_TERMINATION_TIMEOUT = 50;
    public static final long CLIENT_AWAIT_TERMINATION_TIMEOUT = Long.MAX_VALUE;

    public static SocketAddress receiveViaChannel(ByteBuffer buffer, DatagramChannel channel) throws IOException {
        SocketAddress sender = channel.receive(buffer);
        buffer.flip();
        return sender;
    }

    public static DatagramChannel setUpChannel(int port, Selector selector) throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.bind(new InetSocketAddress(port));
        channel.register(selector, SelectionKey.OP_READ);
        return channel;
    }

    public static String receiveStringUTF8BySocketAndGet(byte[] buffer, DatagramSocket socket, DatagramPacket packet)
            throws IOException {
        receiveStringUTF8BySocket(buffer, socket, packet);
        return new String(packet.getData(),
                packet.getOffset(),
                packet.getLength(),
                StandardCharsets.UTF_8);
    }

    public static void receiveStringUTF8BySocket(byte[] buffer, DatagramSocket socket, DatagramPacket packet)
            throws IOException {
        packet.setData(buffer);
        socket.receive(packet);
    }

    public static void sendStringUTF8BySocket(String data, DatagramSocket socket, DatagramPacket packet)
            throws IOException {
        packet.setData(data.getBytes(StandardCharsets.UTF_8));
        socket.send(packet);
    }

    public static boolean validateResponse(Pattern validResponsePattern, String response, int thread, int request) {
        Matcher matcher = validResponsePattern.matcher(response);
        return matcher.matches() && matcher.group("thread").equals(Integer.toString(thread)) &&
                matcher.group("request").equals(Integer.toString(request));
    }

    public static boolean validateResponse(String response, int thread, int request) {
        return validateResponse(validResponsePattern, response, thread, request);
    }

    public static void closeService(ExecutorService service, long timeout) {
        service.shutdown();
        try {
            if (!service.awaitTermination(timeout, TimeUnit.SECONDS)) {
                service.shutdownNow();
                if (!service.awaitTermination(timeout, TimeUnit.SECONDS))
                    System.err.println("Pool wasn't terminated");
            }
        } catch (InterruptedException ie) {
            service.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void startClient(String[] args, Supplier<HelloClient> clientSupplier) {
        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Arguments required: host, port, prefix, threads, requests");
            return;
        }
        String host, prefix;
        int port, threads, requests;
        try {
            host = args[0];
            port = Integer.parseInt(args[1]);
            prefix = args[2];
            threads = Integer.parseInt(args[3]);
            requests = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid arguments, required : int, int, String, int, int");
            return;
        }
        clientSupplier.get().run(host, port, prefix, threads, requests);
    }

    public static void startServer(String[] args, Supplier<HelloServer> serverSupplier) {
        if (args == null || args.length != 2 && Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Arguments required: port(int), threads(int)");
            return;
        }
        int port, threads;
        try {
            port = Integer.parseInt(args[0]);
            threads = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid arguments, required : int, int");
            return;
        }
        try (HelloServer server = serverSupplier.get()) {
            server.start(port, threads);
        }
    }
}
