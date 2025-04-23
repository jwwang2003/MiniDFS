package com.lab1.distributedfs.ShellCommand;

import java.util.List;

public class ReadCommand extends Command {

    @Override
    public String getDescription() {
        return "read: Reads the content of the opened file.";
    }

    @Override
    public String getHelpMessage() {
        return """
                Usage: read
                Reads the content of the currently opened file.
                Example: read""";
    }

    @Override
    public boolean handle(List<String> commandArgs) {
        if (!commandArgs.isEmpty()) {
            System.out.println("Error: read does not accept any arguments.");
            return false;
        }

        // Implement read file logic here
        System.out.println("Reading the content of the file...");
        return true;
    }
}
