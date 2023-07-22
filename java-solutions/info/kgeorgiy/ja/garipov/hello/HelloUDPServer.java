package info.kgeorgiy.ja.garipov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket;
    private ExecutorService service;

    @Override
    public void start(int port, int threads) {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new RuntimeException(e.getMessage());
        }
        service = Executors.newFixedThreadPool(threads);
        IntStream.range(0, threads).forEach((i) -> service.submit(this::listenSocket));
    }

    private void listenSocket() {
        int bufferSize;
        try {
            bufferSize = socket.getReceiveBufferSize();
        } catch (SocketException e) {
            throw new IllegalStateException(e.getMessage());
        }
        byte[] buffer = new byte[bufferSize];
        DatagramPacket packet = new DatagramPacket(buffer, bufferSize);
        while (!socket.isClosed()) {
            try {
                String request = HelloUDPUtil.receiveStringUTF8BySocketAndGet(buffer, socket, packet);
                String response = "Hello, " + request;
                HelloUDPUtil.sendStringUTF8BySocket(response, socket, packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() {
        socket.close();
        HelloUDPUtil.closeService(service, HelloUDPUtil.SERVER_AWAIT_TERMINATION_TIMEOUT);
    }

    public static void main(String[] args) {
        HelloUDPUtil.startServer(args, HelloUDPServer::new);
    }
}
