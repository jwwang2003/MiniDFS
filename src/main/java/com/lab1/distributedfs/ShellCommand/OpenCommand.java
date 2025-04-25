package com.lab1.distributedfs.ShellCommand;

import com.lab1.distributedfs.FileSystem.FileNode;
import com.lab1.distributedfs.Helper;
import com.lab1.distributedfs.IO.Client.Open;
import com.lab1.distributedfs.IO.Client.OpenMode;
import com.lab1.distributedfs.Message.Message;
import com.lab1.distributedfs.Message.MessageAction;

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
            System.out.println("Error: open expected one to two arguments.");
            return true;
        }

        if (commandArgs.getFirst().equals("help")) {
            System.out.println(this.getHelpMessage());
            return true;
        }

        String path = commandArgs.getFirst();
        String[] pathParts = Helper.getPathParts(path);
        path = Helper.reconstructPathname(pathParts);
        OpenMode mode = commandArgs.size() > 1 ? OpenMode.valueOf(commandArgs.get(1).toUpperCase()) : OpenMode.W;

        try {
            Open open = new Open(mode, path);
            Message<?> openReply;

            makeRequest(MessageAction.FIND, path);
            Message<?> findReply = waitForResponse();
            assert findReply != null;

            if (findReply.getMessageAction() == MessageAction.FILE && findReply.getData() instanceof FileNode fileNode) {
                // File found
                open = new Open(mode, path, fileNode);
            }

            if (findReply.getMessageAction() == MessageAction.FAIL && mode != OpenMode.W) {
                System.out.println(findReply.getData());
                System.out.printf("File \"%s\" does not exist! Open in write mode to create a new one.\n", path);
                return true;
            }

            makeRequest(MessageAction.OPEN, open);
            openReply = waitForResponse();
            assert openReply != null;

            if (openReply.getMessageAction() == MessageAction.OPEN)
                System.out.printf("Opened file \"%s\".\n", open.fileNode.getPath());
            else
                System.out.printf("Error: %s.\n", openReply.getData());
        } catch (IllegalArgumentException e) {
            System.out.printf("Error: invalid <mode> value: %s.\n", e.getMessage());
        } catch (InterruptedException | AssertionError e) {
            throw new RuntimeException(e);
        }
    return true;
    }
}
