package com.lab1.distributedfs.IO.DataNodeIO;

public class ReadRequest extends Block {
    // Read from any replica file
    public ReadRequest(int dataNodeID, String filename, int blockID) {
        super(dataNodeID, -1, filename, blockID);
    }

    // Read from a specific replica file
    public ReadRequest(int dataNodeID, int replica, String filename, int blockID) {
        super(dataNodeID, replica, filename, blockID);
    }
}

