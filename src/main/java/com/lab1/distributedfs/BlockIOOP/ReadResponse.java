package com.lab1.distributedfs.BlockIOOP;

public class ReadResponse extends ReadRequest {
    private final byte[] data;

    public ReadResponse(ReadRequest readRequest, byte[] data) {
        super(readRequest.getDataNodeID(), readRequest.getReplica(), readRequest.getFilename(), readRequest.getBlockID());
        this.data = data;
    }

    public byte[] getBytes() {
        return data;
    }

    public int getNumBytes() {
        return data.length;
    }
}