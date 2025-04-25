package com.lab1.distributedfs.Node;

import com.lab1.distributedfs.FileSystem.FileNode;
import com.lab1.distributedfs.Helper;
import com.lab1.distributedfs.IO.Client.Open;
import com.lab1.distributedfs.Const;
import com.lab1.distributedfs.IO.DataNodeIO.Block;
import com.lab1.distributedfs.Message.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * <h1>Client Node</h1>
 * <ul>
 *     <li>Spins up other worker threads (name node, data node)</li>
 *     <li>Basic OP features: open, write, read, close</li>
 * </ul>
 *
 * <h2>Description</h2>
 * The client (main thread) manages all the worker threads.
 * It handles the logic & mechanisms for reading/writing files in blocks,
 * block assignment algorithms, ensuring that a block is successfully written,
 * handling a write failure (try electing another data node to write to), etc.
 */
public class Client implements Runnable {
    private final BlockingQueue<Message<?>> requestQueue;
    private final BlockingQueue<Message<?>> responseQueue;

    // Threading
    private final ExecutorService executorService;
    private final ConcurrentHashMap<Integer, Node> nodeMap;
    // Communication
    public final MessageBroker messageBroker;
    // Thread-safe hashmap to track "opened files" and also implement a locking mechanism
    private final ConcurrentHashMap<String, Open> busyPaths;
    private final List<String> recentBusyPath;

    private int nodeID;

    public Client(BlockingQueue<Message<?>> requestQueue, BlockingQueue<Message<?>> responseQueue) {
        // Bidirectional communication with the main thread
        this.requestQueue = requestQueue;
        this.responseQueue = responseQueue;
        // Initialize the thread pool
        this.executorService = Executors.newFixedThreadPool(Const.NUM_NODES - 1);
        this.nodeMap = new ConcurrentHashMap<>();
        // Communication
        this.messageBroker = new MessageBroker(Const.NUM_NODES);
        this.busyPaths = new ConcurrentHashMap<>();
        this.recentBusyPath = new ArrayList<>();
    }

