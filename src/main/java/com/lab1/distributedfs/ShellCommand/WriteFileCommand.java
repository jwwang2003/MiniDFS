package com.lab1.distributedfs.ShellCommand;

import com.lab1.distributedfs.Helper;
import com.lab1.distributedfs.IO.Client.Open;
import com.lab1.distributedfs.IO.Client.OpenMode;
import com.lab1.distributedfs.Message.Message;
import com.lab1.distributedfs.Message.MessageAction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class WriteFileCommand extends WriteCommand {
    @Override
    public String getDescription() {
        return "write_file: Writes data from an external file to a \"virtual\" file.";
    }

    @Override
    public String getHelpMessage() {
        return """
                Usage: write_file <data_path> <pathname>?
                    Data read from the data file will be appended to the most
                    recently opened file or file specified by the pathname.
                    <data_path> - Path to the data file we want to write (or append).
                    <pathname> - Pathname to the file to write (or append).""";
    }

    @Override
    public boolean handle(List<String> commandArgs) {
        if (!commandArgs.isEmpty() && commandArgs.getFirst().equals("help")) {
            System.out.println(this.getHelpMessage());
            return true;
        }

        if (commandArgs.isEmpty()) {
            System.out.println("Error: write_file expects at least one argument.");
            return false;
        }

        String dataFilePath = commandArgs.get(0);
        String path = commandArgs.size() > 1 ? commandArgs.get(1) : "";

        try {
            // Read and split the file into chunks
            byte[] data = Helper.readFileIntoByteArray(dataFilePath);
            List<byte[]> chunks = Helper.splitFileIntoChunks(data);

            makeRequest(MessageAction.OPEN, new Open(OpenMode.W, path));
            Message<?> openReply = waitForResponse();
            assert openReply != null;

            if (openReply.getMessageAction() != MessageAction.OPEN) throw new Exception(String.valueOf(openReply.getData()));
            assert openReply.getMessageAction() == MessageAction.OPEN && openReply.getData() instanceof Open;

            Open open = (Open) openReply.getData();
            this.handleWrite(open, data);
        } catch (IOException e) {
            System.out.printf("Error: %s.\n", e.getMessage());
            return true;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return true;
        }

        return true;
    }
}
