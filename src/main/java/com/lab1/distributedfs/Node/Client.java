package com.lab1.distributedfs.Node;

import com.lab1.distributedfs.BlockIOOP.IOMode;
import com.lab1.distributedfs.Constants;
import com.lab1.distributedfs.Message.Message;
import com.lab1.distributedfs.Message.RequestType;
import com.lab1.distributedfs.Message.ResponseType;
import org.mockito.internal.matchers.Any;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * The client (main thread) manages all the worker threads, it is also responsible for sending heartbeat requests.
 * There should be logic & mechanisms for reading/writing files in blocks, make block assignment algorithms, ensuring that
 * a block is successfully written, handling a write failure (try electing another data node to write to), etc.
 */
public class Client {
    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutorService; // For scheduling periodic tasks
    private List<DataNode> threadedDataNodes;
    private Map<Integer, DataNode> dataNodeMap;
    private Map<DataNode, BlockingQueue<Message<?>>> dataNodeRequestQueueMap;
    private Map<DataNode, BlockingQueue<Message<?>>> dataNodeResponseQueueMap;

    public Client() {
        // Initialize the thread worker pool
        int numDataNodes = Constants.NUM_DATANODES;
        this.executorService = Executors.newFixedThreadPool(numDataNodes);
        // Initialize the scheduled executor service for heartbeat
        this.scheduledExecutorService = Executors.newScheduledThreadPool(1);

        this.threadedDataNodes = new ArrayList<>();
        this.dataNodeMap = new ConcurrentHashMap<>();
        this.dataNodeRequestQueueMap = new HashMap<>();
        this.dataNodeResponseQueueMap = new HashMap<>();

        for (int i = 0; i < numDataNodes; i++) {
            // Initialize all the DataNodes
            BlockingQueue<Message<?>> reqQueue = new LinkedBlockingQueue<Message<?>>();
            BlockingQueue<Message<?>> resQueue = new LinkedBlockingQueue<Message<?>>();
            DataNode dataNode = new DataNode(reqQueue, resQueue, i);
            dataNodeMap.put(i, dataNode);
            dataNodeRequestQueueMap.put(dataNode, reqQueue);
            dataNodeResponseQueueMap.put(dataNode, resQueue);
            new Thread(dataNode).start();
        }

        pollAndHandleResponses();
        scheduleHeartbeatRequest();
    }

    // Poll the response queues of all DataNodes and handle the responses
    public void pollAndHandleResponses() {
        // A separate thread can be used for polling the responses
        executorService.execute(() -> {
            try {
                while (true) {
                    // Iterate over each DataNode and poll its response queue
                    for (DataNode dataNode : dataNodeMap.values()) {
                        BlockingQueue<Message<?>> responseQueue = dataNodeResponseQueueMap.get(dataNode);
                        // Pulling every other 5 milliseconds
                        Message<?> response = responseQueue.poll(5, TimeUnit.MILLISECONDS); // Poll

                        if (response != null) {
                            // Handle the response based on the type of message
                            handleResponse(dataNode, response);
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
//                System.err.println("Polling thread interrupted: " + e.getMessage());
            }
        });
    }

    private void handleOpenFile(String filename, IOMode mode) {

    }

    // Method to send a heartbeat request to each DataNode every second
    private void scheduleHeartbeatRequest() {
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                // Send heartbeat request to each DataNode's request queue
                Message<?> heartbeatRequest = new Message<>(RequestType.HEARTBEAT, null);
                for (DataNode dataNode : dataNodeMap.values()) {
                    BlockingQueue<Message<?>> requestQueue = dataNodeRequestQueueMap.get(dataNode);
                    requestQueue.put(heartbeatRequest);  // Send heartbeat request
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Heartbeat request interrupted: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);  // Initial delay 0, repeat every 1 second
    }

    // Handle the response based on the type of message received
    private void handleResponse(DataNode dataNode, Message<?> response) {
        // Process the response message
        if (response.getResponseType() == ResponseType.SUCCESS) {
            // Handle success case
            System.out.println("DataNode " + dataNode.getNodeID() + " responded with success: " + response.getData());
        } else if (response.getResponseType() == ResponseType.FAIL) {
            // Handle failure case
            System.out.println("DataNode " + dataNode.getNodeID() + " responded with failure: " + response.getData());
        } else if (response.getResponseType() == ResponseType.NOTFOUND) {
            // Handle not found case (e.g., file not found)
            System.out.println("DataNode " + dataNode.getNodeID() + " responded with NOTFOUND: " + response.getData());
        } else if (response.getResponseType() == ResponseType.ACK) {
            // Handle acknowledgment (e.g., heartbeat)
//            System.out.println("DataNode " + dataNode.getNodeID() + " responded with ACK: " + response.getData());
        } else {
            // Handle any other types of responses
            System.out.println("DataNode " + dataNode.getNodeID() + " responded with unknown response: " + response.getData());
        }
    }

    /**
     * Shutdown method to gracefully shutdown NameNode and DataNode threads
     * Shutdown tasks:
     * - Stop all the DataNode workers
     * - Stop executor service
     * - Stop scheduled executor service
     */
    public void shutdown() {
        // Interrupt all DataNode threads to stop them
        try {
            for (BlockingQueue<Message<?>> queue:this.dataNodeRequestQueueMap.values()) {
                queue.put(new Message<>(RequestType.EXIT, null));
                // Interrupt the running DataNode thread
            }
        } catch (InterruptedException ignored) { }

        System.out.println("Shutting down executor & scheduled executor service...");
        executorService.shutdownNow();              // Shutdown all threads
        scheduledExecutorService.shutdownNow();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Executor service did not terminate in time");
                }
            }
            if (!scheduledExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduledExecutorService.shutdownNow();
                if (!scheduledExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Scheduled executor service did not terminate in time");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            scheduledExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
