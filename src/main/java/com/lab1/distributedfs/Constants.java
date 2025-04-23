package com.lab1.distributedfs;

import java.nio.file.Paths;

public class Constants {
    // General Constants
    public static final int BYTE_SIZE = 1024;
    public static String ROOT = "./data";

    // File Metadata Constants
    public static final String FS_IMAGE_FILE = "fsimage.ser";   // Metadata file for NameNode

    // File Operation Modes
    public static final String WRITE_MODE = "w";
    public static final String READ_MODE = "r";

    // Replication Constants
    public static final int REPLICATION_FACTOR = 3;             // Number of replicas for each block

    // NameNode parameters

    // Worker (DataNode) parameters
    public static final int NUM_DATANODES = 5;                  // Number of DataNodes (threads)
    public static final int WORKER_TIMEOUT = 5000;              // In terms of milliseconds
    public static final int BLOCK_SIZE = 4 * BYTE_SIZE;
    public static final String DATANODE_ROOT_DIR = "dataNodes";
    public static final String BLOCK_FILETYPE = ".blk";         // Short for "block"

    public static String getPath(String pathname) {
        return Paths.get(ROOT, pathname).toString();
    }
}
