package com.lab1.distributedfs.Message;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class MessageBroker {
    // Maps subscriber identifiers to their corresponding message handlers
    private final Map<Integer, List<Consumer<Message<?>>>> subscribers = new ConcurrentHashMap<>();
    private final ExecutorService executorService;

    public MessageBroker(int numberOfThreads) {
        this.executorService = Executors.newFixedThreadPool(numberOfThreads);
    }

    // Method to subscribe a consumer (worker) to a particular topic
    public void subscribe(int topic, Consumer<Message<?>> handler) {
        subscribers.computeIfAbsent(topic, k -> new ArrayList<>()).add(handler);
    }

    // Method to send a message to a specific subscriber
    public void sendToSubscriber(int topic, Message<?> message) {
        List<Consumer<Message<?>>> handlers = subscribers.get(topic);
        if (handlers != null) {
            for (Consumer<Message<?>> handler : handlers) {
                executorService.submit(() -> handler.accept(message));
            }
        }
    }

    // Method to broadcast a message to all subscribers
    public void broadcast(Message<?> message) {
        subscribers.values().forEach(handlers -> {
            for (Consumer<Message<?>> handler : handlers) {
                executorService.submit(() -> handler.accept(message));
            }
        });
    }

    // Shutdown the executor service when done
    public void shutdown() {
        try {
            executorService.shutdown();
            assert executorService.awaitTermination(5, TimeUnit.SECONDS);
            assert executorService.isTerminated();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

