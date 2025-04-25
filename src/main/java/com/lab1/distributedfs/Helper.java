package com.lab1.distributedfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Helper {
    // ======================================== STATIC HELPER FUNCTIONS ================================================
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

    public static List<byte[]> splitDataIntoChunks(byte[] data) throws IOException {
        List<byte[]> chunks = new ArrayList<>();
        int dataLength = data.length;
        int chunkSize = Const.BLOCK_SIZE; // Assuming BLOCK_SIZE is the chunk size (e.g., 4KB)

        // Process the data in chunks
        for (int i = 0; i < dataLength; i += chunkSize) {
            // Calculate the remaining bytes to read
            int remaining = dataLength - i;
            int currentChunkSize = Math.min(chunkSize, remaining);

            byte[] chunk = new byte[currentChunkSize];
            System.arraycopy(data, i, chunk, 0, currentChunkSize);
            chunks.add(chunk);
        }

        return chunks;
    }
}
