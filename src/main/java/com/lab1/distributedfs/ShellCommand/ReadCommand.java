package com.lab1.distributedfs.ShellCommand;

import com.lab1.distributedfs.FileSystem.BlockNode;
import com.lab1.distributedfs.FileSystem.FileNode;
import com.lab1.distributedfs.Helper;
import com.lab1.distributedfs.IO.Client.Open;
import com.lab1.distributedfs.IO.Client.OpenMode;
import com.lab1.distributedfs.IO.DataNodeIO.ReadRequest;
import com.lab1.distributedfs.IO.DataNodeIO.ReadResponse;
import com.lab1.distributedfs.Message.Message;
import com.lab1.distributedfs.Message.RequestType;
import com.lab1.distributedfs.Message.ResponseType;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

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
        if (!commandArgs.isEmpty() && commandArgs.get(0).equals("help")) {
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
            // Request to open the file
            requestQueue.put(
                    new Message<>(RequestType.OPEN, new Open(OpenMode.R, path))
            );
            Message<?> openReply = waitForResponse();
            assert openReply != null;

            // Ensure the file was successfully opened
            if (openReply.getResponseType() != ResponseType.OPEN) {
                throw new Exception(String.valueOf(openReply.getData()));
            }
            Open open = (Open) openReply.getData();
            FileNode fileNode = open.fileNode;

            List<byte[]> rawDataStream  = new ArrayList<>();
            // Iterate over the blocks in the file and request their data
            for (BlockNode blockNode : fileNode.getBlockList()) {
                // Send request for the block data to the appropriate data node
                List<Integer> replicas = blockNode.getReplicas();
                for (int i = 0; i < replicas.size(); i++) {
                    requestQueue.put(
                        new Message<>(RequestType.READ, new ReadRequest(
                            replicas.get(i),
                            i,
                            blockNode.getFilename(),
                            blockNode.getBlockID()
                        ))
                    );

                    try {
                        Message<?> readReply = waitForResponse();
                        assert readReply != null;
                        if (readReply.getResponseType() != ResponseType.READ && !(readReply.getData() instanceof ReadResponse)) {
                            throw new Exception(String.valueOf(readReply.getData()));
                        }
                        ReadResponse readResponse = (ReadResponse) readReply.getData();
                        rawDataStream.add(readResponse.getBytes());
                        break;
                    } catch (InterruptedException ignored) {

                    }
                }
            }

            // Display the data read from file into the console
            System.out.printf("Data read from file \"%s\":\n", path);
            for (int i = 0; i < rawDataStream.size(); i++) {
                String stringData = new String(rawDataStream.get(i));
                System.out.printf("Block %s content: %s\n", i, stringData);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        return true;
    }
}
