package com.lab1.distributedfs.ShellCommand;

import com.lab1.distributedfs.FileSystem.FileNode;
import com.lab1.distributedfs.FileSystem.FileSystemTree;
import com.lab1.distributedfs.Message.Message;
import com.lab1.distributedfs.Message.RequestType;
import com.lab1.distributedfs.Message.ResponseType;

import java.util.List;
import java.util.NoSuchElementException;

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

        if (commandArgs.size() > 1) {
            System.out.println("Error: delete accepts one or no argument.");
            return true;
        }

        String path = "";
        try { path = commandArgs.getFirst(); } catch (NoSuchElementException ignored) {}

        String[] pathParts = FileSystemTree.getPathParts(path);
        path = FileSystemTree.reconstructPathname(pathParts);

        try {
            requestQueue.put(new Message<>(RequestType.DELETE, path));
            Message<?> deleteReply = waitForResponse();
            assert deleteReply != null;

            if (deleteReply.getResponseType() == ResponseType.DELETE && deleteReply.getData() instanceof FileNode fileNode) {
                System.out.println("Deleted file node \"" + fileNode.getPath() + "\"");

                // TODO: Remove the blocks from the DataNode(s)
            } else {
                System.out.println("Error: " + deleteReply.getData());
                return true;
            }


        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return true;
    }
}
