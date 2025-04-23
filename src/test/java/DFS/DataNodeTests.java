package DFS;

import com.lab1.distributedfs.BlockIOOP.DeleteRequest;
import com.lab1.distributedfs.BlockIOOP.DeleteResponse;
import com.lab1.distributedfs.BlockIOOP.WriteResponse;
import com.lab1.distributedfs.Message.Message;
import com.lab1.distributedfs.Message.RequestType;
import com.lab1.distributedfs.BlockIOOP.WriteRequest;
import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;

import com.lab1.distributedfs.Node.DataNode;

import java.io.File;
import java.util.Random;
import java.util.concurrent.*;

public class DataNodeTests {
    private BlockingQueue<Message<?>> requestQueue;
    private BlockingQueue<Message<?>> responseQueue;
    private ExecutorService executorService;
    private DataNode dataNode;
    private File mockStorageDir;
    private final String TEST_FILENAME = "testFile";
    private final int TEST_BLOCK_ID = 1;

    @BeforeEach
    public void setUp() {
        requestQueue = new LinkedBlockingQueue<>();
        responseQueue = new LinkedBlockingQueue<>();

        // Mocking file directory and file operations
        mockStorageDir = mock(File.class);

        dataNode = new DataNode(requestQueue, responseQueue, 0);
        executorService = Executors.newFixedThreadPool(1);
        new Thread(dataNode).start();
    }

    @Test
    public void testGetBlockName() {
        String blockName = DataNode.getBlockName(1, "test", 2);
        Assertions.assertEquals("replica1_test_block2.txt", blockName);
    }

    @Test
    public void testHandleWriteRequest_singleBlock() throws InterruptedException {
        // Given
        byte[] data = new byte[128]; // Set the size to 128 bytes

        // Fill the byte array with random bytes
        Random random = new Random();
        random.nextBytes(data);

//        System.out.println(Arrays.toString(data));
//        System.out.println(data.length);

        // When
        requestQueue.put(new Message<>(RequestType.WRITE, new WriteRequest(0, TEST_FILENAME, TEST_BLOCK_ID, data)));

        // Then
        Message<?> response = responseQueue.take();
        if (response.getData() instanceof WriteResponse writeResponse) {
            System.out.println(writeResponse.getNumBytesWritten());
        }

        // When
        requestQueue.put(new Message<>(RequestType.DELETE, new DeleteRequest(0, TEST_FILENAME)));

        response = responseQueue.take();
        if (response.getData() instanceof DeleteResponse deleteResponse) {
            System.out.println(deleteResponse.getDeletedBlocks());
        }

//        assertTrue(result);
//        verify(responseQueue).put(any(Message.class));  // Verifying that a response was added to the queue
    }
}