    @Override
    public void run() {
        // Initialize the NameNode worker
        NameNode nameNode = new NameNode(messageBroker);
        nodeMap.put(nameNode.getNodeID(), nameNode);
        executorService.execute(nameNode);

        // Initialize the DataNode workers
        for (int i = 1; i < Const.NUM_DATA_NODES + 1; i++) {
            DataNode dataNode;
            try {
                dataNode = new DataNode(i, this.messageBroker);
            } catch (Exception e) {
                System.err.println("FATAL error while initializing data node, node's data directory might be damaged or corrupt?");
                System.err.println(e.getMessage());
                throw new RuntimeException(e);
            }
            nodeMap.put(i, dataNode);
            executorService.execute(dataNode);
        }

        this.nodeID = Const.CLIENT_NODE_ID;

        // Subscribe to the message broker and begin handling requests
        // Communication between threads (NameNode, DataNodes, & Client)
        this.messageBroker.subscribe(this.nodeID, message -> {
            // Handle message
            if (message != null && message.getMessageType() == MessageType.Response) {
                try {
                    this.handleResponse(message);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // Blocking communication between the Client and the main thread
        while(true) {
            // Handling messages from the main thread (async communication with shell)
            try {
                Message<?> message = requestQueue.take();
                this.handleCommand(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void handleResponse(Message<?> response) throws InterruptedException {
        // Handle the responses from the message broker (response)
        switch (response.getMessageType()) {
            case Request -> System.out.println("Error: unimplemented action");
            case Response -> {
                switch (response.getResponseType()) {
                    case WRITE, READ, LSFS, FOUND, STAT, NOTFOUND, ADD, DELETE, FAIL -> responseQueue.put(response);
                }
            }
        }
    }

    public void handleCommand(Message<?> message) throws InterruptedException {
        // Handles commands from the main thread (request)
        switch (message.getMessageType()) {
            case Request -> {
                // Remember to override the source node ID
                message.setSrcNodeID(this.nodeID);
                switch (message.getRequestType()) {
                    case LSFS, ADD, STAT ->
                        // Simply forward the FIND message to the name node
                        this.messageBroker.sendToSubscriber(
                            Const.NAME_NODE_ID, message
                        );
                    case FIND -> {
                        // If the path for find operation is empty then use the most recent opened file (if there is)
                        String path = (String) message.getData();
                        path = (path.isEmpty() && (this.recentBusyPath != null)) ? this.recentBusyPath.getLast() : path;
                        if (path.isEmpty()) {
                            responseQueue.put(new Message<>(ResponseType.FAIL, "path is empty"));
                            return;
                        }
                        if (busyPaths.containsKey(path)) {
                            responseQueue.put(new Message<>(ResponseType.FOUND, busyPaths.get(path).fileNode));
                        } else { this.messageBroker.sendToSubscriber(Const.NAME_NODE_ID, message); }
                    }
                    case DELETE -> {
                        // Ensure the file to delete is not busy.
                        if (busyPaths.containsKey((String) message.getData())) {
                            responseQueue.put(new Message<>(ResponseType.FAIL, "file is busy"));
                        } else { this.messageBroker.sendToSubscriber(Const.NAME_NODE_ID, message); }
                    }
                    // Handle open & close commands
                    case OPEN -> this.handleOpenFile(message);
                    case CLOSE -> this.handleCloseFile(message);
                    // Data Node commands should be passed directly
                    case WRITE, READ -> {
                        if (message.getData() instanceof Block blk) {
                            this.messageBroker.sendToSubscriber(
                                blk.getNodeID(), message
                            );
                        }
                    }
                    // Termination
                    case EXIT -> this.shutdown();
                }
            }
            case Response -> {
                switch (message.getResponseType()) {

                }
            }
        }
    }

    private void handleOpenFile(Message<?> message) throws InterruptedException {
        if (message.getData() instanceof Open open) {
            if (Helper.getPathParts(open.path).length == 0) {
                if (this.recentBusyPath.isEmpty()) {
                    responseQueue.put(new Message<>(this.nodeID, ResponseType.FAIL, "no file opened"));
                    return;
                }
                open = busyPaths.get(this.recentBusyPath.getLast());
            } else if (!busyPaths.containsKey(open.path)) {
                busyPaths.put(open.path, open);
                recentBusyPath.add(open.path);
            } else {
               open = busyPaths.get(open.path);
            }

            responseQueue.put(new Message<>(this.nodeID, ResponseType.OPEN, open));
        }
        else responseQueue.put(new Message<>(this.nodeID, ResponseType.FAIL, "failed to open file"));
    }

    private void handleCloseFile(Message<?> message) throws InterruptedException {
        String path = (String) message.getData();

        if (busyPaths.isEmpty()) {
            responseQueue.put(new Message<>(this.nodeID, ResponseType.FAIL, "no files opened"));
            return;
        }
        try {
            if (Helper.getPathParts(path).length == 0) {
                path = recentBusyPath.getLast();
            }
            Open open = busyPaths.remove(path);
            if (open == null) { throw new NullPointerException(); }
            if (path.equals(recentBusyPath.getLast())) {
                recentBusyPath.remove(path);
            }

            FileNode fileNode = open.fileNode;
            if (fileNode == null) {
                responseQueue.put(new Message<>(this.nodeID, ResponseType.FAIL, "fileNode is null for file"));
                return;
            }
            responseQueue.put(new Message<>(this.nodeID, ResponseType.CLOSE, open));
            return;
        } catch (NullPointerException ignored) {}
        responseQueue.put(new Message<>(this.nodeID, ResponseType.FAIL, "file not found"));
    }

    /**
     * Shutdown method to gracefully shutdown NameNode and DataNode threads
     * Shutdown tasks:
     * - Stop all the DataNode workers
     * - Stop executor service
     * - Stop scheduled executor service
     */
    public void shutdown() {
        // Interrupt all worker threads to stop them
        messageBroker.broadcast(new Message<>(this.nodeID, RequestType.EXIT, null));
        // Shutdown all threads
        executorService.shutdown();
        try {
            assert executorService.awaitTermination(5, TimeUnit.SECONDS);
            assert executorService.isTerminated();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        messageBroker.shutdown();
        Thread.currentThread().interrupt();
    }
}
