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
                Socket socket1 = getSocket();
                Socket socket2 = getSocket();

                ClientHandler clientHandler = new ClientHandler(socket1, socket2);
                Thread thread = new Thread(clientHandler);
                thread.start();
            } catch (IOException e) {
                closeServerSocket();
            }
        }
    }

    private Socket getSocket() throws IOException {
        Socket socket = null;
        while (socket == null) {
            socket = serverSocket.accept();
            if (new BufferedReader(new InputStreamReader(socket.getInputStream())).readLine().startsWith("wait")) {
                break;
            } else {
                ClientHandler clientHandler = new ClientHandler(socket, null);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        }
        return socket;
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
