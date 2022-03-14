package com.bedi.warcaby;

import javafx.scene.Group;
import javafx.scene.layout.Pane;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ChessboardServer {
    public static final int TILE_SIZE = 100;
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;

    private final Tile[][] board = new Tile[WIDTH][HEIGHT];

    private final Group tileGroup = new Group();
    private final Group pieceGroup = new Group();

    private ServerSocket serverSocket;

    private void createContent() {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Tile tile = new Tile((x + y) % 2 == 0, x, y);
                board[x][y] = tile;

                Piece piece = null;
                if (y <= 2 && (x + y) % 2 != 0) {
                    piece = makePiece(PieceType.RED, x, y);
                } else if (y >= 5 && (x + y) % 2 != 0) {
                    piece = makePiece(PieceType.WHITE, x, y);
                }

                if (piece != null) {
                    tile.setPiece(piece);
                }
            }
        }

    }

    private int pixelToBoard(double pixel) {
        return (int)(pixel + ChessboardClient.TILE_SIZE / 2) / TILE_SIZE;
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

    private void makeMove(Piece piece, int newX, int newY, MoveResult moveResult) {
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
    }

    private Piece makePiece(PieceType pieceType, int x, int y) {
        return new Piece(pieceType, x ,y);
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(1234);
        ChessboardServer chessboardServer = new ChessboardServer(serverSocket);
        chessboardServer.startServer();
    }

    public ChessboardServer(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void startServer() throws IOException {
        createContent();
        Socket socket = serverSocket.accept();
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        socket = serverSocket.accept();
        BufferedWriter bufferedWriter2 = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        BufferedReader bufferedReader2 = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        while (true) {
            processMove(bufferedReader, bufferedWriter);
            processMove(bufferedReader2, bufferedWriter2);
        }
    }

    public void processMove(BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            String string = bufferedReader.readLine();
            int X = Integer.parseInt(string.split(" ")[0]);
            int Y = Integer.parseInt(string.split(" ")[1]);
            int newX = (int) Float.parseFloat(string.split(" ")[2]);
            int newY = (int) Float.parseFloat(string.split(" ")[3]);

            MoveResult moveResult = tryMove(board[X][Y].getPiece(), newX, newY);
            makeMove(board[X][Y].getPiece(), newX, newY, moveResult);

            switch (moveResult.getMoveType()) {
                case NONE -> string = "NONE";
                case NORMAL -> string = "NORMAL";
                case KILL -> string = "KILL " + pixelToBoard(moveResult.getPiece().getOldX()) + " " + pixelToBoard(moveResult.getPiece().getOldY());
                default -> string = "NONE";
            }

            bufferedWriter.write(string);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
