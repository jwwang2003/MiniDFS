package com.lab1.distributedfs;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Shell shell = new Shell(scanner);
        shell.printWelcome();
        while (shell.run()) {
            continue;
        }
        scanner.close();
    }
}
