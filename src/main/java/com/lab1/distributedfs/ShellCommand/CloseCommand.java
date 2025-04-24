package com.lab1.distributedfs.ShellCommand;

import com.lab1.distributedfs.Message.Message;
import com.lab1.distributedfs.Message.RequestType;
import com.lab1.distributedfs.Message.ResponseType;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

public class CloseCommand extends Command {

    @Override
    public String getDescription() {
        return "close: Closes a file that was opened (releases the R/W lock).";
    }

    @Override
    public String getHelpMessage() {
        return """
                    Usage: close
                    Closes the currently opened file and releases the R/W lock,
                    if multiple files are opened, it releases the most recently opened.
                    <pathname> - (Optional) Pathname of the file to close.""";
    }

    @Override
    public boolean handle(List<String> commandArgs) {
        if (!commandArgs.isEmpty() && commandArgs.getFirst().equals("help")) {
            System.out.println(this.getHelpMessage());
            return true;
        }

        if (commandArgs.size() > 1) {
            System.out.println("Error: close accepts one or no argument.");
            return true;
        }

        String path = "";
        try { path = commandArgs.getFirst(); } catch (NoSuchElementException ignored) {}

        try {
            requestQueue.put(
                new Message<>(RequestType.CLOSE, path)
            );
            Message<?> closeReply = waitForResponse();
            assert closeReply != null;

            if (closeReply.getResponseType() == ResponseType.CLOSE) {
                System.out.println("Closed file \"" + closeReply.getData() + "\"");
            } else {
                System.out.println("Error: " + closeReply.getData());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return true;
    }
}
