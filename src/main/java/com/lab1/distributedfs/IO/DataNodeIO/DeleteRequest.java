package com.lab1.distributedfs.IO.DataNodeIO;

public class DeleteRequest extends Block {
    private final int dataNodeID;

    // Delete all the blocks of the file based on the filename
    public DeleteRequest(int dataNodeID, String filename) {
        super(-1, filename, -1);
        this.dataNodeID = dataNodeID;
    }

    public int getDataNodeID() {
        return dataNodeID;
    }
}

