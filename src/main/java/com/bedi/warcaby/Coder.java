package com.bedi.warcaby;

public class Coder {
    public static String encode(Piece piece, int newX, int newY, MoveResult moveResult) {
        String result = pixelToBoard(piece.getOldX()) + " " + pixelToBoard(piece.getOldY()) + " " + newX + " " + newY + " " + moveResult.getMoveType().toString();
        if (moveResult.getMoveType() == MoveType.KILL) {
            result += " " + pixelToBoard(moveResult.getPiece().getOldX()) + " " + pixelToBoard(moveResult.getPiece().getOldY());
        }
        return result;
    }

    private static int pixelToBoard(double pixel) {
        return (int)(pixel + ChessboardClient.TILE_SIZE / 2) / ChessboardClient.TILE_SIZE;
    }
}
