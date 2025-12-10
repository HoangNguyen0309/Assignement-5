package io;

import core.Direction;

public interface InputHandler {
    Direction readMovement();
    int readInt();
    boolean readYesNo();
    String readLine();
}
