package com.lab1.distributedfs;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Create a scanner to read input from the user
        Scanner scanner = new Scanner(System.in);
        // Initialize shell
        Shell shell = new Shell(scanner);
        shell.printWelcome();

        // Loop until the user enters 'quit'
        while (shell.run()) {
            // Do stuff...
            continue;
        }

        // End of program
        scanner.close();
    }
}
