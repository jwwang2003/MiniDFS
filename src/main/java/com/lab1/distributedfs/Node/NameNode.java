package com.lab1.distributedfs.Node;

import com.lab1.distributedfs.Const;
import com.lab1.distributedfs.FileSystem.FileNode;
import com.lab1.distributedfs.FileSystem.FileSystemTree;
import com.lab1.distributedfs.Message.Message;
import com.lab1.distributedfs.Message.MessageType;
import com.lab1.distributedfs.Message.RequestType;
import com.lab1.distributedfs.Message.ResponseType;
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

    public NameNode(ConcurrentHashMap<Integer, Node> dataNodes, MessageBroker messageBroker) {
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

    public int getNodeID() {
        return nodeID;
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
                switch (message.getRequestType()) {
                    case RequestType.LSFS -> handleLSFS(message);
                    case RequestType.FIND -> handleFind(message);
                    case RequestType.ADD -> handleAdd(message);
                    case RequestType.DELETE -> handleDelete(message);
                    case RequestType.STAT -> handleStat(message);
                    case RequestType.EXIT -> handleExit();
                }
            }
            case MessageType.Response -> {
                // Name node handling a response from a data or client node
                switch (message.getResponseType()) {
                    case ResponseType.ACK -> handleHeartbeatACK(message);
                    case ResponseType.STAT -> handleStatResponse(message);
                }
            }
            default ->
                // This should not be possible (something must have gone terribly wrong).
                throw new RuntimeException("Invalid message type: " + message.getMessageType());
        }
    }

    // [Requests]

    private void handleLSFS(Message<?> message) throws InterruptedException {
        this.messageBroker.sendToSubscriber(
            message.getSrcNodeID(), new Message<>(this.getNodeID(), ResponseType.LSFS, this.fileSystemTree.displayFileSystem())
        );
    }

    private void handleFind(Message<?> message) {
        // part of the "open" process
        if (message.getData() instanceof String path) {
            try {
                FileNode fileNode = this.fileSystemTree.getFile(path);
                this.messageBroker.sendToSubscriber(
                    message.getSrcNodeID(), new Message<>(this.getNodeID(), ResponseType.FOUND, fileNode)
                );
            } catch (IllegalArgumentException e) {
                String errorMessage = String.format("File query \"%s\" failed with %s", path, e.getMessage());
                this.messageBroker.sendToSubscriber(
                    message.getSrcNodeID(), new Message<>(this.getNodeID(), ResponseType.NOTFOUND, errorMessage)
                );
            }
            return;
        }
        throw new InvalidParameterException("Invalid command parameters (or data).");
    }

    private void handleAdd(Message<?> message) {
        if (message.getData() instanceof FileNode fileNode) {
            this.fileSystemTree.touch(fileNode);
            this.messageBroker.sendToSubscriber(
                message.getSrcNodeID(), new Message<>(this.getNodeID(), ResponseType.ADD, fileNode)
            );
            return;
        }
        throw new InvalidParameterException("Invalid command parameters (or data).");
    }

    private void handleDelete(Message<?> message) {
        if (message.getData() instanceof String path) {
            try {
                FileNode fileNode = this.fileSystemTree.rm(path);
                this.messageBroker.sendToSubscriber(
                    message.getSrcNodeID(), new Message<>(this.getNodeID(), ResponseType.DELETE, fileNode)
                );
            } catch (IllegalArgumentException e) {
                String errorMessage = String.format("file query \"%s\" failed with %s", path, e.getMessage());
                this.messageBroker.sendToSubscriber(
                    message.getSrcNodeID(), new Message<>(this.getNodeID(), ResponseType.NOTFOUND, errorMessage)
                );
            }
        }
    }

    private void handleStat(Message<?> message) {
        this.messageBroker.sendToSubscriber(
            message.getSrcNodeID(), new Message<>(this.getNodeID(), ResponseType.STAT, this.dataNodeStatus)
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
        if (!this.persist()) {
            System.out.printf("Error: while terminating NameNode %s", this.nodeID);
        }
        Thread.currentThread().interrupt();
    }

    // [Responses]

    private void handleHeartbeatACK(Message<?> message) {
        long currentTime = System.nanoTime();
        int senderID = message.getSrcNodeID();

        if (this.dataNodeStatus.containsKey(senderID)) {
            DataNodeStatus datanodeStatus = this.dataNodeStatus.get(senderID);
            long diff = currentTime - datanodeStatus.lastSeen;
            // Update the diff value
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

    private boolean persist() {
        try {
            this.fileSystemTree.saveToFile(Const.getPath(Const.FS_IMAGE_FILE));
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    // Periodic tasks

    private void scheduleHeartbeatRequest() {
        // Periodically broadcast heartbeat request signals
        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            // Send heartbeat request to each DataNode's request queue
            Message<?> heartbeatRequest = new Message<>(this.nodeID, RequestType.HEARTBEAT, null);
            this.messageBroker.broadcast(heartbeatRequest);
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    private void scheduleDataNodeStatusCheck() {
        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            for (DataNodeStatus status : dataNodeStatus.values()) {
                // Alive if lastSeen was within timeout
                status.alive = (now - status.lastSeen) <= Const.WORKER_TIMEOUT;
                Message<?> statusRequest = new Message<>(this.nodeID, RequestType.STAT, status);
                this.messageBroker.sendToSubscriber(status.nodeId, statusRequest);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

}
