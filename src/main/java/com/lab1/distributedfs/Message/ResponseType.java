package com.lab1.distributedfs.Message;

public enum ResponseType {
    ACK,            // Used as the response for the heartbeat
    SUCCESS,        // On a successful data write or read
    NOTFOUND,
    FAIL,           // Failure to write or read data
    TIMEOUT         // Timeout
}
