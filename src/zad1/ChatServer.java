/**
 *
 *  @author Koc Paweł s34754
 *
 */

package zad1;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class ChatServer {
    private final int port;
    private ServerSocket serverSocket;
    private Thread acceptorThread;
    
    private final ExecutorService vtExecutor = Executors.newVirtualThreadPerTaskExecutor();
    
    private final ConcurrentHashMap<PrintWriter, String> clients = new ConcurrentHashMap<>();
    private final StringBuilder serverLog = new StringBuilder();
    private final ReentrantLock logLock = new ReentrantLock();
    private volatile boolean isRunning = false;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.nnnnnnnnn");

    public ChatServer(int port) {
        this.port = port;
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            acceptorThread = new Thread(() -> {
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        vtExecutor.execute(() -> handleClient(clientSocket));
                    } catch (IOException e) {
                        if (isRunning) e.printStackTrace();
                    }
                }
            });
            acceptorThread.start();
            System.out.println("Server started");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException _) {
        }
        
        logAndBroadcast("ChatServer: chat closed");
        
        vtExecutor.shutdown();
        System.out.println("Server stopped");
    }

    private void handleClient(Socket socket) {
        try (
                socket;
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            String line;
            String id = null;
            
            while ((line = in.readLine()) != null) {
                String[] parts = line.split("\t", 2);
                String cmd = parts[0];
                
                if ("LOGIN".equals(cmd)) {
                    id = parts.length > 1 ? parts[1] : "Unknown";
                    clients.put(out, id);
                    logAndBroadcast(id + " logged in");
                } else if ("MSG".equals(cmd)) {
                    if (id != null) {
                        logAndBroadcast(id + ": " + (parts.length > 1 ? parts[1] : ""));
                    }
                } else if ("LOGOUT".equals(cmd)) {
                    if (id != null) {
                        logAndBroadcast(id + " logged out");
                        clients.remove(out);
                        break; 
                    }
                }
            }
        } catch (IOException _) {
        }
    }

    private void logAndBroadcast(String msg) {
        String time = LocalTime.now().format(TIME_FORMATTER);
        String logEntry = time + " " + msg + "\n";

        logLock.lock();
        try {
            serverLog.append(logEntry);
        } finally {
            logLock.unlock();
        }

        for (PrintWriter out : clients.keySet()) {
            out.println(msg);
        }
    }

    public String getServerLog() {
        return serverLog.toString();
    }
}
