package io.code4all.networking.chat_app;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatClient {
    public static void main(String[] args) {
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Welcome to the Varbies Chat Client!!");
        try {
            Socket socket = new Socket("localhost", ChatServer.port);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            String line;

            // Start the log thread here
            ExecutorService singleThread = Executors.newSingleThreadExecutor();
            singleThread.submit(new ServerLogger(reader));


            do {
                line = consoleReader.readLine();
                writer.write(line);
                writer.newLine();
                writer.flush();
            } while (!line.equals("/quit"));
            singleThread.shutdownNow();
        } catch (IOException e) {
            System.err.println("IO Exception in ChatClient main thread");
            System.err.println(e);
        }
    }
}

class ServerLogger implements Runnable {
    private BufferedReader reader;

    public ServerLogger(BufferedReader reader) {
        this.reader = reader;
    }

    @Override
    public void run() {
        String line;

        try {
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.err.println("IO Exception trying to log from server");
            System.err.println(e);
        }
        System.out.println("Exiting Chat client");
    }
}

