package info.kgeorgiy.ja.garipov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class HelloUDPClient implements HelloClient {
    private final Pattern validResponsePattern = Pattern.compile("\\D*(?<thread>\\d+)\\D+(?<request>\\d+)\\D*");
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        ExecutorService clients = Executors.newFixedThreadPool(threads);
        InetSocketAddress serverSocketAddress;
        try {
            serverSocketAddress = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + e.getMessage());
            return;
        }
        IntStream.range(0, threads).forEach((i) -> submitClientTask(clients,
                serverSocketAddress,
                prefix,
                requests,
                i));
        HelloUDPUtil.closeService(clients, HelloUDPUtil.CLIENT_AWAIT_TERMINATION_TIMEOUT);
    }

    private void submitClientTask(final ExecutorService clients,
                                  final InetSocketAddress serverSocketAddress,
                                  String prefix,
                                  int requests,
                                  int i) {
        clients.submit(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(HelloUDPUtil.CLIENT_SOCKET_TIMEOUT);
                int responseBufferSize = socket.getReceiveBufferSize();
                final byte[] responseBuffer = new byte[responseBufferSize];
                DatagramPacket packet = new DatagramPacket(responseBuffer, 0, serverSocketAddress);
                for (int j = 0; j < requests; j++) {
                    if (socket.isClosed()) { break; }
                    String request = prefix + i + "_" + j;
                    String response;
                    try {
                        HelloUDPUtil.sendStringUTF8BySocket(request, socket, packet);
                        System.out.println("Request = " + request);
                        response = HelloUDPUtil.receiveStringUTF8BySocketAndGet(responseBuffer, socket, packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                        j--;
                        continue;
                    }
                    System.out.println("Response = " + response);
                    if (!HelloUDPUtil.validateResponse(validResponsePattern, response, i, j)) { j--; }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        HelloUDPUtil.startClient(args, HelloUDPClient::new);
    }
}
