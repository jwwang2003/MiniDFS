package com.lab1.distributedfs.Node;

public class DataNodeStatus {
    public int nodeId;
    public long lastSeen;
    public boolean alive;
    public int blockCount;
    public long storageUsed;

    DataNodeStatus(int nodeId, long lastSeen) {
        this.nodeId = nodeId;
        this.lastSeen = lastSeen;
        this.alive = true;
        this.blockCount = 0;
        this.storageUsed = 0;
    }

    @Override
    public String toString() {
        return String.format(
            "DataNodeStatus[nodeId=%d, lastSeen=%d, alive=%b, blockCount=%d, storageUsed=%d]",
            nodeId, lastSeen, alive, blockCount, storageUsed
        );
    }
}
