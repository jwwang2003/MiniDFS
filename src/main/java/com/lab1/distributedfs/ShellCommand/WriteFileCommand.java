package com.lab1.distributedfs.ShellCommand;

import java.util.List;

public class WriteFileCommand extends Command {
    @Override
    public String getDescription() {
        return "write_file: Writes data from a external file to the a \"virtual\" file.";
    }

    @Override
    public String getHelpMessage() {
        return """
                Usage: write_file <data_path> <pathname>?
                    Data read from the data file will be appended to the most
                    recently opened file or file specified by the pathname.
                    <data_path> - Path to the data file to we want to write (or append).
                    <pathname> - Pathname to the file to write (or append).""";
    }

    @Override
    public boolean handle(List<String> commandArgs) {
        if (commandArgs.isEmpty()) {
            System.out.println("Error: No data provided to write.");
            return false;
        }

        // write_txt src dest
        client.handleWriteFile(commandArgs.getFirst(), new byte[0]);

        return true;
    }
}
