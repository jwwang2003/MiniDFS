package com.lab1.distributedfs.IO.DataNodeIO;

public class ReadResponse extends ReadRequest {
    private final byte[] data;

    public ReadResponse(ReadRequest readRequest, byte[] data) {
        super(readRequest.getNodeID(), readRequest.getReplica(), readRequest.getPathname(), readRequest.getBlockID());
        this.data = data;
    }

    public byte[] getBytes() {
        return data;
    }

    public int getNumBytes() {
        return data.length;
    }
}