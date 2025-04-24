package com.lab1.distributedfs.IO.DataNodeIO;

public class WriteRequest extends Block {
    private final byte[] data;

    public WriteRequest(int replica, String filename, int blockID, byte[] data) {
        super(replica, filename, blockID);
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}