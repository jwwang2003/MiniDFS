package com.lab1.distributedfs.Node;

import com.lab1.distributedfs.Message.MessageBroker;

public class Node implements Runnable {
    protected final MessageBroker messageBroker;
    protected final int nodeID;

    public Node(int nodeID, MessageBroker messageBroker) {
        this.nodeID = nodeID;
        this.messageBroker = messageBroker;
    }

    @Override
    public void run() {
        // Example implementation
        messageBroker.subscribe(this.nodeID, message -> {
            // Handle message
            System.out.println(this.nodeID + " received: " + message.toString());
        });
    }
}
