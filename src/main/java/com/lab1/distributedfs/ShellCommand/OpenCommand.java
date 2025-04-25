package com.lab1.distributedfs.ShellCommand;

import com.lab1.distributedfs.FileSystem.FileNode;
import com.lab1.distributedfs.FileSystem.FileSystemTree;
import com.lab1.distributedfs.Helper;
import com.lab1.distributedfs.IO.Client.Open;
import com.lab1.distributedfs.IO.Client.OpenMode;
import com.lab1.distributedfs.Message.Message;
import com.lab1.distributedfs.Message.RequestType;
import com.lab1.distributedfs.Message.ResponseType;

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
            String path = commandArgs.getFirst();
            String[] pathParts = Helper.getPathParts(path);
            path = Helper.reconstructPathname(pathParts);

            OpenMode mode = commandArgs.size() > 1 ? OpenMode.valueOf(commandArgs.get(1).toUpperCase()) : OpenMode.W;
            try {
                Open open = new Open(mode, path);
                Message<?> openReply = null;

                requestQueue.put(new Message<>(RequestType.FIND, path));
                Message<?> findReply = waitForResponse();
                assert findReply != null;

                if (findReply.getResponseType() == ResponseType.FOUND && findReply.getData() instanceof FileNode fileNode) {
                    // File found
                    open = new Open(mode, path, fileNode);
                    System.out.println("Found file: " + fileNode.getPath());
                }

                if (findReply.getResponseType() == ResponseType.NOTFOUND && mode != OpenMode.W) {
                    System.out.println(findReply.getData());
                    System.out.printf("File \"%s\" does not exist! Open in write mode to create a new one\n", path);
                    return true;
                }

                requestQueue.put(
                    new Message<>(RequestType.OPEN, open)
                );
                openReply = waitForResponse();
                assert openReply != null;

                if (openReply.getResponseType() == ResponseType.OPEN) {
                    System.out.println("Opened file \"" + open.fileNode.getPath() + "\"");
                } else {
                    System.out.println("Error: " + openReply.getData());
                }
            } catch (InterruptedException | AssertionError e) {
                throw new RuntimeException(e);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Error: invalid <mode> value: " + e.getMessage());
        }

    return true;
    }
}
