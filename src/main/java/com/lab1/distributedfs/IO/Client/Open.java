package com.lab1.distributedfs.IO.Client;

import com.lab1.distributedfs.FileSystem.BlockNode;
import com.lab1.distributedfs.FileSystem.FileNode;

import java.util.ArrayList;
import java.util.List;

public class Open {
    public OpenMode openMode;
    public String path;
    public FileNode fileNode;

    public Open(OpenMode openMode, String path) {
        this.openMode = openMode;
        this.path = path;
        this.fileNode = new FileNode(path, 0, new ArrayList<>());
    }

    public Open(OpenMode openMode, String path, FileNode fileNode) {
        this.openMode = openMode;
        this.path = path;
        this.fileNode = fileNode;
    }
}
