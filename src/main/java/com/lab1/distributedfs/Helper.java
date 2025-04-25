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
    // Method to read the file into a byte array
    public static byte[] readFileIntoByteArray(String filePath) throws IOException {
        File file = new File(filePath);
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] fileData = new byte[(int) file.length()];
            fis.read(fileData);
            return fileData;
        }
    }

    // Method to split the byte array into chunks
    public static List<byte[]> splitFileIntoChunks(byte[] fileData) {
        List<byte[]> chunks = new ArrayList<>();
        int chunkSize = Const.BLOCK_SIZE;
        int totalChunks = (int) Math.ceil((double) fileData.length / chunkSize);

        for (int i = 0; i < totalChunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, fileData.length);

            byte[] chunk = Arrays.copyOfRange(fileData, start, end);
            chunks.add(chunk);
        }
        return chunks;
    }

    public static List<byte[]> splitDataIntoChunks(byte[] data) {
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
