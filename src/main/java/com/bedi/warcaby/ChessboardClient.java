package com.bedi.warcaby;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChessboardClient extends Application {
    public static final int TILE_SIZE = 100;
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;

    public static String mode = null;

    private final Tile[][] board = new Tile[WIDTH][HEIGHT];

    private final Group tileGroup = new Group();
    private final Group pieceGroup = new Group();

    private Socket socket;
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;

    private int player;
    private int winner = 0;
    private final Label colorLabel = new Label();

    private float time = 0;
    private final Timer timer = new Timer();

    private boolean isItMyTurn = false;

    public static void main(String[] args) {
        mode = args[0];
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException, InterruptedException {
        try {
            socket = new Socket("localhost", 1234);
        } catch (IOException e) {
            Thread.sleep(5000);
            socket = new Socket("localhost", 1234);
        }

        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bufferedWriter.write(mode);
            bufferedWriter.newLine();
            bufferedWriter.flush();

            if (bufferedReader.readLine().equals("1")) {
                player = 1;
            } else {
                player = 2;
            }
        } catch (IOException e) {
            closeEverything();
        }

        countTime();
        listenToServer();

        Scene scene = new Scene(createContent());
        stage.setTitle("Checkers");
        stage.setScene(scene);
        stage.show();

    }

    private Parent createContent() {
        Pane root = new Pane();
        root.setPrefSize(WIDTH * TILE_SIZE, HEIGHT * TILE_SIZE);
        root.getChildren().addAll(tileGroup, pieceGroup, colorLabel, timer);

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Tile tile = new Tile((x + y) % 2 == 0, x, y);
                board[x][y] = tile;

                tileGroup.getChildren().add(tile);

                Piece piece = null;
                if (y <= 2 && (x + y) % 2 != 0) {
                    piece = makePiece(PieceType.GRAY, x, y);
                } else if (y >= 5 && (x + y) % 2 != 0) {
                    piece = makePiece(PieceType.WHITE, x, y);
                }

                if (piece != null) {
                    tile.setPiece(piece);
                    pieceGroup.getChildren().add(piece);
                }
            }
        }

        colorLabel.relocate(0,0);
        colorLabel.setText("You play" + ((player == 1) ? " GRAY" : " WHITE"));

        return root;
    }

    private Piece makePiece(PieceType pieceType, int x, int y) {
        Piece piece = new Piece(pieceType, x ,y);

        piece.setOnMouseReleased(e -> {
            int newX = Coder.pixelToBoard(piece.getLayoutX());
            int newY = Coder.pixelToBoard(piece.getLayoutY());

            requestMove(piece, newX, newY);
        });

        return piece;
    }

    public void requestMove(Piece piece, int newX, int newY) {
        if (!isItMyTurn) {
            makeMove(piece, newX, newY, new MoveResult(MoveType.NONE));
            return;
        }

        try {
            bufferedWriter.write(Coder.pixelToBoard(piece.getOldX()) + " " + Coder.pixelToBoard(piece.getOldY()) + " " + newX + " " + newY);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void makeMove(Piece piece, int newX, int newY, MoveResult moveResult) {
        MoveType moveType = moveResult.getMoveType();
        switch (moveType) {
            case NONE -> piece.abortMove();
            case NORMAL -> {
                board[Coder.pixelToBoard(piece.getOldX())][Coder.pixelToBoard(piece.getOldY())].setPiece(null);
                piece.move(newX, newY);
                board[newX][newY].setPiece(piece);
                isItMyTurn = false;
                if ((newY == 7 && piece.getPieceType() == PieceType.GRAY) || (newY == 0 && piece.getPieceType() == PieceType.WHITE)) {
                    Platform.runLater(piece::promote);
                }
            }
            case KILL -> {
                board[Coder.pixelToBoard(piece.getOldX())][Coder.pixelToBoard(piece.getOldY())].setPiece(null);
                piece.move(newX, newY);
                board[newX][newY].setPiece(piece);

                Piece otherPiece = moveResult.getPiece();
                board[Coder.pixelToBoard(otherPiece.getOldX())][Coder.pixelToBoard(otherPiece.getOldY())].setPiece(null);
                Platform.runLater(() -> pieceGroup.getChildren().remove(otherPiece));
                isItMyTurn = false;
                if ((newY == 7 && piece.getPieceType() == PieceType.GRAY) || (newY == 0 && piece.getPieceType() == PieceType.WHITE)) {
                    Platform.runLater(piece::promote);
                }
            }
        }
    }

    public void countTime() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            if (winner != 0) {
                Platform.runLater(() -> timer.set((winner == player) ? "You won!" : "You lost!"));
                executor.shutdown();
            }

            if (isItMyTurn) {
                time += 0.1;
                Platform.runLater(() -> timer.set("Timer: " + (int)time + "s."));
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    public void listenToServer() {
        new Thread(() -> {
            String message;

            while (socket.isConnected() && winner == 0) {
                try {
                    message = bufferedReader.readLine();
                    System.out.println(message);
                    if (message.startsWith("PING")) {
                        isItMyTurn = true;
                    } else {
                        String[] partsOfMessage = message.split(" ");
                        int fromX = Integer.parseInt(partsOfMessage[0]);
                        int fromY = Integer.parseInt(partsOfMessage[1]);
                        int newX = Integer.parseInt(partsOfMessage[2]);
                        int newY = Integer.parseInt(partsOfMessage[3]);

                        Piece piece = board[fromX][fromY].getPiece();

                        switch(partsOfMessage[4]) {
                            case "NONE" -> makeMove(piece, newX, newY, new MoveResult(MoveType.NONE));
                            case "NORMAL" -> makeMove(piece, newX, newY, new MoveResult(MoveType.NORMAL));
                            case "KILL" -> {
                                int killX = Integer.parseInt(partsOfMessage[5]);
                                int killY = Integer.parseInt(partsOfMessage[6]);
                                Piece killedPiece = board[killX][killY].getPiece();

                                makeMove(piece, newX, newY, new MoveResult(MoveType.KILL, killedPiece));
                            }
                            case "END1" -> winner = 1;
                            case "END2" -> winner = 2;
                        }
                    }
                } catch (IOException e) {
                    closeEverything();
                }
            }
            closeEverything();
        }).start();
    }

    private void closeEverything() {
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


