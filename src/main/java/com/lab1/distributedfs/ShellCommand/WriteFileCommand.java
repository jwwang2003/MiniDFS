package com.lab1.distributedfs.ShellCommand;

import com.lab1.distributedfs.Helper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class WriteFileCommand extends Command {
    @Override
    public String getDescription() {
        return "write_file: Writes data from an external file to a \"virtual\" file.";
    }

    @Override
    public String getHelpMessage() {
        return """
                Usage: write_file <data_path> <pathname>?
                    Data read from the data file will be appended to the most
                    recently opened file or file specified by the pathname.
                    <data_path> - Path to the data file we want to write (or append).
                    <pathname> - Pathname to the file to write (or append).""";
    }

    @Override
    public boolean handle(List<String> commandArgs) {
        if (commandArgs.isEmpty()) {
            System.out.println("Error: write_file expects at least one argument.");
            return false;
        }

        String dataFilePath = commandArgs.get(0);
        String virtualFilePath = commandArgs.size() > 1 ? commandArgs.get(1) : null;

        try {
            // Read and split the file into chunks
            List<byte[]> chunks = Helper.splitFileIntoChunks(dataFilePath);

            System.out.println(chunks.size());

            // Convert the first chunk back to a string (UTF-8)
            String chunkString = new String(chunks.getFirst(), StandardCharsets.UTF_8);

            // Print the chunk as a string
            System.out.println("First chunk in UTF-8 format: ");
            System.out.println(chunkString);

            // Process the chunks and write them to the virtual file
//            client.handleWriteFile(chunks, virtualFilePath);
        } catch (IOException e) {
            System.out.println("Error reading data file: " + e.getMessage());
            return true;
        }

        return true;
    }
}
