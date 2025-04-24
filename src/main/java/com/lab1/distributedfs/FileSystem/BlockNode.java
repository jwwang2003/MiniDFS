package com.lab1.distributedfs.FileSystem;

import com.lab1.distributedfs.CONST;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class BlockNode implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int blockID;
    private final String filename;
    private int blockSize;                      // Track the current size of block

    private final List<Integer> dataNodes;      // Replicas are stored here

    public BlockNode(int blockID, String filename, int blockSize, List<Integer> dataNodes) {
        this.blockID = blockID;
        this.filename = filename;
        this.dataNodes = dataNodes;
        this.blockSize = blockSize;
    }

    public int getBlockID() {
        return blockID;
    }

    public String getFilename() {
        return filename;
    }

    // Manage replicas
    public List<Integer> getReplicas() {
        // Returns a list of data nodes where this block can be found
        return this.dataNodes;
    }

    public boolean removeReplica(int replica) {
        try { return this.dataNodes.remove((Integer) replica); }
        catch (NullPointerException e) { return false; }
    }

    public boolean addReplica(int replica) {
        try { return this.dataNodes.add(replica); }
        catch (NullPointerException e) { return false; }
    }

    public int expand(int size) {
        // Appending more data to the current block (if it is not full)
        // Note that here is no "shrink" method
        if (size + this.blockSize > CONST.BLOCK_SIZE) { return -1; }
        return this.blockSize += size;
    }

    public int getFreeSpace() {
        return CONST.BLOCK_SIZE - this.blockSize;
    }
}
