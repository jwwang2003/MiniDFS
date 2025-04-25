package com.lab1.distributedfs.IO.DataNodeIO;

public class WriteRequest extends Block {

    private final byte[] data;

    public WriteRequest(WriteRequest writeRequest) {
        super(writeRequest.getNodeID(), writeRequest.getReplica(), writeRequest.getPathname(), writeRequest.getBlockID());
        this.data = writeRequest.getData();
    }

    public WriteRequest(int dataNodeID, int replica, String pathname, int blockID, byte[] data) {
        super(dataNodeID, replica, pathname, blockID);
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}