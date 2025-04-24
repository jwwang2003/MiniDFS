package com.lab1.distributedfs;

import java.nio.file.Paths;

public class Const {
    // General Constants
    public static final int BYTE_SIZE = 1024;
    public static String ROOT = "./data";

    // File Metadata Constants
    public static final String FS_IMAGE_FILE = "fsimage.ser";   // Metadata file for NameNode

    // Replication Constants
    public static final int REPLICATION_FACTOR = 3;             // Number of replicas for each block

    // NameNode parameters
    public static final int NAME_NODE_ID = 0;

    // Worker (DataNode) parameters
    public static final int         NUM_DATA_NODES = 5;                     // Number of DataNodes (threads)
    public static final int         WORKER_TIMEOUT = 5000;                  // In terms of milliseconds
    public static final int         BLOCK_SIZE = 4 * BYTE_SIZE;
    public static final String      DATANODE_ROOT_DIR = "dataNodes";
    public static final String      BLOCK_FILETYPE = ".blk";                // Short for "block"

    public static final int         NUM_NODES = NUM_DATA_NODES + 3;         // +2 b.c. name node & client node
    public static final int         CLIENT_NODE_ID = NUM_DATA_NODES - 2;
    public static final int         MAIN_NODE_ID = NUM_DATA_NODES - 1;

    // Name Node - 0
    // Data Node - 1
    // Data Node - 2
    // ,,,
    // Data Node - N
    // Client Node - N + 1
    // Main thread - N + 2

    public static String getPath(String pathname) {
        return Paths.get(ROOT, pathname).toString();
    }
}
