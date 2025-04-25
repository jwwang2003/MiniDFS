package com.lab1.distributedfs.ShellCommand;

import com.lab1.distributedfs.FileSystem.BlockNode;
import com.lab1.distributedfs.FileSystem.FileNode;
import com.lab1.distributedfs.Helper;
import com.lab1.distributedfs.IO.DataNodeIO.WriteRequest;
import com.lab1.distributedfs.IO.DataNodeIO.WriteResponse;
import com.lab1.distributedfs.Message.Message;
import com.lab1.distributedfs.Message.MessageAction;

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
                Usage: delete <pathname>
                    Deletes the data of the most recently opened file or
                    the file specified by the pathname (if specified).
                    <pathname> - Pathname of the file to delete.""";
    }

    @Override
    public boolean handle(List<String> commandArgs) {
        if (!commandArgs.isEmpty() && commandArgs.getFirst().equals("help")) {
            System.out.println(this.getHelpMessage());
            return true;
        }

        if (commandArgs.isEmpty()) {
            System.out.println("Error: delete requires at least one argument.");
            return true;
        }

        String path = "";
        try { path = commandArgs.getFirst(); } catch (NoSuchElementException ignored) {}
        String[] pathParts = Helper.getPathParts(path);
        path = Helper.reconstructPathname(pathParts);

        try {
            makeRequest(MessageAction.DELETE, path);
            Message<?> deleteReply = waitForResponse();
            assert deleteReply != null;

            if (deleteReply.getMessageAction() != MessageAction.DELETE)  throw new Exception("delete failed");

            FileNode fileNode = (FileNode) deleteReply.getData();

            // Remove the blocks from the DataNode(s)
            for (BlockNode blockNode: fileNode.getBlockList()) {
                List<Integer> replicas = blockNode.getReplicas();
                for (int i = 0; i < replicas.size(); i++) {
                    WriteRequest wr = new WriteRequest(replicas.get(i), i, fileNode.getPath(), blockNode.getBlockID(), null);
                    makeRequest(MessageAction.WRITE, wr);
                    Message<?> blockDeleteReply = waitForResponse();
                    assert blockDeleteReply != null;
                    if (blockDeleteReply.getData() instanceof WriteResponse writeResponse) {
                        if (blockDeleteReply.getMessageAction() != MessageAction.WRITE || writeResponse.getNumBytesWritten() < 0)
                            throw new Exception("delete block failed");
                    } else
                        throw new Exception("delete block failed (invalid response)");
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            System.out.printf("Error: %s.\n", e.getMessage());
        }
        return true;
    }
}
