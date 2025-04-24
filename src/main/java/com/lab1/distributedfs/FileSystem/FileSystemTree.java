package com.lab1.distributedfs.FileSystem;

import com.lab1.distributedfs.CONST;

import java.io.*;
import java.util.*;

public class FileSystemTree implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final DirectoryNode root;

    public FileSystemTree() {
        this.root = new DirectoryNode("root"); // root directory
    }

    public DirectoryNode getRoot() {
        return root;
    }

    // ================================================== OP ===========================================================

    public void touch(String path, String fileName, long fileSize, List<BlockNode> blockList) {
        this.touch(path, fileName, fileSize, blockList, true);
    }

    // Method to add a file to a given path
    public void touch(String path, String fileName, long fileSize, List<BlockNode> blockList, boolean recursive) {
        // Add the file to the final directory node
        FileNode fileNode = new FileNode(path, fileName, fileSize, blockList);
        this.touch(path, fileNode, recursive);
    }

    public void touch(String path, FileNode fileNode) {
        this.touch(path, fileNode, true);
    }

    public void touch(String path, FileNode fileNode, boolean recursive) {
        String[] dirs = path.trim().split("/");
        // Remove any empty strings from the array
        dirs = Arrays.stream(dirs)
                .filter(dir -> !dir.isEmpty())
                .toArray(String[]::new);

        DirectoryNode currentDir = root;
        // Traverse through the directories in the path
        for (String dir : dirs) {
            DirectoryNode temp = currentDir.getSubDirectory(dir);
            if (temp == null && recursive) {
                currentDir.addSubDirectory(dir, new DirectoryNode(dir));
            } else {
                throw new IllegalArgumentException("Directory " + dir + " not found");
            }
            currentDir = currentDir.getSubDirectory(dir);
        }

        currentDir.addFile(fileNode.getFilename(), fileNode);
    }

    public void rm(String pathname) {
        // Split the pathname into directories and file name
        String[] pathParts = pathname.trim().split("/");
        // Remove any empty strings from the array
        pathParts = Arrays.stream(pathParts)
                .filter(dir -> !dir.isEmpty())
                .toArray(String[]::new);

        // If the path is empty or only contains the file name, handle it
        if (pathParts.length == 0 || pathParts[0].isEmpty()) {
            throw new IllegalArgumentException("Invalid pathname");
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

        if (fileToDelete == null) {
            throw new IllegalArgumentException("File " + fileName + " not found in path " + pathname);
        }

        // Remove the file from the directory
        currentDir.deleteFile(fileToDelete.getFilename());

        // Check if the directory is empty and delete it if necessary
        removeEmptyDirectories(root, null);
    }

    private DirectoryNode getDirectoryNode(String[] pathParts) {
        String[] dirs = Arrays.copyOfRange(pathParts, 0, pathParts.length - 1);  // All parts except the last one

        DirectoryNode currentDir = root;

        // Traverse through the directories
        for (String dir : dirs) {
            DirectoryNode subDir = currentDir.getSubDirectory(dir);
            if (subDir == null) {
                throw new IllegalArgumentException("Directory " + dir + " not found in path " + Arrays.toString(dirs));
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

    public FileNode getFile(String pathname) {
        // Split the pathname into directories and file name
        String[] pathParts = pathname.trim().split("/");
        // Remove any empty strings from the array
        pathParts = Arrays.stream(pathParts)
            .filter(dir -> !dir.isEmpty())
            .toArray(String[]::new);

        // If the path is empty or only contains the file name, handle it
        if (pathParts.length == 0 || pathParts[0].isEmpty()) {
            throw new IllegalArgumentException("Invalid pathname");
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
        throw new IllegalArgumentException("File " + fileName + " not found at path " + pathname);
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

    // Method to read the file and split it into chunks of 4KB
    public static List<byte[]> splitFileIntoChunks(String filePath) throws IOException {
        List<byte[]> chunks = new ArrayList<>();
        File file = new File(filePath);
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[CONST.BLOCK_SIZE];
            int bytesRead;

            // Read the file in 4KB chunks
            while ((bytesRead = fis.read(buffer)) != -1) {
                if (bytesRead < CONST.BLOCK_SIZE) {
                    // If bytesRead is less than CHUNK_SIZE, resize the buffer to the actual bytes read
                    byte[] chunk = new byte[bytesRead];
                    System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                    chunks.add(chunk);
                } else {
                    // Full 4KB chunk
                    chunks.add(buffer.clone());
                }
            }
        }
        return chunks;
    }
}
