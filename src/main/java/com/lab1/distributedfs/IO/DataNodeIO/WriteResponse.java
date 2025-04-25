package com.lab1.distributedfs.IO.DataNodeIO;

public class WriteResponse extends WriteRequest{
    private final int bytesWritten;

    public WriteResponse(WriteRequest writeRequest, int bytesWritten) {
        super(writeRequest);
        this.bytesWritten = bytesWritten;
    }

    public String getFilename() {
        return getBlockName(this.getReplica(), this.getPathname(), this.getBlockID());
    }

    public int getNumBytesWritten() {
        return bytesWritten;
    }
}