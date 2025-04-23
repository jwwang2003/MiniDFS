package com.lab1.distributedfs.ShellCommand;

import java.util.List;

public class CloseCommand extends Command {

    @Override
    public String getDescription() {
        return "close: Closes a file that was opened (releases the R/W lock).";
    }

    @Override
    public String getHelpMessage() {
        return """
                Usage: close
                Closes the currently opened file and releases the R/W lock.""";
    }

    @Override
    public boolean handle(List<String> commandArgs) {
        if (!commandArgs.isEmpty() && commandArgs.getFirst().equals("help")) {
            System.out.println(this.getHelpMessage());
            return true;
        }

        if (!commandArgs.isEmpty()) {
            System.out.println("Error: close does not accept any arguments.");
            return true;
        }

        // Implement file-closing logic here
        System.out.println("Closing the file...");
        return true;
    }
}
