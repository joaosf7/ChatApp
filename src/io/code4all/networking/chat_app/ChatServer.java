package io.code4all.networking.chat_app;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    protected static LinkedList<ServerWorker> serverWorkers;
    public static final int port = 8080;

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        ExecutorService cachedPool = Executors.newCachedThreadPool();
        serverWorkers = new LinkedList<>();

        System.out.println("Welcome to the Varbies Multi-client chat!!!!");
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server started: " + serverSocket);

            while (true) {
                System.out.println(Thread.currentThread().getName() + ": Waiting for connection at port " + port + "...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client accepted: " + clientSocket);
                ChatServer.serverWorkers.add(new ServerWorker(clientSocket));
                cachedPool.submit(chatServer.serverWorkers.getLast());
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public static void broadcast(String line) {
        System.out.println("Broadcasting: " + line);
        for (ServerWorker worker : serverWorkers) {
            try {
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(worker.getSocket().getOutputStream()));
                output.write(line);
                output.newLine();
                output.flush();
            } catch (IOException e) {
                System.err.println("IO Exception when trying to broadcast!!");
                System.err.println("Removing worker...");
                serverWorkers.remove(worker);
                System.err.println(e);
            }
        }
    }

    public static void sendMessageToWorker(ServerWorker worker, String line) {
        try {
            BufferedWriter output = new BufferedWriter(new OutputStreamWriter(worker.getSocket().getOutputStream()));
            output.write(line);
            output.newLine();
            output.flush();
        } catch (IOException e) {
            System.err.println("IOException when trying to send message to worker: " + worker.getName());
            System.err.println(e);
        }

    }
}

class ServerWorker implements Runnable {
    private Socket clientSocket;
    private String name;
    private static int workerCounter;

    public ServerWorker(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.name = "Anonymous" + workerCounter++;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " started working");
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String line;
            while (!(line = input.readLine()).equals("/quit")) {
                if (line.equals(""))
                    continue;
                else if (line.charAt(0) == '/') {
                    String command = line.split(" ")[0];
                    String argument = null;
                    if (line.split(" ").length >= 2)
                        argument = line.split(" ")[1];
                    switch (command) {
                        case "/help":
                            showHelp();
                            break;
                        case "/nick":
                            changeNick(argument);
                            break;
                        case "/list":
                            list();
                            break;
                        case "/whisper":
                            whisper(line);
                            break;
                        case "/send":
                            sendFile(line);
                            break;
                        default:
                            ChatServer.broadcast("Unknown command. Type /help for a list of known commands.");
                    }
                } else
                    ChatServer.broadcast(name + " says: " + line);
            }
            input.close();
            clientSocket.close();
            ChatServer.serverWorkers.remove(this);
            System.out.println("Client " + name + " is disconnecting");
        } catch (IOException e) {
            System.err.println("IO issues on input BufferedReader");
            System.err.println(e);
        }

    }

    private void sendFile(String line) {
        if (line.split(" ").length < 3) {
            ChatServer.sendMessageToWorker(this, "Can't send a file without a user and a file :)");
            return;
        }
        for (ServerWorker worker : ChatServer.serverWorkers)
            if (worker.getName().equals(line.split(" ")[1]) && !(worker.getName().equals(this.getName())))
                ChatServer.sendMessageToWorker(worker, this.getName() + " whispers: " + line.split(" ", 3)[2]);
    }

    private void showHelp() {
        ChatServer.sendMessageToWorker(this, "List of command to use in the Varbies Mega Chat:");
        ChatServer.sendMessageToWorker(this, "/help - lists all the commands");
        ChatServer.sendMessageToWorker(this, "/nick - change your nickname");
        ChatServer.sendMessageToWorker(this, "/list - lists all the users currently in this chat");
        ChatServer.sendMessageToWorker(this, "/whisper nick message - whisper a message to a friend");
        ChatServer.sendMessageToWorker(this, "/send nick file - sends a file to selected user");
    }

    private void changeNick(String argument) {
        if (argument == null)
            ChatServer.sendMessageToWorker(this, "Your nickname is: " + name);
        else {
            int i;
            for (i = 0; i < ChatServer.serverWorkers.size(); i++)
                if (ChatServer.serverWorkers.get(i).getName().equals(argument)) {
                    ChatServer.sendMessageToWorker(this, "Nickname already in use!");
                    break;
                }
            if (i == ChatServer.serverWorkers.size()) {
                ChatServer.broadcast(name + " sets nickname to " + argument);
                name = argument;
            }
        }
    }

    private void list() {
        ChatServer.sendMessageToWorker(this, "List of all the users currently in this chat:");
        for (ServerWorker worker : ChatServer.serverWorkers)
            ChatServer.sendMessageToWorker(this, worker.name);
    }

    private void whisper(String line) {
        if (line.split(" ").length < 3) {
            ChatServer.sendMessageToWorker(this, "Can't whisper without user and a message :)");
            return;
        }
        for (ServerWorker worker : ChatServer.serverWorkers)
            if (worker.getName().equals(line.split(" ")[1]) && !(worker.getName().equals(this.getName())))
                ChatServer.sendMessageToWorker(worker, this.getName() + " whispers: " + line.split(" ", 3)[2]);
    }

    public Socket getSocket() {
        return this.clientSocket;
    }

    public String getName() {
        return this.name;
    }
}

