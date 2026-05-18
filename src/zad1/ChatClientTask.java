/**
 *
 *  @author Koc Paweł s34754
 *
 */

package zad1;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

public class ChatClientTask implements Runnable {
    private final ChatClient client;
    private final List<String> msgs;
    private final int wait;
    private final CountDownLatch doneLatch = new CountDownLatch(1);
    private Exception executionException;

    private ChatClientTask(ChatClient client, List<String> msgs, int wait) {
        this.client = client;
        this.msgs = msgs;
        this.wait = wait;
    }

    public static ChatClientTask create(ChatClient c, List<String> msgs, int wait) {
        return new ChatClientTask(c, msgs, wait);
    }

    @Override
    public void run() {
        try {
            client.login();
            if (wait != 0) Thread.sleep(wait);
            
            for (String msg : msgs) {
                client.send(msg);
                if (wait != 0) Thread.sleep(wait);
            }
            
            client.logout();
            if (wait != 0) Thread.sleep(wait);
        } catch (Exception e) {
            this.executionException = e;
        } finally {
            doneLatch.countDown();
        }
    }

    public ChatClient getClient() {
        return client;
    }

    public void get() throws InterruptedException, ExecutionException {
        doneLatch.await();
        if (executionException != null) {
            throw new ExecutionException(executionException);
        }
    }
}
