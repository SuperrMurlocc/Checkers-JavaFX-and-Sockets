package com.bedi.warcaby;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Chessboard extends Application {
    public static final int TILE_SIZE = 100;
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;

    private final Tile[][] board = new Tile[WIDTH][HEIGHT];

    private final Group tileGroup = new Group();
    private final Group pieceGroup = new Group();

    private Parent createContent() {
        Pane root = new Pane();
        root.setPrefSize(WIDTH * TILE_SIZE, HEIGHT * TILE_SIZE);
        root.getChildren().addAll(tileGroup, pieceGroup);

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Tile tile = new Tile((x + y) % 2 == 0, x, y);
                board[x][y] = tile;

                tileGroup.getChildren().add(tile);

                Piece piece = null;
                if (y <= 2 && (x + y) % 2 != 0) {
                    piece = makePiece(PieceType.RED, x, y);
                } else if (y >= 5 && (x + y) % 2 != 0) {
                    piece = makePiece(PieceType.WHITE, x, y);
                }

                if (piece != null) {
                    tile.setPiece(piece);
                    pieceGroup.getChildren().add(piece);
                }
            }
        }


        return root;
    }

    private int pixelToBoard(double pixel) {
        return (int)(pixel + Chessboard.TILE_SIZE / 2) / TILE_SIZE;
    }

    private MoveResult tryMove(Piece piece, int newX, int newY) {
        if (board[newX][newY].hasPiece() || (newX + newY) % 2 == 0) {
            return new MoveResult(MoveType.NONE);
        }

        int oldX = pixelToBoard(piece.getOldX());
        int oldY = pixelToBoard(piece.getOldY());

        if (Math.abs(newX - oldX) == 1 && newY - oldY == piece.getPieceType().moveDir ) {
            return new MoveResult(MoveType.NORMAL);
        } else if (Math.abs(newX - oldX) == 2 && Math.abs(newY - oldY) == 2) {
            int middleX = oldX + (newX - oldX) / 2;
            int middleY = oldY + (newY - oldY) / 2;

            if (board[middleX][middleY].hasPiece() && board[middleX][middleY].getPiece().getPieceType() != piece.getPieceType()) {
                return new MoveResult(MoveType.KILL, board[middleX][middleY].getPiece());
            }
        } else if (piece.getPieceType().moveDir == 0 && Math.abs(newX - oldX) == 1 && Math.abs(newY - oldY) == 1) {
            return new MoveResult(MoveType.NORMAL);
        }

        return new MoveResult(MoveType.NONE);
    }

    private Piece makePiece(PieceType pieceType, int x, int y) {
        Piece piece = new Piece(pieceType, x ,y);

        piece.setOnMouseReleased(e -> {
            int newX = pixelToBoard(piece.getLayoutX());
            int newY = pixelToBoard(piece.getLayoutY());

            MoveResult moveResult = tryMove(piece, newX, newY);

            switch (moveResult.getMoveType()) {
                case NONE -> piece.abortMove();
                case NORMAL -> {
                    board[pixelToBoard(piece.getOldX())][pixelToBoard(piece.getOldY())].setPiece(null);
                    piece.move(newX, newY);
                    board[newX][newY].setPiece(piece);

                    if (newY == 7 || newY == 0) {
                        piece.promote();
                    }
                }
                case KILL -> {
                    board[pixelToBoard(piece.getOldX())][pixelToBoard(piece.getOldY())].setPiece(null);
                    piece.move(newX, newY);
                    board[newX][newY].setPiece(piece);

                    Piece otherPiece = moveResult.getPiece();
                    board[pixelToBoard(otherPiece.getOldX())][pixelToBoard(otherPiece.getOldY())].setPiece(null);
                    pieceGroup.getChildren().remove(otherPiece);

                    if (newY == 7 || newY == 0) {
                        piece.promote();
                    }
                }
            }
        });

        return piece;
    }

    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(createContent());
        stage.setTitle("Checkers");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
