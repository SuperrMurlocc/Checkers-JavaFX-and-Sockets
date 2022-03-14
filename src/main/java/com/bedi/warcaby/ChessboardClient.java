package com.bedi.warcaby;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class ChessboardClient extends Application {
    public static final int TILE_SIZE = 100;
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;

    private int player;

    private float time = 0;
    private final Text colorText = new Text();
    private final StringProperty timeString = new SimpleStringProperty();
    private final Label timeText = new Label();

    private final Tile[][] board = new Tile[WIDTH][HEIGHT];

    private final Group tileGroup = new Group();
    private final Group pieceGroup = new Group();

    private Socket socket;
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;

    private boolean isItMyTurn = false;

    private Parent createContent() {
        Pane root = new Pane();
        root.setPrefSize(WIDTH * TILE_SIZE, HEIGHT * TILE_SIZE);
        root.getChildren().addAll(tileGroup, pieceGroup, colorText, timeText);

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

        timeText.relocate(0,30);
        colorText.relocate(0,0);
        colorText.setText("You play" + ((player == 1) ? " RED" : " WHITE"));
        timeText.textProperty().bind(timeString);

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

            requestMove(piece, newX, newY);
        });

        return piece;
    }

    private void makeMove(Piece piece, int newX, int newY, MoveResult moveResult) {
        switch (moveResult.getMoveType()) {
            case NONE -> piece.abortMove();
            case NORMAL -> {
                board[pixelToBoard(piece.getOldX())][pixelToBoard(piece.getOldY())].setPiece(null);
                piece.move(newX, newY);
                board[newX][newY].setPiece(piece);

                if (newY == 7 || newY == 0) {
                    Platform.runLater(piece::promote);
                }

                isItMyTurn = false;
            }
            case KILL -> {
                board[pixelToBoard(piece.getOldX())][pixelToBoard(piece.getOldY())].setPiece(null);
                piece.move(newX, newY);
                board[newX][newY].setPiece(piece);

                Piece otherPiece = moveResult.getPiece();
                board[pixelToBoard(otherPiece.getOldX())][pixelToBoard(otherPiece.getOldY())].setPiece(null);
                Platform.runLater(() -> pieceGroup.getChildren().remove(otherPiece));

                if (newY == 7 || newY == 0) {
                    Platform.runLater(piece::promote);
                }

                isItMyTurn = false;
            }
        }

    }

    public void requestMove(Piece piece, int newX, int newY) {
        if (!isItMyTurn) {
            makeMove(piece, newX, newY, new MoveResult(MoveType.NONE));
            return;
        }

        try {
            bufferedWriter.write(pixelToBoard(piece.getOldX()) + " " + pixelToBoard(piece.getOldY()) + " " + newX + " " + newY);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        socket = new Socket("localhost", 1234);

        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            if (bufferedReader.readLine().equals("1")) {
                player = 1;
            } else {
                player = 2;
            }
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }

        countTime();
        listenToServer();

        Scene scene = new Scene(createContent());
        stage.setTitle("Checkers");
        stage.setScene(scene);
        stage.show();

    }

    public static void main(String[] args) {
        launch();
    }

    public void countTime() {
        new Thread(() -> {
            while (socket.isConnected()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (isItMyTurn) {
                    time += 0.1;
                    Platform.runLater(() -> timeString.set("Timer: " + (int)time + "s."));
                }
            }
        }).start();
    }


    public void listenToServer() {
        new Thread(() -> {
            String message;

            while (socket.isConnected()) {
                try {
                    message = bufferedReader.readLine();
                    System.out.println(message);
                    if (message.startsWith("PING")) {
                        isItMyTurn = true;
                    } else {
                        String[] partsOfMessage = message.split(" ");
                        switch(partsOfMessage[4]) {
                            case "NONE" -> {
                                int fromX = Integer.parseInt(partsOfMessage[0]);
                                int fromY = Integer.parseInt(partsOfMessage[1]);
                                int newX = Integer.parseInt(partsOfMessage[2]);
                                int newY = Integer.parseInt(partsOfMessage[3]);

                                makeMove(board[fromX][fromY].getPiece(), newX, newY, new MoveResult(MoveType.NONE));
                            }

                            case "NORMAL" -> {
                                int fromX = Integer.parseInt(partsOfMessage[0]);
                                int fromY = Integer.parseInt(partsOfMessage[1]);
                                int newX = Integer.parseInt(partsOfMessage[2]);
                                int newY = Integer.parseInt(partsOfMessage[3]);

                                makeMove(board[fromX][fromY].getPiece(), newX, newY, new MoveResult(MoveType.NORMAL));
                            }
                            case "KILL" -> {
                                int fromX = Integer.parseInt(partsOfMessage[0]);
                                int fromY = Integer.parseInt(partsOfMessage[1]);
                                int newX = Integer.parseInt(partsOfMessage[2]);
                                int newY = Integer.parseInt(partsOfMessage[3]);
                                int killX = Integer.parseInt(partsOfMessage[5]);
                                int killY = Integer.parseInt(partsOfMessage[6]);

                                makeMove(board[fromX][fromY].getPiece(), newX, newY, new MoveResult(MoveType.KILL, board[killX][killY].getPiece()));
                            }
                        }
                    }

                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }).start();
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
