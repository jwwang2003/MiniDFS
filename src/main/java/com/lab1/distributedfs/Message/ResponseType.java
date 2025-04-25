package com.lab1.distributedfs.Message;

/**
 * The idea is that the response types should be similar to the request type, but it has its own class because there might
 * be extra states to handle (for example, FIND operation would have two possible results: FOUND or NOTFOUND).
 * The purpose of having request and response separate is to have this clear separation.
 */
public enum ResponseType {
    // General IO
    OPEN,           // Return OPEN if a file is successfully opened (payload data type should be an Open object)
    CLOSE,          // Return CLOSE if a file is successfully closed (payload data type should be an Open object)
    // Note that for the above two actions, only file metadata (or file node) data is mutated

    // File DATA (handed per block, handled by file nodes)
    READ,           // On read success returns the BlockNode wrapped inside the IO component
    WRITE,          // On write success returns the BlockNode wrapped inside the IO component

    STAT,

    // FS tree operations
    // FIND operation usually results in FOUND or NOTFOUND, FAIL is only thrown if there is an internal error
    NOTFOUND,
    FOUND,          // If the file is found, then the data attribute will be a FileNode

    ADD,            // Note that the ADD operation also overwrites the file node if it already exists
                    // This accomplishes the task of "adding" new nodes and updating existing ones (just add it again)
    DELETE,         // Shell "rm" equivalent (no need to remove dirs, they are automatically removed if empty)
                    // Since this just a simple "file" system, we only can manage files, not directories
    LSFS,

    ACK,            // Used as the response for the heartbeat (and maybe other things?)

    // General
    FAIL,           // Generally used to handle errors and exceptions
    TIMEOUT,
}
