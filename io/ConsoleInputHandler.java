package io;

import java.util.Scanner;

import core.Direction;

public class ConsoleInputHandler implements InputHandler {
    private Scanner scanner;

    public ConsoleInputHandler() {
        this.scanner = new Scanner(System.in);
    }

    public Direction readMovement() {
        while (true) {
            System.out.print("Move (W/A/S/D): ");
            String line = scanner.nextLine().trim().toUpperCase();
            if (line.equals("W")) return Direction.UP;
            if (line.equals("S")) return Direction.DOWN;
            if (line.equals("A")) return Direction.LEFT;
            if (line.equals("D")) return Direction.RIGHT;
            System.out.println("Invalid movement command.");
        }
    }

    public int readInt() {
        while (true) {
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException ex) {
                System.out.print("Enter a valid integer: ");
            }
        }
    }

    public boolean readYesNo() {
        while (true) {
            String line = scanner.nextLine().trim().toUpperCase();
            if (line.equals("Y") || line.equals("YES")) return true;
            if (line.equals("N") || line.equals("NO")) return false;
            System.out.print("Enter Y or N: ");
        }
    }

    public String readLine() {
        return scanner.nextLine();
    }
}

