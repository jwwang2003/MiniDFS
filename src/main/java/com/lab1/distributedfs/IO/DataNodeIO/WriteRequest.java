package com.lab1.distributedfs.IO.DataNodeIO;

public class WriteRequest extends Block {
    private final boolean appendBlock;
    private final byte[] data;

    public WriteRequest(WriteRequest writeRequest) {
        super(writeRequest.getNodeID(), writeRequest.getReplica(), writeRequest.getPathname(), writeRequest.getBlockID());
        this.data = writeRequest.getData();
        this.appendBlock = writeRequest.appendBlock;
    }

    public WriteRequest(int dataNodeID, int replica, String pathname, int blockID, byte[] data) {
        super(dataNodeID, replica, pathname, blockID);
        this.data = data;
        this.appendBlock = false;
    }

    public WriteRequest(int dataNodeID, int replica, String pathname, int blockID, byte[] data, boolean isAppendBlock) {
        super(dataNodeID, replica, pathname, blockID);
        this.data = data;
        this.appendBlock = isAppendBlock;
    }

    public byte[] getData() {
        return data;
    }
    public boolean isAppendBlock() { return this.appendBlock; }
}