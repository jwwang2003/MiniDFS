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
    private boolean acquired;

    public FileNode(String path, String fileName, long fileSize, List<BlockNode> blockList) {
        this.path = path;
        this.filename = fileName;
        this.fileSize = fileSize;
        this.blockList = blockList;
        this.fileFormat = FileFormat.fromExtension(fileName);
        this.acquired = false;
    }

    // New constructor that extracts filename from path
    public FileNode(String path, long fileSize, List<BlockNode> blockList) {
        this.path = path;
        // Extract the filename from the path
        this.filename = new File(path).getName();
        this.fileSize = fileSize;
        this.blockList = blockList;
        this.fileFormat = FileFormat.fromExtension(filename); // Extract file format based on the filename
        this.acquired = false;
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

    public void aquire() {
        this.acquired = true;
    }

    public void release() {
        this.acquired = false;
    }

    public boolean isAquired() {
        return this.acquired;
    }

    @Override
    public String toString() {
        return "FileNode{" +
                "filename='" + filename + '\'' +
                ", fileSize=" + fileSize +
                ", blockCount=" + blockList.size() +
                ", fileFormat=" + fileFormat +  // Include file format in toString
                '}';
    }
}