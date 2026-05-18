/**
 *
 *  @author Koc Paweł s34754
 *
 */

package zad1;

import java.io.*;
import java.net.*;
import java.util.concurrent.CountDownLatch;

public class ChatClient {
    private final String host;
    private final int port;
    private final String id;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread readerThread;
    
    private final StringBuilder chatView;
    private final CountDownLatch logoutLatch;

    public ChatClient(String host, int port, String id) {
        this.host = host;
        this.port = port;
        this.id = id;
        this.chatView = new StringBuilder();
//        this.chatView.append("=== ").append(id).append(" chat view\n");
        this.logoutLatch = new CountDownLatch(1);
    }

    public void login() {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            readerThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        chatView.append(line).append("\n");
                        
                        if (line.equals(id + " logged out") || line.equals("ChatServer: chat closed")) {
                            logoutLatch.countDown();
                            break;
                        }
                    }
                } catch (IOException e) {
                    if (!socket.isClosed()) {
                        chatView.append("*** ").append(e.toString()).append("\n");
                    }
                } finally {
                    logoutLatch.countDown();
                }
            });
            readerThread.start();
            
            out.println("LOGIN\t" + id);
        } catch (IOException e) {
            chatView.append("*** ").append(e.toString()).append("\n");
        }
    }

    public void logout() {
        if (out != null) {
            out.println("LOGOUT\t");
        }
        try {
            logoutLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            chatView.append("*** ").append(e.toString()).append("\n");
        }
    }

    public void send(String req) {
        if (out != null) {
            out.println("MSG\t" + req);
        }
    }

    public String getChatView() {
        return chatView.toString();
    }
    
    public String getId() {
        return id;
    }
}
