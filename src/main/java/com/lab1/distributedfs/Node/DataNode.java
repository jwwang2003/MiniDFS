package com.lab1.distributedfs.Node;

import com.lab1.distributedfs.BlockIOOP.*;
import com.lab1.distributedfs.Constants;
import com.lab1.distributedfs.Message.Message;
import com.lab1.distributedfs.Message.MessageType;
import com.lab1.distributedfs.Message.RequestType;
import com.lab1.distributedfs.Message.ResponseType;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.nio.file.Paths;
import java.nio.file.Path;

/*
The DataNode manages the storage for multiple blocks, each bloch has its own fixed size.
Simulating the concept of a "distributed file system", but in this case, the virtual file system
we are implementing is not distributed between multiple disks/servers, but just between different files.

Features:
- Supported operations
    - Write
    - Read
    - Delete
- Heartbeat
- Exit (stop current thread)
 */

public class DataNode implements Runnable {
    private final BlockingQueue<Message<?>> messageQueue;
    private final BlockingQueue<Message<?>> responseQueue;
    private final int nodeID;
    private File storageDir;

    public DataNode(BlockingQueue<Message<?>> messageQueue, BlockingQueue<Message<?>> responseQueue, int nodeID) {
        this.messageQueue = messageQueue;
        this.responseQueue = responseQueue;
        this.nodeID = nodeID;
    }

