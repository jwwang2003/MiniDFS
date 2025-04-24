package com.lab1.distributedfs.IO.DataNodeIO;

import com.lab1.distributedfs.Node.DataNode;

public class WriteResponse extends WriteRequest{
    private final int bytesWritten;

    public WriteResponse(WriteRequest writeRequest, int bytesWritten) {
        super(writeRequest.getReplica(), writeRequest.getFilename(), writeRequest.getBlockID(), writeRequest.getData());
        this.bytesWritten = bytesWritten;
    }

    public String getFileName() {
        return DataNode.getBlockName(this.getReplica(), this.getFilename(), this.getBlockID());
    }

    public int getNumBytesWritten() {
        return bytesWritten;
    }
}