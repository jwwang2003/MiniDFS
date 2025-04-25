package com.lab1.distributedfs.ShellCommand;

import com.lab1.distributedfs.FileSystem.FileNode;
import com.lab1.distributedfs.Helper;
import com.lab1.distributedfs.IO.Client.Open;
import com.lab1.distributedfs.Message.Message;
import com.lab1.distributedfs.Message.MessageAction;

import java.util.List;
import java.util.NoSuchElementException;

public class CloseCommand extends Command {

    @Override
    public String getDescription() {
        return "close: Closes a file that was opened (releases the R/W lock).";
    }

    @Override
    public String getHelpMessage() {
        return """
                Usage: close
                    Closes the currently opened file and releases the R/W lock,
                    if multiple files are opened, it releases the most recently opened.
                    <pathname> - (Optional) Pathname of the file to close.""";
    }

    @Override
    public boolean handle(List<String> commandArgs) {
        if (!commandArgs.isEmpty() && commandArgs.getFirst().equals("help")) {
            System.out.println(this.getHelpMessage());
            return true;
        }

        if (commandArgs.size() > 1) {
            System.out.println("Error: invalid arguments.");
            System.out.println(this.getHelpMessage());
            return true;
        }

        String path = "";
        try { path = commandArgs.getFirst(); } catch (NoSuchElementException ignored) {}
        String[] pathParts = Helper.getPathParts(path);
        path = Helper.reconstructPathname(pathParts);

        try {
            makeRequest(MessageAction.CLOSE, path);
            Message<?> closeReply = waitForResponse();
            assert closeReply != null;

            if (closeReply.getMessageAction() == MessageAction.CLOSE && closeReply.getData() instanceof Open open) {
                System.out.printf("Closed file \"%s\".\n", open.path);
            } else {
                System.out.printf("Error: %s.\n", closeReply.getData());
                return true;
            }

            makeRequest(MessageAction.ADD, open.fileNode);
            Message<?> addReply = waitForResponse();
            assert addReply != null;

            if (addReply.getMessageAction() != MessageAction.ADD || !(addReply.getData() instanceof FileNode)) {
                throw new Exception(String.valueOf(closeReply.getData()));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            System.err.printf("Error: %s.\n", e.getMessage());
        }

        return true;
    }
}
