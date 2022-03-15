package com.bedi.warcaby;

public enum PieceType {
    GRAY(1), WHITE(-1), GRAY_SUP(2), WHITE_SUP(-2);

    public int moveDir;

    PieceType(int moveDir) {
        this.moveDir = moveDir;
    }
}
