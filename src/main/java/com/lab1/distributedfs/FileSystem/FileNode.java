package com.lab1.distributedfs.FileSystem;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

// Class to represent the FileNode (which stores file metadata)
public class FileNode implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;
    private final long fileSize;
    private final List<BlockNode> blockList;

    public FileNode(String fileName, long fileSize, List<BlockNode> blockList) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.blockList = blockList;
    }

    // Getters for the file metadata
    public String getFilename() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public List<BlockNode> getBlockList() {
        return blockList;
    }

    @Override
    public String toString() {
        return "FileNode{" +
                "fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +
                ", blockCount=" + blockList.size() +
                '}';
    }
}