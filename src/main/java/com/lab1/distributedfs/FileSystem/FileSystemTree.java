package com.lab1.distributedfs.FileSystem;

import com.lab1.distributedfs.Helper;

import java.io.*;
import java.util.*;

public class FileSystemTree implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final DirectoryNode root;

    public FileSystemTree() {
        this.root = new DirectoryNode("/"); // root directory
    }

    public DirectoryNode getRoot() {
        return root;
    }

    // ================================================== OP ===========================================================

    public void touch(FileNode fileNode) {
        String path = fileNode.getPath();
        List<String> pathParts = Arrays.stream(Helper.getPathParts(path)).toList();
        pathParts = pathParts.subList(0, pathParts.size() - 1);
        path = Helper.reconstructPathname(pathParts.toArray(new String[0]));

        DirectoryNode currentDir = root;
        currentDir = this.getSubDirectory(path, currentDir);
        currentDir.addFile(fileNode.getFilename(), fileNode);
    }

    public FileNode rm(String pathname) {
        String[] pathParts = Helper.getPathParts(pathname);

        // If the path is empty or only contains the file name, handle it
        if (pathParts.length == 0 || pathParts[0].isEmpty()) {
            throw new IllegalArgumentException("invalid pathname");
        }

        // Separate the directory path from the file name
        String fileName = pathParts[pathParts.length - 1];
        DirectoryNode currentDir = getDirectoryNode(pathParts);

        // Delete the file from the final directory
        FileNode fileToDelete = null;
        for (FileNode file : currentDir.getFiles()) {
            if (file.getFilename().equals(fileName)) {
                fileToDelete = file;
                break;
            }
        }

        if (fileToDelete == null) { throw new IllegalArgumentException("file \"" + fileName + "\" not found in path \"" + pathname + "\""); }

        // Remove the file from the directory
        currentDir.deleteFile(fileToDelete.getFilename());

        // Check if the directory is empty and delete it if necessary
        removeEmptyDirectories(root, null);

        return fileToDelete;
    }

    // ================================================ SEARCH =========================================================

    public FileNode findFile(String fileName) {
        return findFileInDirectory(root, fileName);
    }

    private FileNode findFileInDirectory(DirectoryNode dir, String fileName) {
        // Check each file in the current directory
        for (FileNode file : dir.getFiles()) {
            if (file.getFilename().equals(fileName)) {
                return file;  // Return the file if found
            }
        }

        // Recurse into subdirectories
        for (DirectoryNode subDir : dir.getSubDirectories()) {
            FileNode foundFile = findFileInDirectory(subDir, fileName);
            if (foundFile != null) {
                return foundFile;  // Return the file if found in subdirectory
            }
        }

        // If not found in any directory or subdirectory, return null or throw exception
        return null;  // Alternatively, throw an exception if file is not found
    }

    public FileNode getFile(String path) {
        String[] pathParts = Helper.getPathParts(path);

        // If the path is empty or only contains the file name, handle it
        if (pathParts.length == 0 || pathParts[0].isEmpty()) {
            throw new IllegalArgumentException("invalid path");
        }

        // Separate the directory path from the file name
        String fileName = pathParts[pathParts.length - 1];
        DirectoryNode currentDir = getDirectoryNode(pathParts);

        // Check for the file in the final directory
        for (FileNode file : currentDir.getFiles()) {
            if (file.getFilename().equals(fileName)) {
                return file;  // Return the file if found
            }
        }

        // If the file is not found, return null or throw an exception
        throw new IllegalArgumentException("file \"" + fileName + "\" not found at path \"" + path + "\"");
    }

    // ================================================ HELPER =========================================================

    private DirectoryNode getSubDirectory(String path, DirectoryNode currentDir) {

        String[] dirs = Helper.getPathParts(new File(path).getPath());

        // Traverse through the directories in the path
        for (String dir : dirs) {
            DirectoryNode temp = currentDir.getSubDirectory(dir);
            if (temp == null) {
                currentDir.addSubDirectory(dir, new DirectoryNode(dir));
            }

            // Update the current directory to the next subdirectory
            currentDir = currentDir.getSubDirectory(dir);
        }
        return currentDir;
    }

    private DirectoryNode getDirectoryNode(String[] pathParts) {
        String[] dirs = Arrays.copyOfRange(pathParts, 0, pathParts.length - 1);  // All parts except the last one
        DirectoryNode currentDir = root;

        // Traverse through the directories
        for (String dir : dirs) {
            DirectoryNode subDir = currentDir.getSubDirectory(dir);
            if (subDir == null) {
                throw new IllegalArgumentException("directory \"" + dir + "\" not found in path \"" + Arrays.toString(dirs) + "\"");
            }
            currentDir = subDir;  // Move to the next level
        }
        return currentDir;
    }

    // Helper method to remove empty directories
    private void removeEmptyDirectories(DirectoryNode dir, DirectoryNode parentDir) {
        // If the current directory has no files or subdirectories, we can remove it
        if (dir.getFiles().isEmpty() && dir.getSubDirectories().isEmpty()) {
            if (parentDir != null) {
                parentDir.deleteSubDirectory(dir.getDirectoryName()); // Remove the empty directory from its parent
            }
        } else {
            // Recurse into subdirectories with the current directory as the parent
            for (DirectoryNode subDir : dir.getSubDirectories()) {
                removeEmptyDirectories(subDir, dir);
            }
        }
    }

    // ================================================ OUTPUT =========================================================

    // Method to display the file system structure
    public String displayFileSystem() {
        return root.displayStructure("");
    }

    // ================================================ PERSIST ========================================================

    // Method to serialize the file system into a file
    public void saveToFile(String filename) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
            out.writeObject(this);
        }
    }

    // Method to deserialize the file system from a file
    public static FileSystemTree loadFromFile(String filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            return (FileSystemTree) in.readObject();
        }
    }
}
