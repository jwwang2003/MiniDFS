package com.lab1.distributedfs.ShellCommand;

import com.lab1.distributedfs.BlockIOOP.IOMode;

import java.util.List;

public class OpenCommand extends Command {
    @Override
    public String getDescription() {
        return "open: Opens a file within the virtual FS in a given mode";
    }

    @Override
    public String getHelpMessage() {
        return """
                Usage: open <filename> <mode>?
                <filename> - Name of the file to open.
                <mode>? - (optional) Mode to open the file in (e.g., R(read), W(write)).""";
    }

    @Override
    public boolean handle(List<String> commandArgs) {
        if (commandArgs.isEmpty()) {
            System.out.println("Error: expected one to two arguments: open <filename> <mode>");
            return true;
        }

        if (commandArgs.get(0).equals("help")) {
            System.out.println(this.getHelpMessage());
            return true;
        }

        try {
            String filename = commandArgs.get(0);
            IOMode mode = commandArgs.size() > 1 ? IOMode.valueOf(commandArgs.get(1).toUpperCase()) : null;
            System.out.println("Opening file: " + filename + " with mode: " + mode);
            // Add your file-opening logic here
        } catch (IllegalArgumentException e) {
            System.out.println("Error: invalid <mode> value: " + e.getMessage());
        }

        return true;
    }
}
