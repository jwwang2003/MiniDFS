package com.lab1.distributedfs.Node;

import com.lab1.distributedfs.Const;
import com.lab1.distributedfs.FileSystem.FileNode;
import com.lab1.distributedfs.FileSystem.FileSystemTree;
import com.lab1.distributedfs.Message.Message;
import com.lab1.distributedfs.Message.MessageType;
import com.lab1.distributedfs.Message.MessageAction;
import com.lab1.distributedfs.Message.MessageBroker;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * <h1>NameNode</h1>
 * <ul>
 *     <li>Keep track of a file system tree</li>
 *     <li>Manage the "metadata" of files</li>
 *     <li>Request DataNode heartbeat</li>
 *     <li>Handles find, add & mutate requests from the client (user)</li>
 *     <li>ONLY file metadata is managed</li>
 * </ul>
 * After the client receives the metadata, they directly request the data nodes for the respective chunks
 */
public class NameNode extends Node {
    private FileSystemTree fileSystemTree;                  // The name node manages a file system tree
    private final Map<Integer, DataNodeStatus> dataNodeStatus;

    // For handling heartbeat
    private final ScheduledExecutorService scheduledExecutorService;

    public NameNode(MessageBroker messageBroker) {
        // The node ID for the name node is 0 by default (there is always only one)
        super(Const.NAME_NODE_ID, messageBroker);

        this.fileSystemTree = new FileSystemTree();
        this.dataNodeStatus = new ConcurrentHashMap<>();

        // Check for persistence
        String persistFilePath = Const.getPath(Const.FS_IMAGE_FILE);
        File persistFile = new File(persistFilePath);
        if (persistFile.exists()) {
            try {
                this.fileSystemTree = FileSystemTree.loadFromFile(persistFilePath);
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Failed to load the file system from the persistent file: " + e);
            }
        }

        this.scheduledExecutorService = Executors.newScheduledThreadPool(2);
        this.scheduleHeartbeatRequest();
        this.scheduleDataNodeStatusCheck();
    }

    @Override
    public void run() {
        // Subscribe to the message broker and start handling messages
        this.messageBroker.subscribe(this.nodeID, message -> {
            // Handle message
            if (message != null) {
                try {
                    this.handleMessage(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private void handleMessage(Message<?> message) throws InterruptedException {
        switch (message.getMessageType()) {
            case MessageType.Request -> {
                // Client/user requesting something from the name node
                switch (message.getMessageAction()) {
                    case LSFS -> handleLSFS(message);
                    case FIND -> handleFind(message);
                    case ADD -> handleAdd(message);
                    case DELETE -> handleDelete(message);
                    case STAT -> handleStat(message);
                    case EXIT -> handleExit();
                }
            }
            case MessageType.Response -> {
                // Name node handling a response from a data or client node
                switch (message.getMessageAction()) {
                    case HEARTBEAT -> handleHeartbeatACK(message);
                    case STAT -> handleStatResponse(message);
                }
            }
            default ->
                // This should not be possible (something must have gone terribly wrong).
                throw new RuntimeException("Invalid message type: " + message.getMessageType());
        }
    }

    // [Requests]

    private void handleLSFS(Message<?> message) {
        reply(message, MessageAction.LSFS, this.fileSystemTree.displayFileSystem());
    }

    private void handleFind(Message<?> message) {
        // part of the "open" process
        if (message.getData() instanceof String path) {
            try {
                FileNode fileNode = this.fileSystemTree.getFile(path);
                reply(message, MessageAction.FILE, fileNode);
            } catch (IllegalArgumentException e) {
                String errorMessage = String.format("file query \"%s\" failed with %s", path, e.getMessage());
                reply(message, MessageAction.FAIL, errorMessage);
            }
            return;
        }
        throw new InvalidParameterException("Invalid command parameters (or data).");
    }

    private void handleAdd(Message<?> message) {
        if (message.getData() instanceof FileNode fileNode) {
            this.fileSystemTree.touch(fileNode);
            reply(message, MessageAction.ADD, fileNode);
            return;
        }
        throw new InvalidParameterException("Invalid command parameters (or data).");
    }

    private void handleDelete(Message<?> message) {
        if (message.getData() instanceof String path) {
            try {
                FileNode fileNode = this.fileSystemTree.rm(path);
                reply(message, MessageAction.DELETE, fileNode);
            } catch (IllegalArgumentException e) {
                String errorMessage = String.format("deletion \"%s\" failed with %s", path, e.getMessage());
                reply(message, MessageAction.FAIL, errorMessage);
            }
        }
    }

    private void handleStat(Message<?> message) {
        this.messageBroker.sendToSubscriber(
            message.getSrcNodeID(), responseMessage(MessageAction.STAT, this.dataNodeStatus)
        );
    }

    private void handleExit() {
        System.out.printf("NameNode %s exiting...\n", this.nodeID);
        this.scheduledExecutorService.shutdown();
        try {
            assert this.scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS);
            assert this.scheduledExecutorService.isTerminated();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        this.persist();
        Thread.currentThread().interrupt();
    }

    // Other

    private void handleHeartbeatACK(Message<?> message) {
        long currentTime = System.nanoTime();
        int senderID = message.getSrcNodeID();

        if (this.dataNodeStatus.containsKey(senderID)) {
            DataNodeStatus datanodeStatus = this.dataNodeStatus.get(senderID);
            datanodeStatus.lastSeen = currentTime;
            this.dataNodeStatus.replace(senderID, datanodeStatus);
        } else {
            this.dataNodeStatus.put(senderID, new DataNodeStatus(message.getSrcNodeID(), currentTime));
        }
    }

    private void handleStatResponse(Message<?> message) {
        int senderID = message.getSrcNodeID();
        DataNodeStatus datanodeStatus = (DataNodeStatus) message.getData();
        this.dataNodeStatus.put(senderID, datanodeStatus);
    }

    // Other methods

    private void persist() {
        try {
            this.fileSystemTree.saveToFile(Const.getPath(Const.FS_IMAGE_FILE));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    // Periodic tasks

    private void scheduleHeartbeatRequest() {
        // Periodically broadcast heartbeat request signals
        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            // Send heartbeat request to each DataNode's request queue
            broadcast(MessageAction.HEARTBEAT, null);
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    private void scheduleDataNodeStatusCheck() {
        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            for (DataNodeStatus status : dataNodeStatus.values()) {
                // Alive if lastSeen was within timeout
                status.alive = (now - status.lastSeen) <= Const.WORKER_TIMEOUT;
                send(status.nodeId, MessageAction.STAT, status);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }
}
