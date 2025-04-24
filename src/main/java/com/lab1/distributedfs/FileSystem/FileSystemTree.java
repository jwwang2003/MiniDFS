package com.lab1.distributedfs.FileSystem;

import com.lab1.distributedfs.Const;

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
        List<String> pathParts = Arrays.stream(getPathParts(path)).toList();
        pathParts = pathParts.subList(0, pathParts.size() - 1);
        path = reconstructPathname(pathParts.toArray(new String[0]));

        DirectoryNode currentDir = root;
        currentDir = this.getSubDirectory(path, currentDir, true);
        currentDir.addFile(fileNode.getFilename(), fileNode);
    }

    public FileNode rm(String pathname) {
        String[] pathParts = getPathParts(pathname);

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

        if (fileToDelete == null) { throw new IllegalArgumentException("File " + fileName + " not found in path " + pathname); }

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
        String[] pathParts = getPathParts(path);

        // If the path is empty or only contains the file name, handle it
        if (pathParts.length == 0 || pathParts[0].isEmpty()) {
            throw new IllegalArgumentException("Invalid path");
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
        throw new IllegalArgumentException("File " + fileName + " not found at path " + path);
    }

    // ================================================ HELPER =========================================================

    private DirectoryNode getSubDirectory(String path, DirectoryNode currentDir, boolean recursive) {

        String[] dirs = getPathParts(new File(path).getPath());

        // Traverse through the directories in the path
        for (String dir : dirs) {
            DirectoryNode temp = currentDir.getSubDirectory(dir);
            if (temp == null && recursive) {
                // If directory doesn't exist, create it (recursively)
                currentDir.addSubDirectory(dir, new DirectoryNode(dir));
            } else if (temp == null) {
                // If directory doesn't exist and recursive flag is false, throw exception
                throw new IllegalArgumentException("Directory " + dir + " not found");
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

    public static String[] getPathParts(String pathname) {
        // Split the pathname into directories and file name
        String[] pathParts = pathname.trim().split("/");
        // Remove any empty strings from the array
        return Arrays.stream(pathParts)
                .filter(dir -> !dir.isEmpty())
                .toArray(String[]::new);
    }

    public static String reconstructPathname(String[] pathParts) {
        // Remove any empty strings from the array
        pathParts = Arrays.stream(pathParts)
                .filter(dir -> !dir.isEmpty())
                .toArray(String[]::new);

        // Join the path parts with "/"
        String reconstructedPath = String.join("/", pathParts);

        // If you want to ensure there is no leading or trailing slash, you can remove them
        // Remove leading slash (if present)
        if (reconstructedPath.startsWith("/")) {
            reconstructedPath = reconstructedPath.substring(1);
        }

        // Remove trailing slash (if present)
        if (reconstructedPath.endsWith("/")) {
            reconstructedPath = reconstructedPath.substring(0, reconstructedPath.length() - 1);
        }

        return "/" + reconstructedPath;
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

    // ======================================== STATIC HELPER FUNCTIONS ================================================

    // Method to read the file and split it into chunks of 4KB (or whatever that is specified int the constants)
    public static List<byte[]> splitFileIntoChunks(String filePath) throws IOException {
        List<byte[]> chunks = new ArrayList<>();
        File file = new File(filePath);
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[Const.BLOCK_SIZE];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                if (bytesRead < Const.BLOCK_SIZE) {
                    // If bytesRead is less than CHUNK_SIZE, resize the buffer to the actual bytes read
                    byte[] chunk = new byte[bytesRead];
                    System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                    chunks.add(chunk);
                } else {
                    chunks.add(buffer.clone());
                }
            }
        }
        return chunks;
    }
}
