package com.lab1.distributedfs.FileSystem;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectoryNode implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String directoryName;
    private final Map<String, DirectoryNode> subDirectories; // A map of subdirectories
    private final Map<String, FileNode> files;  // A map of files

    public DirectoryNode(String directoryName) {
        this.directoryName = directoryName;
        this.subDirectories = new HashMap<>();
        this.files = new HashMap<>();
    }

    public String getDirectoryName() {
        return this.directoryName;
    }

    // Add a subdirectory
    public void addSubDirectory(String name, DirectoryNode subDirectory) {
        subDirectories.put(name, subDirectory);
    }

    // Add a file
    public void addFile(String name, FileNode file) {
        files.put(name, file);
    }

    public List<FileNode> getFiles() {
        return files.values().stream().toList();
    }

    public Collection<DirectoryNode> getSubDirectories() {
        return subDirectories.values();
    }

    // Get the subdirectory by its name
    public DirectoryNode getSubDirectory(String dirName) {
        return subDirectories.get(dirName);
    }

    // Display the directory structure
    public String displayStructure(String indent) {
        StringBuilder sb = new StringBuilder();

        // Append the directory information
        sb.append(indent).append("Directory: ").append(directoryName).append("\n");

        // Append files in the directory
        for (FileNode file : files.values()) {
            sb.append(indent).append("  ").append(file).append("\n");
        }

        // Recursively append subdirectories
        for (DirectoryNode subDir : subDirectories.values()) {
            sb.append(subDir.displayStructure(indent + "  "));
        }

        // Return the accumulated string
        return sb.toString();
    }

    // Delete a file by its name
    public void deleteFile(String fileName) {
        if (files.containsKey(fileName)) {
            files.remove(fileName);
        } else {
            throw new IllegalArgumentException("File " + fileName + " not found in directory " + directoryName);
        }
    }

    // Delete a subdirectory by its name
    public void deleteSubDirectory(String dirName) {
        if (subDirectories.containsKey(dirName)) {
            DirectoryNode subDir = subDirectories.get(dirName);
            // If the subdirectory is empty, delete it
            if (subDir.getFiles().isEmpty() && subDir.getSubDirectories().isEmpty()) {
                subDirectories.remove(dirName);
            } else {
                throw new IllegalArgumentException("Subdirectory " + dirName + " is not empty and cannot be deleted.");
            }
        } else {
            throw new IllegalArgumentException("Subdirectory " + dirName + " not found in directory " + directoryName);
        }
    }
}
