package com.lab1.distributedfs.ShellCommand;

import java.util.List;

public class WriteCommand extends Command {

    @Override
    public String getDescription() {
        return "write: Writes data to the opened file.";
    }

    @Override
    public String getHelpMessage() {
        return """
                Usage: write <data>
                <data> - Data to write to the file.
                Example: write Hello, World!""";
    }

    @Override
    public boolean handle(List<String> commandArgs) {
        if (commandArgs.isEmpty()) {
            System.out.println("Error: No data provided to write.");
            return false;
        }

        String data = String.join(" ", commandArgs);
        System.out.println("Writing data to the file: " + data);
        // Implement file-writing logic here
        return true;
    }
}
