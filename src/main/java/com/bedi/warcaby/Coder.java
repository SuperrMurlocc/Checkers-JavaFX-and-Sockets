package com.bedi.warcaby;

import javafx.application.Platform;
import javafx.scene.Group;

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

    public static void performNormal(Piece piece, int newX, int newY, Tile[][] board) {
        board[Coder.pixelToBoard(piece.getOldX())][Coder.pixelToBoard(piece.getOldY())].setPiece(null);
        piece.move(newX, newY);
        board[newX][newY].setPiece(piece);
    }

    public static void performKill(Piece piece, int newX, int newY, Tile[][] board) {
        board[Coder.pixelToBoard(piece.getOldX())][Coder.pixelToBoard(piece.getOldY())].setPiece(null);
        piece.move(newX, newY);
        board[newX][newY].setPiece(piece);
    }
}
