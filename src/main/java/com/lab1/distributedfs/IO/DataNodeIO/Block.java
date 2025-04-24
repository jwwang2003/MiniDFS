package com.lab1.distributedfs.IO.DataNodeIO;

import com.lab1.distributedfs.CONST;
import com.lab1.distributedfs.Node.DataNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Block {
    // Basic attributes
    private final int replica;
    private final String filename;
    private final int blockID;

    // Regex expression for the naming pattern of blocks within a DataNode
    public static final Pattern pattern = Pattern.compile("replica(\\d+)_([^_]+)_block(\\d+)" + CONST.BLOCK_FILETYPE);

    // Constructor that takes replica, filename, and blockID
    public Block(int replica, String filename, int blockID) {
        this.replica = replica;
        this.filename = filename;
        this.blockID = blockID;
    }

    // Constructor that extracts replica and blockID from the filename using regex
    public Block(String filename) {
        // Using regex to extract replica and blockID
        int[] replicaAndBlock = extractReplicaAndBlockFromFilename(filename);
        this.replica = replicaAndBlock[0];
        this.blockID = replicaAndBlock[1];
        this.filename = filename;
    }

    public String getFileName() {
        return DataNode.getBlockName(this.replica, this.filename, this.blockID) + CONST.BLOCK_FILETYPE;
    }

    public int getReplica() {
        return replica;
    }

    public String getFilename() {
        return filename;
    }

    public int getBlockID() {
        return blockID;
    }

    // Method to extract replica and blockID from the filename using regex
    private int[] extractReplicaAndBlockFromFilename(String filename) {
        // Regex to match filenames like "replica1_myfile_block2.blk"
        Matcher matcher = pattern.matcher(filename);

        if (matcher.find()) {
            // Extract replica and blockID from the matcher groups
            int replica = Integer.parseInt(matcher.group(1));  // Extract replica number
            int blockID = Integer.parseInt(matcher.group(3));  // Extract block number
            return new int[] { replica, blockID };
        } else {
            // Return default values if the filename does not match the expected format
            System.out.printf("Error: Invalid filename format: %s%n", filename);
            return new int[] { -1, -1 };  // Indicate failure to parse
        }
    }
}
