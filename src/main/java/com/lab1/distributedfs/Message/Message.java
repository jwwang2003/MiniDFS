package com.lab1.distributedfs.Message;

import java.io.Serializable;

public class Message<T> implements Serializable {
    /*
    By default, node 0 should be the main node (name server),
    any node above 0 would be a data node, and -1 means "any" node.
     */
    private int srcNodeID;
    private final MessageType messageType;      // Request / response
    private final MessageAction messageAction;    // The type of response (e.g., ACK, NACK, SUCCESS, FAILURE, etc.)

    private T data;                             // Data related to the request

    public Message(int srcNodeID, MessageType messageType, MessageAction messageAction, T data) {
        this.srcNodeID = srcNodeID;
        this.messageType = messageType;
        this.messageAction = messageAction;
        this.data = data;
    }

    public int getSrcNodeID() {
        return srcNodeID;
    }

    public void setSrcNodeID(int srcNodeID) {
        this.srcNodeID = srcNodeID;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public MessageAction getMessageAction() { return messageAction; }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return  "Message{" +
                "messageType=" + messageType +
                ", responseType=" + messageAction +
                ", data=" + data +
                '}';
    }
}
