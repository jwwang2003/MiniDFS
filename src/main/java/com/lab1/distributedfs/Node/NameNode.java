package com.lab1.distributedfs.Node;

import com.lab1.distributedfs.Constants;
import com.lab1.distributedfs.FileSystem.FileSystemTree;

import java.io.File;
import java.io.IOException;

public class NameNode extends FileSystemTree {
    public NameNode() {
        super();
        // Check if the persistent file exists
        String persistFilePath = Constants.getPath(Constants.FS_IMAGE_FILE);
        File persistFile = new File(persistFilePath);

        // If the file exists, load the file system from it
        if (persistFile.exists()) {
            try {
                // Load the file system tree from the persistent file
                loadFromFile(persistFilePath);
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Failed to load the file system from the persistent file: " + e.getMessage());
            }
        } else {
            System.out.println("No persistent file found. Initializing a new file system.");
        }
    }

    public boolean persist() {
        try {
            this.saveToFile(Constants.getPath(Constants.FS_IMAGE_FILE));
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
