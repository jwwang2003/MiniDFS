package com.lab1.distributedfs.FileSystem;

public enum FileFormat {
    TXT("txt"),
    BIN("bin");

    private final String extension;

    FileFormat(String extension) {
        this.extension = extension;
    }

    // Method to determine the file format based on extension
    public static FileFormat fromExtension(String filename) {
        // Extract the file extension
        String fileExtension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        // Match the file extension to the enum values
        for (FileFormat format : values()) {
            if (format.extension.equals(fileExtension)) {
                return format;
            }
        }
        // Default to BIN if no match is found
        return BIN;
    }
}