package com.lab1.distributedfs.FileSystem;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

// Class to represent the FileNode (which stores file metadata)
public class FileNode implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String path;
    private final String filename;
    private final long fileSize;
    private final List<BlockNode> blockList;
    private final FileFormat fileFormat; // Variable to hold the file format

    // New constructor that extracts filename from path
    public FileNode(String path, List<BlockNode> blockList) {
        this.path = path;
        // Extract the filename from the path
        this.filename = new File(path).getName();
        // Determine file size
        long tempSize = 0;
        for (BlockNode blockNode : blockList) { tempSize += blockNode.getSize(); }
        this.fileSize = tempSize;
        this.blockList = blockList;
        this.fileFormat = FileFormat.fromExtension(filename); // Extract file format based on the filename
    }

    public String getPath() {
        return path;
    }

    // Getters for the file metadata
    public String getFilename() {
        return filename;
    }

    public long getFileSize() {
        return fileSize;
    }

    public List<BlockNode> getBlockList() {
        return blockList;
    }

    public FileFormat getFileFormat() {
        return fileFormat; // Getter to access the file format
    }

    @Override
    public String toString() {
        return "FileNode{" +
                "path='" + path + '\'' +
                ", filename='" + filename + '\'' +
                ", fileSize=" + fileSize +
                ", blockCount=" + blockList.size() +
                ", fileFormat=" + fileFormat +  // Include file format in toString
                '}';
    }
}