package com.bedi.warcaby;

public class MoveResult {
    private MoveType moveType;
    private Piece piece;

    public MoveType getMoveType() {
        return moveType;
    }

    public Piece getPiece() {
        return piece;
    }

    public MoveResult(MoveType moveType) {
        this(moveType, null);
    }

    public MoveResult(MoveType moveType, Piece piece) {
        this.moveType = moveType;
        this.piece = piece;
    }
}
