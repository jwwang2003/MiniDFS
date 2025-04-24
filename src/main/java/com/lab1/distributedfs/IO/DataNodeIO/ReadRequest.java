package com.lab1.distributedfs.IO.DataNodeIO;

public class ReadRequest extends Block {
    private final int dataNodeID;

    public ReadRequest(int dataNodeID, int replica, String filename, int blockID) {
        super(replica, filename, blockID);
        this.dataNodeID = dataNodeID;
    }

    public int getDataNodeID() {
        return dataNodeID;
    }
}

