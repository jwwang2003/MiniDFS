package com.lab1.distributedfs;

import com.lab1.distributedfs.ShellCommand.OpenCommand;
import com.lab1.distributedfs.ShellCommand.CloseCommand;
import com.lab1.distributedfs.ShellCommand.ReadCommand;
import com.lab1.distributedfs.ShellCommand.WriteCommand;
import com.lab1.distributedfs.ShellCommand.Command;
import com.lab1.distributedfs.ShellParser.ParseException;
import com.lab1.distributedfs.ShellParser.ShellParser;

import java.util.*;

public class Shell {
    private final Scanner scanner;
    private final Map<String, Command> commands;

    public Shell(Scanner scanner) {
        this.scanner = scanner;
        this.commands = new LinkedHashMap<>();

        // Register commands
        this.commands.put("help", new HelpCommand());
        this.commands.put("open", new OpenCommand());
        this.commands.put("close", new CloseCommand());
        this.commands.put("write", new WriteCommand());
        this.commands.put("read", new ReadCommand());
        this.commands.put("quit", new QuitCommand());
    }

    public void printWelcome() {
        System.out.println("Welcome to MiniDFS shell, a virtual DFS simulator that uses multi-threading to simulate a \"distributed\" file system.");
        System.out.println("Brought to you by Jimmy Wang (COMP130123).");
        System.out.println("Type \"quit\" to exit, or \"help\" to see valid commands.\n");
    }

    public boolean run() {
        // Display the prompt
        System.out.print("shell> ");

        // Read the user input
        String input = scanner.nextLine().trim();

        // Try parsing the input and display the result
        try {
            List<String> parsedResult = ShellParser.parseString(input);
            return this.handleShellInput(parsedResult);
        } catch (ParseException e) {
            System.out.println("Error parsing input: " + e.getMessage());
        }

        return false;
    }

    private boolean handleShellInput(List<String> parsedInput) {
        if (parsedInput.isEmpty()) {
            return false;
        }

        String command = parsedInput.getFirst().trim();
        List<String> commandArgs = parsedInput.subList(1, parsedInput.size());

        Command shellCommand = commands.get(command);
        if (shellCommand != null) {
            return shellCommand.handle(commandArgs);
        } else {
            System.out.println("Unknown command: " + command);
            return true;
        }
    }

    public class HelpCommand extends Command {
        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public String getHelpMessage() {
            return "";
        }

        @Override
        public boolean handle(List<String> commandArgs) {
            System.out.println("Available commands:");
            for (Command command : commands.values()) {
                if (command instanceof HelpCommand) { continue; }
                System.out.println("\t" + command.getDescription());
            }
            return true;
        }
    }

    public class QuitCommand extends Command {
        @Override
        public String getDescription() {
            return "quit: Quit the shell";
        }

        @Override
        public String getHelpMessage() {
            return "";
        }

        @Override
        public boolean handle(List<String> commandArgs) {
            System.out.println("Attempting to shutdown...");
            client.shutdown();
            System.exit(0);
            return false;
        }
    }
}
