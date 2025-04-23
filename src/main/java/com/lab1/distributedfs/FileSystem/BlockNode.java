package com.lab1.distributedfs.FileSystem;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class BlockNode implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int blockID;
    private final String filename;
    private final List<Integer> dataNodes;      // Replicas

    public BlockNode(int blockID, String filename, List<Integer> dataNodes) {
        this.blockID = blockID;
        this.filename = filename;
        this.dataNodes = dataNodes;
    }

    public int getBlockID() {
        return blockID;
    }

    public String getFilename() {
        return filename;
    }

    public List<Integer> getReplicas() {
        return this.dataNodes;
    }
}
