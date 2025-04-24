package com.lab1.distributedfs.ShellCommand;

import java.util.List;

public class WriteCommand extends Command {
    @Override
    public String getDescription() {
        return "write: Writes data from command argument to a \"virtual\" file.";
    }

    @Override
    public String getHelpMessage() {
        return """
                Usage: write <data> <pathname>?
                    String data defined in <data> will be appended to the most
                    recently opened file or file specified by the pathname.
                    <data> - Any string of data.
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
