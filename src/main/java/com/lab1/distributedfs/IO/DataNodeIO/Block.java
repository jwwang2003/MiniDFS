package com.lab1.distributedfs.IO.DataNodeIO;

import com.lab1.distributedfs.Const;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Block {
    // Basic attributes
    private final int nodeID;
    private final int replica;
    private final String pathname;
    private final int blockID;          // Bascially the index of the data chunk

    // Regex expression for the naming pattern of blocks within a DataNode
    // File naming format replica{i}_pathname_block{i}.{file extension}
    public static final Pattern pattern = Pattern.compile("replica(\\d+)_([^_]+)_block(\\d+)" + Const.BLOCK_FILETYPE);

    // Constructor that takes replica, pathname, and blockID
    public Block(int nodeID, int replica, String pathname, int blockID) {
        this.nodeID = nodeID;
        this.replica = replica;
        this.pathname = pathname;
        this.blockID = blockID;
    }

    // Constructor that extracts replica and blockID from the filename using regex
    public Block(int nodeID, String path) throws Exception {
        this.nodeID = nodeID;

        // Regex to match filenames like "replica1_file_block2.blk"
        Matcher matcher = pattern.matcher(path);

        if (matcher.find()) {
            // Extract replica and blockID from the matcher groups
            this.replica = Integer.parseInt(matcher.group(1));  // Extract replica number
            this.pathname = matcher.group(2);
            this.blockID = Integer.parseInt(matcher.group(3));  // Extract block number
        } else {
            throw new Exception("invalid filename format (data node block)");
        }
    }

    public int getNodeID() { return nodeID; }

    public String getFilename() {
        return getBlockName(this.replica, this.pathname, this.blockID).replace("/", "\\") + Const.BLOCK_FILETYPE;
    }

    public int getReplica() {
        return replica;
    }

    public String getPathname() {
        return pathname;
    }

    public int getBlockID() {
        return blockID;
    }

    // Static helper function
    static public String getBlockName(int replica, String filename, int block) {
        return String.format("replica%s_%s_block%s", replica, filename, block);
    }
}
