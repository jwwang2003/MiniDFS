package com.lab1.distributedfs.Node;

import com.lab1.distributedfs.Const;
import com.lab1.distributedfs.IO.DataNodeIO.*;
import com.lab1.distributedfs.Message.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <h1>DataNode</h1>
 *
 * <p>
 *  The DataNode manages the storage for multiple blocks, each bloch has its own fixed size.
 *  Simulating the concept of a "distributed file system", but in this case, the virtual file system
 *  we are implementing is not distributed between multiple disks/servers, but just between different files.
 * </p>
 *
 * <h2>Features:</h2>
 * <ul>
 *     <li>Thread-safe (i think?)/li>
 *     <li>Write, read, delete (part of write)</li>
 *     <li>Heartbeat</li>
 *     <li>Exit (stop the current thread)</li>
 * </ul>
 */
public class DataNode extends Node {
    private final File storageDir;
    private final AtomicInteger blockCount  = new AtomicInteger(0);
    private final AtomicLong storageUsed = new AtomicLong(0);

    public DataNode(int nodeID, MessageBroker messageBroker) throws Exception {
        super(nodeID, messageBroker);

        // Create directory to store blocks if it doesn't exist
        // node#/...
        String nodeDir = String.format("node%s", nodeID);
        String path = Paths.get(Const.getPath(Const.DATANODE_ROOT_DIR), nodeDir).toString();
        this.storageDir = new File(path);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        // Read data node blocks (if they exist)
        // Calculate block count & data node size
        File[] existingFiles = storageDir.listFiles((dir, name) -> Block.pattern.matcher(name).matches());
        if (existingFiles != null) {
            for (File f : existingFiles) {
                if (!f.isFile()) { continue; }
                // reconstruct your Block object from its filename
                Block block = new Block(this.nodeID, f.getName());
                List<Block> blocks = Collections.synchronizedList(new ArrayList<>());
                blocks.add(block);
                blockCount.incrementAndGet();
                storageUsed.addAndGet(f.length());
            }
        } else {
            blockCount.set(0);
            storageUsed.set(0);
        }
    }

    @Override
    public void run() {
        this.messageBroker.subscribe(this.nodeID, message -> {
            // Handle message
            if (message != null) {
                this.handleMessage(message);
            }
        });
    }

    // ========================================== INTERNAL FUNCTIONS ===================================================
    private void handleMessage(Message<?> message) {
        if (message.getMessageType() != MessageType.Request) {
            // DataNoes are "worker threads", they only accept request messages, they do not send any requests
            // Therefore, if the message is NOT a request, then something must have gone horribly wrong.
            // throw new RuntimeException("Invalid message type: " + message.getMessageType());
            return;
        }

        // Determine the request type
        switch (message.getMessageAction()) {
            case READ -> handleReadRequest(message);
            case WRITE -> handleWriteRequest(message);
            case HEARTBEAT -> handleHeartbeat();
            case STAT -> handleStatusRequest(message);
            case EXIT -> handleExit();
        }
    }

    private void handleReadRequest(Message<?> message) {
        if (message.getData() instanceof ReadRequest readRequest) {
            // Construct the path to the requested block file
            String blockFileName = readRequest.getFilename();
            Path path = Paths.get(storageDir.getPath(), blockFileName);

            // Check if the file exists
            File file = path.toFile();
            if (!file.exists()) {
                // If the file doesn't exist, respond with failure
                String errorMessage = String.format("Error: file block not found (node%s, %s)", this.nodeID, blockFileName);
                reply(message, MessageAction.FAIL, errorMessage);
                return;
            }

            try {
                // Read the file data into a byte array
                byte[] fileData = java.nio.file.Files.readAllBytes(path);
                // Send the data back via the response queue
                reply(message, MessageAction.READ, new ReadResponse(readRequest, fileData));
            } catch (IOException e) {
                String errorMessage = String.format(
                    "Error: while reading file block (node%s, %s): %s%n\n",
                    this.nodeID,
                    readRequest.getPathname(),
                    e.getMessage()
                );
                reply(message, MessageAction.FAIL, errorMessage + Arrays.toString(e.getStackTrace()));
            }
        } else {
            // If the data is not of the expected type, handle it gracefully
            String errorMessage = String.format("Error: unexpected data type: %s", message.getData().getClass().getName());
            reply(message, MessageAction.FAIL, errorMessage);
        }
    }