    @Override
    public void run() {
        // Create directory to store blocks if it doesn't exist
        // node#/...
        String nodeDir = String.format("node%s", nodeID);
        String path = Paths.get(Constants.DATANODE_ROOT_DIR, nodeDir).toString();
        this.storageDir = new File(path);
        if (!storageDir.exists()) {
            boolean res = storageDir.mkdirs();
        }

        try {
            while (true) {
                // Polling is used to detect timeouts
                Message<?> message = messageQueue.poll(Constants.WORKER_TIMEOUT, TimeUnit.MILLISECONDS);
                if (message != null) {
                    this.handleMessage(message);
                } else {
                    // Theoretically, the main thread (name server) should be sending a heartbeat request every second.
                    // Therefore, if the worker thread did not receive anything for more than that, there is a problem.
                    // Handle timeout situation (e.g., main thread crashed or delayed)

                    System.out.printf("DataNode %s detected timeout from main thread (NameNode)", this.nodeID);
                    break;  // Optionally exit the loop or restart worker thread
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getNodeID() {
        return nodeID;
    }

    // ========================================== INTERNAL FUNCTIONS ===================================================
    private void handleMessage(Message<?> message) throws InterruptedException {
        if (message.getMessageType() != MessageType.Request) {
            // DataNoes are "worker threads", they only accept request messages.
            // Therefore, if the message is NOT a request, then something must have gone horribly wrong.
            throw new RuntimeException("Invalid message type: " + message.getMessageType());
        }

        // Determine the request type
        switch (message.getRequestType()) {
            case RequestType.READ -> handleReadRequest(message);
            case RequestType.WRITE -> handleWriteRequest(message);
            case RequestType.DELETE -> handleDeleteRequest(message);
            case RequestType.HEARTBEAT -> handleHeartbeat();
            case RequestType.EXIT -> handleExit();
            default -> {
                throw new RuntimeException("Invalid request type: " + message.getRequestType());
            }
        }
    }

    private void handleReadRequest(Message<?> message) throws InterruptedException {
        if (message.getData() instanceof ReadRequest readRequest) {
            // Construct the path to the requested block file
            String blockFileName = readRequest.getFilename();
            Path path = Paths.get(storageDir.getPath(), blockFileName);

            // Check if the file exists
            File file = path.toFile();
            if (!file.exists()) {
                // If the file doesn't exist, respond with failure
                String errorMessage = String.format("Error: file not found (node%s, %s)", nodeID, blockFileName);
                this.responseQueue.put(new Message<>(ResponseType.NOTFOUND, errorMessage));
                return;
            }

            try {
                // Read the file data into a byte array
                byte[] fileData = java.nio.file.Files.readAllBytes(path);
                // Send the data back via the response queue
                this.responseQueue.put(
                    new Message<>(ResponseType.SUCCESS, new ReadResponse(readRequest, fileData))
                );
            } catch (IOException e) {
                String errorMessage = String.format(
                    "Error: while reading block (node%s, %s): %s%n\n",
                    nodeID,
                    readRequest.getFilename(),
                    e.getMessage()
                );
                this.responseQueue.put(
                    new Message<>(
                        ResponseType.FAIL, errorMessage + Arrays.toString(e.getStackTrace())
                    )
                );
            }
        } else {
            // If the data is not of the expected type, handle it gracefully
            String errorMessage = String.format("Error: unexpected data type: %s", message.getData().getClass().getName());
            this.responseQueue.put(
                new Message<>(
                    ResponseType.FAIL, errorMessage
                )
            );
        }
    }

    private void handleWriteRequest(Message<?> message) throws InterruptedException {
        if (message.getData() instanceof WriteRequest writeRequest) {
            byte[] dataBytes = writeRequest.getData();  // Convert the incoming data to byte array
            int totalBlocks = (int) Math.ceil((double) dataBytes.length / Constants.BLOCK_SIZE);

            if (totalBlocks == 0) {
                this.responseQueue.put(
                    new Message<>(ResponseType.SUCCESS, new WriteResponse(writeRequest, dataBytes.length))
                );
                return;
            }
            else if (totalBlocks > 1) {
                String errorMessage = String.format(
                    "Error: while persisting block (node%s, %s): %s%n\n",
                    nodeID,
                    writeRequest.getFileName(),
                    "dataframe exceeded unit block size"
                );
                this.responseQueue.put(
                    new Message<>(
                        ResponseType.FAIL, errorMessage
                    )
                );
                return;
            }

            try {
                String blockFileName = writeRequest.getFileName();
                Path path = Paths.get(storageDir.getPath(), blockFileName);
                this.persistBlock(path, dataBytes);
                this.responseQueue.put(
                    new Message<>(ResponseType.SUCCESS, new WriteResponse(writeRequest, dataBytes.length))
                );
            } catch (IOException e) {
                String errorMessage = String.format(
                    "Error: while persisting block (node%s, %s): %s%n\n",
                    nodeID,
                    writeRequest.getFileName(),
                    e.getMessage()
                );
                this.responseQueue.put(
                    new Message<>(
                        ResponseType.FAIL, errorMessage + Arrays.toString(e.getStackTrace())
                    )
                );
            }
        } else {
            // If the data is not of the expected type, handle it gracefully
            String errorMessage = String.format("Error: unexpected data type: %s", message.getData().getClass().getName());
            this.responseQueue.put(
                new Message<>(
                    ResponseType.FAIL, errorMessage
                )
            );
        }
    }

    private void handleDeleteRequest(Message<?> message) throws InterruptedException {
        if (message.getData() instanceof DeleteRequest deleteRequest) {
            // Get the filename to delete (we will delete all replicas and blocks of this file)
            String filename = deleteRequest.getFilename();

            // Get the path to the storage directory where blocks are saved
            File nodeDir = new File(storageDir.getPath());
            System.out.println(nodeDir.getPath());

            // Find all matching files and delete them
            File[] filesToDelete = nodeDir.listFiles((dir, name) -> Block.pattern.matcher(name).matches());

            Set<Integer> blocksDeleted = new HashSet<>();
            if (filesToDelete != null && filesToDelete.length > 0) {
                // Loop through the files and delete them
                for (File file : filesToDelete) {
                    Block block = new Block(file.getName());
                    boolean deleted = file.delete();
                    if (!deleted) {
                        String errorMessage = String.format("Failed to delete file: %s%n",file.getName());
                        this.responseQueue.put(new Message<>(ResponseType.FAIL, errorMessage));
                        return;
                    }
                    blocksDeleted.add(block.getBlockID());
                }
                // Send success response
                this.responseQueue.put(new Message<>(ResponseType.SUCCESS, new DeleteResponse(deleteRequest, blocksDeleted)));
            } else {
                // No files found to delete
                String errorMessage = String.format("No files found to delete for filename: %s", filename);
                this.responseQueue.put(new Message<>(ResponseType.NOTFOUND, errorMessage));
            }
        } else {
            // If the data is not of the expected type, handle it gracefully
            String errorMessage = String.format("Error: unexpected data type: %s", message.getData().getClass().getName());
            this.responseQueue.put(new Message<>(ResponseType.FAIL, errorMessage));
        }
    }

    /**
     * Helper function for writing data to a block (file).
     * We assume that the file block will always be successfully written (if no IOException is raised, guaranteed by the system)
     * @param path The filepath to write the file to
     * @param data Data to write (an array of bytes)
     * @throws IOException Thrown if a file fails to write
     */
    private void persistBlock(Path path, byte[] data) throws IOException {
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(path.toAbsolutePath().toString()))) {
            // Write the data out to a file
            out.write(data);
        }
    }

    /**
     * Sends an ACK response to the response queue, ACKing the heartbeat request from the main thread.
     */
    private void handleHeartbeat() throws InterruptedException {
        responseQueue.put(new Message<>(ResponseType.ACK, null));
    }

    public void handleExit() {
        System.out.printf("DataNode %s exiting...\n", this.nodeID);
        Thread.currentThread().interrupt();
    }

    static public String getBlockName(int replica, String filename, int block) {
        return String.format("replica%s_%s_block%s", replica, filename, block);
    }
}
