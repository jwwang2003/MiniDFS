package com.lab1.distributedfs.Node;

import com.lab1.distributedfs.Message.Message;
import com.lab1.distributedfs.Message.MessageAction;
import com.lab1.distributedfs.Message.MessageBroker;
import com.lab1.distributedfs.Message.MessageType;

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

    protected <T> Message<?> requestMessage(MessageAction messageAction, T data) {
        return new Message<>(this.nodeID, MessageType.Request, messageAction, data);
    }

    protected <T> Message<?> responseMessage(MessageAction messageAction, T data) {
        return new Message<>(this.nodeID, MessageType.Response, messageAction, data);
    }

    protected <T> void send(int target, MessageAction messageAction, T data) {
        this.messageBroker.sendToSubscriber(target, requestMessage(messageAction, data));
    }

    protected <T> void broadcast(MessageAction messageAction, T data) {
        this.messageBroker.broadcast(requestMessage(messageAction, data));
    }

    protected <T> void reply(Message<?> prevMessage, MessageAction messageAction, T data) {
        this.messageBroker.sendToSubscriber(prevMessage.getSrcNodeID(), responseMessage(messageAction, data));
    }

    protected <T> void broadcastReply(MessageAction messageAction, T data) {
        this.messageBroker.broadcast(responseMessage(messageAction, data));
    }
}
