package com.lab1.distributedfs.Message;

import java.io.Serializable;

public class Message<T> implements Serializable {
    /*
    By default, node 0 should be the main node (name server),
    any node above 0 would be a data node, and -1 means "any" node.
     */

    private final MessageType messageType;      // Request / response
    private final RequestType requestType;      // Type of the request (e.g., WRITE, READ, HEARTBEAT)
    private final ResponseType responseType;    // The type of response (e.g., ACK, NACK, SUCCESS, FAILURE, etc.)

    private T data;                             // Data related to the request

    // General constructor
    public Message(MessageType messageType, RequestType requestType, ResponseType responseType, T data) {
        this.messageType = messageType;
        this.requestType = requestType;
        this.data = data;
        this.responseType = responseType;
    }

    // Constructor for a message of type "request"
    public Message(RequestType requestType, T data) {
        this.messageType = MessageType.Request;
        this.requestType = requestType;
        this.data = data;
        this.responseType = null;
    }

    // Constructor for a message of type "response"
    public Message(ResponseType responseType, T data) {
        this.messageType = MessageType.Response;
        this.responseType = responseType;
        this.data = data;
        this.requestType = null;
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
