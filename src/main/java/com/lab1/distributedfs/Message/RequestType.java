package com.lab1.distributedfs.Message;

/**
 * BlockIO should consist of: read operation, write operation, open & close a "file"
 */
public enum RequestType {
    WRITE,
    READ,
    DELETE,         // Removes a file based on its "filename", or a specific blockID?
    HEARTBEAT,      // Check the worker thread is alive
    EXIT            // Worker thread exits
}
