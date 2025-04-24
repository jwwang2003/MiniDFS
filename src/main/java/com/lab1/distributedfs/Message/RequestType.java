package com.lab1.distributedfs.Message;

/**
 * BlockIO should consist of: read operation, write operation, open & close a "file"
 */
public enum RequestType {
    // General IO
    OPEN,
    CLOSE,
    WRITE,
    READ,

    // Extended IO (operations for the FS tree, a.k.a. name node)
    FIND,           // Index the FS tree for a file based on the provided path
    ADD,
    DELETE,         // Removes a file based on its "filename", or a specific blockID?

    LSFS,           // Display file system

    HEARTBEAT,      // Check the worker thread is alive
    EXIT            // Worker thread exits
}
