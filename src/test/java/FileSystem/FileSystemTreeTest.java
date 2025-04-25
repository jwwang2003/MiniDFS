package FileSystem;

import com.lab1.distributedfs.FileSystem.BlockNode;
import com.lab1.distributedfs.FileSystem.DirectoryNode;
import com.lab1.distributedfs.FileSystem.FileNode;
import com.lab1.distributedfs.FileSystem.FileSystemTree;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileSystemTreeTest {

    private FileSystemTree fileSystemTree;
    private static final String TEST_FILE_NAME = "./fsimage.ser";

    @BeforeEach
    public void setUp() {
        // Set up a new FileSystemTree before each test
        fileSystemTree = new FileSystemTree();

        // Add some random files to the tree
        List<BlockNode> blockList = new ArrayList<>();
        blockList.add(new BlockNode(1, "", 128, new ArrayList<>()));

        // Adding files
        fileSystemTree.touch("/file1.txt", blockList, true);
        fileSystemTree.touch("file2.txt", blockList, true);
        fileSystemTree.touch("/tmp/file3.txt", blockList, true);

        FileNode file1 = new FileNode("/tmp/file3.txt", new ArrayList<>());
    }

    @Test
    public void testAddFile_validPath() {
        DirectoryNode rootDir = fileSystemTree.getRoot();
        assertEquals(2, rootDir.getFiles().size(), "File count in root directory should be 2.");
        assertEquals("file1.txt", rootDir.getFiles().getFirst().getFilename(), "File name should be 'file1.txt'.");
    }

    @Test
    public void testAddFile_invalidDirectory() {
        List<BlockNode> blockList = new ArrayList<>();
        blockList.add(new BlockNode(1, "", 128, new ArrayList<>()));

        // Test: Adding a file to a non-existing directory should throw an exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                fileSystemTree.touch("nonexistentDir/file2.txt", blockList, false));
        assertEquals("Directory nonexistentDir not found", exception.getMessage());
    }

    @Test
    public void testDeleteFile_validPath() {
        // Delete file 'file2.txt' located in 'root/sub_dir1'
        System.out.println(fileSystemTree.displayFileSystem());
        fileSystemTree.rm("/tmp/file3.txt");
        System.out.println(fileSystemTree.displayFileSystem());
    }

    @Test
    public void testSaveAndLoadFileSystem() throws IOException, ClassNotFoundException {
        // Save to file
        fileSystemTree.saveToFile(TEST_FILE_NAME);
        System.out.println(fileSystemTree.displayFileSystem());

        System.out.println(fileSystemTree.getFile("/tmp/file3.txt"));
        // Load the file system back from the file
        FileSystemTree loadedTree = FileSystemTree.loadFromFile(TEST_FILE_NAME);

        // Assert that the loaded file system matches the original
        DirectoryNode rootDir = loadedTree.getRoot();
        DirectoryNode tmpDir = loadedTree.getRoot().getSubDirectory("tmp");
        assertEquals(2, rootDir.getFiles().size(), "File count in loaded root directory should be 2.");
        assertEquals(1, tmpDir.getFiles().size(), "File count in loaded root directory should be 1.");
        assertEquals("file1.txt", rootDir.getFiles().getFirst().getFilename(), "File name in loaded file system should be 'file1.txt'.");
        assertEquals("file3.txt", tmpDir.getFiles().getFirst().getFilename(), "File name in loaded file system should be 'file1.txt'.");
    }

    @Test
    public void testSerializationDeserialization() throws IOException, ClassNotFoundException {
        // Serializing and then deserializing the file system
        fileSystemTree.touch("/file1.txt", new ArrayList<>(), false);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(TEST_FILE_NAME))) {
            out.writeObject(fileSystemTree);
        }

        // Deserialize the file system
        FileSystemTree deserializedFileSystem;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(TEST_FILE_NAME))) {
            deserializedFileSystem = (FileSystemTree) in.readObject();
        }

        // Assert that the file system was correctly deserialized
        DirectoryNode rootDir = deserializedFileSystem.getRoot();
        assertEquals(2, rootDir.getFiles().size(), "File count in deserialized root directory should be 1.");
        assertEquals("file1.txt", rootDir.getFiles().getFirst().getFilename(), "File name in deserialized system should be 'file1.txt'.");
    }

    @AfterEach
    public void tearDown() {
        // Clean up the files after tests
        new File(TEST_FILE_NAME).delete();
    }
}