    private void handleWriteRequest(Message<?> message) {
        if (!(message.getData() instanceof WriteRequest writeRequest)) {
            String errorMessage = String.format( "Error: unexpected data type: %s", message.getData().getClass().getName());
            reply(message, MessageAction.FAIL, errorMessage);
            return;
        }

        byte[] dataBytes = writeRequest.getData();
        String blockFileName = writeRequest.getFilename();
        Path path = Paths.get(storageDir.getPath(), blockFileName);
        File blockFile = path.toFile();
        int size = (int) blockFile.length();

        // If no data, delete the block file instead of writing
        if (dataBytes == null || dataBytes.length == 0) {
            if (blockFile.exists()) {
                if (!blockFile.delete()) {
                    String err = String.format("Error: failed to delete block (node%s, %s)", this.nodeID, blockFileName);
                    reply(message, MessageAction.FAIL, err);
                    return;
                }
            }
            // Acknowledge "write" of 0 bytes (i.e. deletion)
            this.blockCount.addAndGet(-1);
            this.storageUsed.addAndGet(-size);
            reply(message, MessageAction.WRITE, new WriteResponse(writeRequest, (int) blockFile.length()));
            return;
        }

        // Otherwise, normal single‐block write logic
        int totalBlocks = (int) Math.ceil((double) (dataBytes.length + size) / Const.BLOCK_SIZE);
        if (totalBlocks > 1) {
            String err = String.format(
                "Error: while persisting block (node%s, %s): data exceeds block size",
                this.nodeID, blockFileName
            );
            reply(message, MessageAction.FAIL, err);
            return;
        }

        try {
            // write (or overwrite) the block file
            this.persistBlock(path, dataBytes);
            reply(message, MessageAction.WRITE, new WriteResponse(writeRequest, totalBlocks));
            if (!writeRequest.isAppendBlock()) {
                this.blockCount.addAndGet(1);
            }
            this.storageUsed.addAndGet(dataBytes.length);
        } catch (IOException e) {
            String err = String.format("Error: while persisting block (node%s, %s): %s", this.nodeID, blockFileName, e.getMessage());
            reply(message, MessageAction.FAIL, err);
        }
    }

    private void handleStatusRequest(Message<?> message) {
        if (!(message.getData() instanceof DataNodeStatus dataNodeStatus)) {
            String errorMessage = String.format( "Error: unexpected data type: %s", message.getData().getClass().getName());
            reply(message, MessageAction.FAIL, errorMessage);
            return;
        }

        dataNodeStatus.blockCount = this.blockCount.get();
        dataNodeStatus.storageUsed = this.storageUsed.get();
        reply(message, MessageAction.STAT, dataNodeStatus);
    }

    /**
     * Helper function for writing data to a block (file).
     * We assume that the file block will always be successfully written (if no IOException is raised, guaranteed by the system)
     * @param path The filepath to write the file to
     * @param data Data to write (an array of bytes)
     * @throws IOException Thrown if a file fails to write
     */
    private void persistBlock(Path path, byte[] data) throws IOException {
        // 1) Make sure the directory tree exists
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        // 2) Open the file via NIO, with CREATE/TRUNCATE semantics
        try (BufferedOutputStream out = new BufferedOutputStream(
            Files.newOutputStream(path,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND))
        ) {
            out.write(data);
            out.flush();
        }
    }

    /**
     * Sends an ACK response to the response queue, ACKing the heartbeat request from the main thread.
     */
    private void handleHeartbeat() {
        broadcastReply(MessageAction.HEARTBEAT, null);
    }

    public void handleExit() {
        System.out.printf("DataNode %s exiting...\n", this.nodeID);
        Thread.currentThread().interrupt();
    }
}
