package com.lab1.distributedfs.ShellCommand;

import com.lab1.distributedfs.Const;
import com.lab1.distributedfs.Message.Message;
import com.lab1.distributedfs.Message.RequestType;
import com.lab1.distributedfs.Message.ResponseType;

import java.util.List;

public class LSFSCommand extends Command {
    @Override
    public String getDescription() {
        return "lsfs: Displays the current filesystem tree.";
    }

    @Override
    public String getHelpMessage() {
        return """
                    Usage: lsfs
                    Dumps the current filesystem tree into the console.""";
    }

    @Override
    public boolean handle(List<String> commandArgs) {
        if (!commandArgs.isEmpty()) {
            if (commandArgs.getFirst().equals("help")) System.out.println(this.getHelpMessage());
            else System.out.println("Error: read does not accept any arguments.");
            return true;
        }

        try {
            requestQueue.put(
                new Message<>(Const.MAIN_NODE_ID, RequestType.LSFS, null)
            );
            Message<?> lsReply = waitForResponse();
            assert lsReply != null;

            if (lsReply.getResponseType() == ResponseType.LSFS) {
                System.out.print(lsReply.getData());
            }
        } catch (InterruptedException e) { throw new RuntimeException(e); }

        return true;
    }
}
