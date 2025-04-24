package com.lab1.distributedfs.IO.DataNodeIO;

import com.lab1.distributedfs.Node.DataNode;

import java.util.List;
import java.util.Set;

public class DeleteResponse extends DeleteRequest {
    private final Set<Integer> blocksDeleted;

    public DeleteResponse(DeleteRequest deleteRequest, Set<Integer> blocksDeleted) {
        super(deleteRequest.getDataNodeID(), deleteRequest.getFilename());
        this.blocksDeleted = blocksDeleted;
    }

    public String getFileName() {
        return DataNode.getBlockName(this.getReplica(), this.getFilename(), this.getBlockID());
    }

    public List<Integer> getDeletedBlocks() {
        return blocksDeleted.stream().toList();
    }
}