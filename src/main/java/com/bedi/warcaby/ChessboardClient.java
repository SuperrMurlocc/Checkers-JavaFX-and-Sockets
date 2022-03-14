package com.bedi.warcaby;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Objects;

public class ChessboardClient extends Application {
    public static final int TILE_SIZE = 100;
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;

    private final Tile[][] board = new Tile[WIDTH][HEIGHT];

    private final Group tileGroup = new Group();
    private final Group pieceGroup = new Group();

    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;

    private Parent createContent() throws IOException {
        Socket socket = new Socket("localhost", 1234);

        try {
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }

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
        return (int)(pixel + ChessboardClient.TILE_SIZE / 2) / TILE_SIZE;
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

    public MoveResult tryMove(Piece piece, int newX, int newY) {
        String string = null;

        try {
            bufferedWriter.write(pixelToBoard(piece.getOldX()) + " " + pixelToBoard(piece.getOldY()) + " " + newX + " " + newY);
            bufferedWriter.newLine();
            bufferedWriter.flush();

            string = bufferedReader.readLine();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        MoveResult moveResult;
        switch (Objects.requireNonNull(string).substring(0, 4)) {
            case "NORM" -> moveResult = new MoveResult(MoveType.NORMAL);
            case "KILL" -> {
                int killX = (int) Float.parseFloat(string.split(" ")[1]);
                int killY = (int) Float.parseFloat(string.split(" ")[2]);
                moveResult = new MoveResult(MoveType.KILL, board[killX][killY].getPiece());
            }
            default -> moveResult = new MoveResult(MoveType.NONE);
        }
        return moveResult;
    }

    @Override
    public void start(Stage stage) throws IOException {
        Scene scene = new Scene(createContent());
        stage.setTitle("Checkers");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) throws IOException {
        launch();
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
