package com.lab1.distributedfs.Node;

import com.lab1.distributedfs.FileSystem.FileNode;
import com.lab1.distributedfs.FileSystem.FileSystemTree;
import com.lab1.distributedfs.IO.Client.Open;
import com.lab1.distributedfs.Const;
import com.lab1.distributedfs.Message.*;

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
    public MessageBroker messageBroker;
    // Thread-safe hashmap to track "opened files" and also implement a locking mechanism
    private final ConcurrentHashMap<String, Open> busyPaths;
    private String recentBusyPath;

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
    }

    @Override
    public void run() {
        // Initialize the NameNode worker
        NameNode nameNode = new NameNode(nodeMap, messageBroker);
        nodeMap.put(nameNode.getNodeID(), nameNode);
        executorService.execute(nameNode);

        // Initialize the DataNode workers
        for (int i = 1; i < Const.NUM_DATA_NODES + 1; i++) {
            DataNode dataNode = new DataNode(i, this.messageBroker);
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
                    case LSFS, FOUND, NOTFOUND, ADD, DELETE -> responseQueue.put(response);
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
                    case LSFS, FIND, ADD ->
                        // Simply forward the FIND message to the name node
                        this.messageBroker.sendToSubscriber(
                            Const.NAME_NODE_ID, message
                        );
                    case DELETE -> {
                        if (busyPaths.containsKey((String) message.getData())) {
                            responseQueue.put(new Message<>(ResponseType.FAIL, "File is busy"));
                        } else {
                            this.messageBroker.sendToSubscriber(
                                    Const.NAME_NODE_ID, message
                            );
                        }
                    }
                    // Handle open & close commands
                    case OPEN -> this.handleOpenFile(message);
                    case CLOSE -> this.handleCloseFile(message);
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

    public void handleOpenFile(Message<?> message) throws InterruptedException {
        if (message.getData() instanceof Open open && !busyPaths.containsKey(open.path)) {
            busyPaths.put(open.path, open);
            recentBusyPath = open.path;
            responseQueue.put(new Message<>(this.nodeID, ResponseType.OPEN, open));
        }
        else responseQueue.put(new Message<>(this.nodeID, ResponseType.FAIL, "File is already opened"));
    }

    private void handleCloseFile(Message<?> message) throws InterruptedException {
        String path = (String) message.getData();

        if (busyPaths.isEmpty()) {
            responseQueue.put(new Message<>(this.nodeID, ResponseType.FAIL, "No files opened"));
            return;
        }
        try {
            if (FileSystemTree.getPathParts(path).length == 0) {
                path = recentBusyPath;
                recentBusyPath = null;
            }
            Open open = busyPaths.remove(path);
            if (open == null) { throw new NullPointerException(); }
            recentBusyPath = path.equals(recentBusyPath) ? null : recentBusyPath;

            // TODO: Handle putting the FileNode to the FS tree (either inserting or updating)
            FileNode fileNode = open.fileNode;
            if (fileNode == null) {
                responseQueue.put(new Message<>(this.nodeID, ResponseType.FAIL, "FileNode is null for file"));
                return;
            }

            responseQueue.put(new Message<>(this.nodeID, ResponseType.CLOSE, open));
            return;
        } catch (NullPointerException ignored) {}

        responseQueue.put(new Message<>(this.nodeID, ResponseType.FAIL, "File not found"));
    }

    public void handleWriteFile(String pathname, byte[] data) {
        if (!this.busyPaths.containsKey(pathname)) {
            System.out.println("File is not open.");
        }
    }

    public byte[] handleReadFile(String pathname) {
        if (!this.busyPaths.containsKey(pathname)) {
            System.out.println("File is not open.");
            return null;
        }
        return new byte[0];
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
