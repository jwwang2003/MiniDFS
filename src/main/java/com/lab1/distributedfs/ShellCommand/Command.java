package com.lab1.distributedfs.ShellCommand;

import com.lab1.distributedfs.Node.Client;

import java.util.List;

public class Command {
    protected static Client client = new Client();

    public String getDescription() { return ""; }
    // Method to return the command help message
    public String getHelpMessage() { return ""; }

    // Method to handle the command logic
    public boolean handle(List<String> commandArgs) { return true; }
}
