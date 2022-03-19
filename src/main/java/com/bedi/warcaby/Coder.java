package com.bedi.warcaby;

import java.util.Random;

public class Coder {
    public static String encode(Piece piece, int newX, int newY, MoveResult moveResult) {
        String result = pixelToBoard(piece.getOldX()) + " " + pixelToBoard(piece.getOldY()) + " " + newX + " " + newY + " " + moveResult.getMoveType().toString();
        if (moveResult.getMoveType() == MoveType.KILL) {
            result += " " + pixelToBoard(moveResult.getPiece().getOldX()) + " " + pixelToBoard(moveResult.getPiece().getOldY());
        }
        return result;
    }

    public static int pixelToBoard(double pixel) {
        return (int)(pixel + ChessboardClient.TILE_SIZE / 2) / ChessboardClient.TILE_SIZE;
    }

    private static int randInt(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min) + min;
    }

    public static String generateMove() {
        return randInt(0,8) + " " + randInt(0,8) + " " + randInt(0,8) + " " + randInt(0,8);
    }
}
