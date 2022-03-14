package com.bedi.warcaby;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public record Server(ServerSocket serverSocket) {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(1234);
        Server server = new Server(serverSocket);
        server.startServer();
    }

    public void startServer() {
        while (! serverSocket.isClosed()) {
            try {
                Socket socket1 = serverSocket.accept();
                Socket socket2 = serverSocket.accept();

                ClientHandler clientHandler = new ClientHandler(socket1, socket2);
                Thread thread = new Thread(clientHandler);
                thread.start();
            } catch (IOException e) {
                closeServerSocket();
            }
        }
    }

    public void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
