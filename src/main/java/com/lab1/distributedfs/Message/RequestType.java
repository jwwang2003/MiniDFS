package com.lab1.distributedfs.Message;

public enum RequestType {
    // General IO
    OPEN,
    CLOSE,
    WRITE,
    READ,

    STAT,           // Request a status update from the data node(s)

    // Extended IO (operations for the FS tree, a.k.a. name node)
    FIND,           // Index the FS tree for a file based on the provided path
    ADD,
    DELETE,         // Removes a file based on its "filename", or a specific blockID?

    LSFS,           // Display file system

    HEARTBEAT,      // Check the worker thread is alive
    EXIT            // Worker thread exits
}
