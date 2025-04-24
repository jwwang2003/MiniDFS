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
                    Usage: read <pathname>?
                    Reads the contents of the currently opened file or
                    the file of the pathname (if specified).
                    <pathname> - (Optional) Pathname of the file to read.""";
    }

    @Override
    public boolean handle(List<String> commandArgs) {
        if (!commandArgs.isEmpty()) {
            System.out.println("Error: read does not accept any arguments.");
            return true;
        }

        client.handleReadFile(commandArgs.getFirst());

        return true;
    }
}
