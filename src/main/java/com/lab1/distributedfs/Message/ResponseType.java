package com.lab1.distributedfs.Message;

public enum ResponseType {
    // General IO
    OPEN,
    CLOSE,
    READ,           // On read success
    WRITE,          // On write success

    // FS operations
    NOTFOUND,
    FOUND,          // If the file is found, then the data attribute will be a FileNode
    ADD,
    DELETE,

    LSFS,

    ACK,            // Used as the response for the heartbeat and other things

    // General
    FAIL,           // Failure to write or read data
    TIMEOUT,        // Timeout
}
