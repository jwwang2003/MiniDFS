package com.lab1.distributedfs.Message;

import com.lab1.distributedfs.CONST;

import java.io.Serializable;

public class Message<T> implements Serializable {
    /*
    By default, node 0 should be the main node (name server),
    any node above 0 would be a data node, and -1 means "any" node.
     */
    private int srcNodeID;
    private final MessageType messageType;      // Request / response
    private final RequestType requestType;      // Type of the request (e.g., WRITE, READ, HEARTBEAT)
    private final ResponseType responseType;    // The type of response (e.g., ACK, NACK, SUCCESS, FAILURE, etc.)

    private T data;                             // Data related to the request

    // General constructor
    public Message(int srcNodeID, MessageType messageType, RequestType requestType, ResponseType responseType, T data) {
        this.srcNodeID = srcNodeID;
        this.messageType = messageType;
        this.requestType = requestType;
        this.data = data;
        this.responseType = responseType;
    }

    // Constructor for a message of type "request"
    public Message(int srcNodeID, RequestType requestType, T data) {
        this.srcNodeID = srcNodeID;
        this.messageType = MessageType.Request;
        this.requestType = requestType;
        this.data = data;
        this.responseType = null;
    }

    // Constructor for a message of type "response"
    public Message(int srcNodeID, ResponseType responseType, T data) {
        this.srcNodeID = srcNodeID;
        this.messageType = MessageType.Response;
        this.responseType = responseType;
        this.data = data;
        this.requestType = null;
    }

    // Constructor for a message of type "request"
    public Message(RequestType requestType, T data) {
        this.srcNodeID = CONST.MAIN_NODE_ID;
        this.messageType = MessageType.Request;
        this.requestType = requestType;
        this.data = data;
        this.responseType = null;
    }

    // Constructor for a message of type "response"
    public Message(ResponseType responseType, T data) {
        this.srcNodeID = CONST.MAIN_NODE_ID;
        this.messageType = MessageType.Response;
        this.responseType = responseType;
        this.data = data;
        this.requestType = null;
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

    public RequestType getRequestType() {
        return requestType;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

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
                ", requestType=" + requestType +
                ", responseType=" + responseType +
                ", data=" + data +
                '}';
    }
}
