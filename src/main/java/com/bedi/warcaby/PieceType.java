package com.bedi.warcaby;

public enum PieceType {
    RED(1), WHITE(-1), RED_SUP(2), WHITE_SUP(-2);

    public int moveDir;

    PieceType(int moveDir) {
        this.moveDir = moveDir;
    }
}
