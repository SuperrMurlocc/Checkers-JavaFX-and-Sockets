package com.bedi.warcaby;

public enum PieceType {
    RED(1), WHITE(-1);

    public int moveDir;

    PieceType(int moveDir) {
        this.moveDir = moveDir;
    }
}
