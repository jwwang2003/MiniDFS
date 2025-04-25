package com.lab1.distributedfs.ShellCommand;

import com.lab1.distributedfs.Const;
import com.lab1.distributedfs.FileSystem.BlockNode;
import com.lab1.distributedfs.FileSystem.FileNode;
import com.lab1.distributedfs.Helper;
import com.lab1.distributedfs.IO.Client.Open;
import com.lab1.distributedfs.IO.Client.OpenMode;
import com.lab1.distributedfs.IO.DataNodeIO.WriteRequest;
import com.lab1.distributedfs.Message.Message;
import com.lab1.distributedfs.Message.RequestType;
import com.lab1.distributedfs.Message.ResponseType;
import com.lab1.distributedfs.Node.DataNodeStatus;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class WriteCommand extends Command {
    @Override
    public String getDescription() {
        return "write: Writes data from command argument to a \"virtual\" file.";
    }

    @Override
    public String getHelpMessage() {
        return """
                Usage: write <data> <pathname>?
                    String data defined in <data> will be appended to the most
                    recently opened file or file specified by the pathname.
                    <data> - Any string of data.
                    <pathname> - Pathname to the file to write (or append).""";
    }

    @Override
    public boolean handle(List<String> commandArgs) {
        if (commandArgs.isEmpty()) {
            System.out.println("Error: No data provided to write.");
            return true;
        }

        String data = commandArgs.get(0).trim().strip();
        String path = commandArgs.size() > 1 ? commandArgs.get(1) : "";
        String[] pathParts = Helper.getPathParts(path);
        path = Helper.reconstructPathname(pathParts);

        try {
            // First: retrieve Open object from the client thread
            requestQueue.put(new Message<>(RequestType.OPEN, new Open(OpenMode.W, path)));
            Message<?> openReply = waitForResponse();
            assert openReply != null;

            if (openReply.getResponseType() != ResponseType.OPEN) throw new Exception(String.valueOf(openReply.getData()));
            assert openReply.getResponseType() == ResponseType.OPEN && openReply.getData() instanceof Open;

            Open open = (Open) openReply.getData();

            System.out.printf("Writing data \"%s\" to file \"%s\"\n", data, open.fileNode.getPath());

            // Second: retrieve valid (active) data nodes that was can write to
            requestQueue.put(new Message<>(RequestType.STAT, null));
            Message<?> statReply = waitForResponse();
            assert statReply != null;

            if (statReply.getResponseType() != ResponseType.STAT) throw new Exception(String.valueOf(statReply.getData()));
            Object raw = statReply.getData();
            if (!(raw instanceof Map<?, ?> temp)) { throw new Exception("expected a Map but got " + raw.getClass()); }

            // Now we know it’s some kind of Map
            Map<Integer,DataNodeStatus> dataNodeStatuses = new HashMap<>();
            for (Map.Entry<?,?> e : temp.entrySet()) {
                // optionally check key/value types
                if (!(e.getKey() instanceof Integer) || !(e.getValue() instanceof DataNodeStatus)) {
                    throw new Exception("bad entry: " + e);
                }
                dataNodeStatuses.put((Integer)e.getKey(), (DataNodeStatus)e.getValue());
            }

            // Debugging purposes
//            dataNodeStatuses.forEach((key, value) ->
//                System.out.println(value)
//            );

            // Third: determine which data nodes store which block of data (remember to update the block node as well)
            // Using round-robin approach?
            List<Map.Entry<Integer,DataNodeStatus>> sortedEntries = dataNodeStatuses.entrySet().stream()
                .filter(e -> e.getValue().alive)
                .sorted(Comparator
                    .comparingInt((Map.Entry<Integer,DataNodeStatus> e) -> e.getValue().blockCount)
                    .thenComparingLong(e -> e.getValue().storageUsed)
                    .thenComparing(Map.Entry::getKey)
                )
                .toList();

            // Extract just the node IDs in the sorted order
            List<Integer> nodeIds = sortedEntries.stream()
                .map(Map.Entry::getKey)
                .toList();

            // Sanity check: need at least `replicationFactor` alive nodes
            if (nodeIds.size() < Const.REPLICATION_FACTOR) {
                throw new Exception("not enough alive DataNodes for replication");
            }

            // Final step: send out the write tasks

            // Convert the string data into a byte array using UTF-8 encoding
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);

            FileNode fileNode = open.fileNode;
            String filename = Helper.reconstructPathname(Helper.getPathParts(open.fileNode.getFilename()));

            int startBlockID = 0;
            for (BlockNode blockNode : fileNode.getBlockList()) {
                if (blockNode.getFreeSpace() > 0) {
                    // Found a block is still has space
                    int freeSpace = blockNode.getFreeSpace();
                    int bytesToWrite = Math.min(freeSpace, dataBytes.length);
                    byte[] dataToWrite = Arrays.copyOfRange(dataBytes, 0, bytesToWrite);

                    List<Integer> replicas = blockNode.getReplicas();
                    for(int i = 0; i < replicas.size(); i++) {
                        WriteRequest writeRequest = new WriteRequest(
                            replicas.get(i),
                            i,
                            open.path,
                            blockNode.getBlockID(),
                            dataToWrite
                        );
                        // Send the write command off to that DataNode
                        requestQueue.put(new Message<>(RequestType.WRITE, writeRequest));
                        Message<?> writeReply = waitForResponse();
                        assert writeReply != null;
                    }
                    dataBytes = Arrays.copyOfRange(dataBytes, bytesToWrite, dataBytes.length);
                }
            }

            List<byte[]> chunks = Helper.splitDataIntoChunks(dataBytes);
            for (int chunkIdx = 0; chunkIdx < chunks.size(); chunkIdx++) {
                List<Integer> replicas = new ArrayList<>();
                byte[] chunkData = chunks.get(chunkIdx);
                // pick N distinct targets in round-robin fashion
                for (int r = 0; r < Const.REPLICATION_FACTOR; r++) {
                    int nodeIndex     = (chunkIdx + r) % nodeIds.size();
                    int dataNodeId    = nodeIds.get(nodeIndex);
                    // this goes into the “replica” field of your filename
                    replicas.add(dataNodeId);
                    // build a WriteRequest (a subclass of Block)
                    WriteRequest wr = new WriteRequest(
                        /* dataNodeID = */ dataNodeId,
                        /* replica    = */ r,
                        /* pathname   = */ open.path,
                        /* blockID    = */ startBlockID + chunkIdx,
                        /* data       = */ chunkData
                    );
                    // Send the write command off to that DataNode
                    requestQueue.put(new Message<>(RequestType.WRITE, wr));
                    Message<?> writeReply = waitForResponse();
                    assert writeReply != null;
                }
                open.fileNode.getBlockList().add(new BlockNode(chunkIdx, filename, data.length(), replicas));
            }

            // Forth: send all the requests to data nodes & wait for response
            // If a timeout occurs, that might mean the data node is offline, elect another data node to store the block\

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return true;
        }
        return true;
    }
}
