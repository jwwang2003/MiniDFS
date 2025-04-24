package com.lab1.distributedfs.Message;

public enum ResponseType {
    ACK,            // Used as the response for the heartbeat

    NOTFOUND,
    FOUND,          // If the file is found, then the data attribute will be a FileNode

    OPEN,
    CLOSE,
    READ,           // On read success
    WRITE,          // On write success

    LSFS,

    // General
    FAIL,           // Failure to write or read data
    TIMEOUT,        // Timeout
}
