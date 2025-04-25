package com.lab1.distributedfs.ShellCommand;

import com.lab1.distributedfs.Const;
import com.lab1.distributedfs.Message.Message;
import com.lab1.distributedfs.Message.RequestType;
import com.lab1.distributedfs.Node.Client;

import java.util.List;
import java.util.concurrent.*;

public class Command {
    public static Client client;
    public static ExecutorService executorService;
    public static BlockingQueue<Message<?>> requestQueue = new LinkedBlockingQueue<>();
    public static BlockingQueue<Message<?>> responseQueue = new LinkedBlockingQueue<>();

    public String getDescription() { return ""; }

    // Method to return the command help message
    public String getHelpMessage() { return ""; }

    // Method to handle the command logic
    public boolean handle(List<String> commandArgs) { return false; }

    public static void shutdown() {
        System.out.println("Attempting to shutdown...");
        try {
            requestQueue.put(new Message<>(Const.MAIN_NODE_ID, RequestType.EXIT, null));
            executorService.shutdown();
            assert executorService.awaitTermination(5, TimeUnit.SECONDS);
            assert executorService.isTerminated();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static Message<?> waitForResponse() {
        try {
            return responseQueue.poll(Const.WORKER_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // That means the timer must have expired
            System.out.println("Fatal error: timeout");
            shutdown();
            return null;
        }
    }
}
