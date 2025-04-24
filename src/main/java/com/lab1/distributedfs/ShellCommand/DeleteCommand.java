package com.lab1.distributedfs.ShellCommand;

import java.util.List;

public class DeleteCommand extends Command{
    @Override
    public String getDescription() {
        return "delete: Delete a file in the \"virtual\" filesystem";
    }

    @Override
    public String getHelpMessage() {
        return """
                Usage: delete <pathname>?
                    Deletes the data of the most recently opened file or
                    the file specified by the pathname (if specified).
                    <pathname> - (Optional) Pathname of the file to delete.""";
    }

    @Override
    public boolean handle(List<String> commandArgs) {
        if (!commandArgs.isEmpty() && commandArgs.getFirst().equals("help")) {
            System.out.println(this.getHelpMessage());
            return true;
        }

        return true;
    }
}
