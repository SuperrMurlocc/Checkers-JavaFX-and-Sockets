package com.bedi.warcaby;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;

    private final Tile[][] board = new Tile[WIDTH][HEIGHT];

    private Socket socket1;
    private BufferedWriter bufferedWriter1;
    private BufferedReader bufferedReader1;
    private Socket socket2;
    private BufferedWriter bufferedWriter2;
    private BufferedReader bufferedReader2;

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
        return (int)(pixel + ChessboardClient.TILE_SIZE / 2) / ChessboardClient.TILE_SIZE;
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
        } else if (piece.getPieceType().moveDir == 2 || piece.getPieceType().moveDir == -2 && Math.abs(newX - oldX) == 1 && Math.abs(newY - oldY) == 1) {
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

                if (newY == 7 || newY == 0) {
                    piece.promote();
                }
            }
        }
    }

    private Piece makePiece(PieceType pieceType, int x, int y) {
        return new Piece(pieceType, x ,y);
    }

    public static void ping(BufferedWriter bufferedWriter) throws IOException {
        bufferedWriter.write("PING");
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    public boolean processMove(int moveDir) {
        while (true) {
            try {
                BufferedReader fromBufferedReader;
                BufferedWriter fromBufferedWriter;
                BufferedWriter toBufferedWriter;

                if (moveDir == -1) {
                    fromBufferedReader = bufferedReader1;
                    fromBufferedWriter = bufferedWriter1;
                    toBufferedWriter = bufferedWriter2;
                } else {
                    fromBufferedReader = bufferedReader2;
                    fromBufferedWriter = bufferedWriter2;
                    toBufferedWriter = bufferedWriter1;
                }

                ping(fromBufferedWriter);

                String messageFrom = fromBufferedReader.readLine();
                System.out.println(messageFrom);

                int fromX = Integer.parseInt(messageFrom.split(" ")[0]);
                int fromY = Integer.parseInt(messageFrom.split(" ")[1]);
                int newX = (int) Float.parseFloat(messageFrom.split(" ")[2]);
                int newY = (int) Float.parseFloat(messageFrom.split(" ")[3]);

                MoveResult moveResult = tryMove(board[fromX][fromY].getPiece(), newX, newY);
                if (Math.signum(board[fromX][fromY].getPiece().getPieceType().moveDir) == Math.signum(moveDir)) {
                    fromBufferedWriter.write(Coder.encode(board[fromX][fromY].getPiece(), newX, newY, new MoveResult(MoveType.NONE)));
                    fromBufferedWriter.newLine();
                    fromBufferedWriter.flush();

                    return false;
                }

                String toMessage = Coder.encode(board[fromX][fromY].getPiece(), newX, newY, moveResult);
                makeMove(board[fromX][fromY].getPiece(), newX, newY, moveResult);

                toBufferedWriter.write(toMessage);
                toBufferedWriter.newLine();
                toBufferedWriter.flush();

                fromBufferedWriter.write(toMessage);
                fromBufferedWriter.newLine();
                fromBufferedWriter.flush();

                if (moveResult.getMoveType() == MoveType.NONE) {
                    return false;
                } else {
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public ClientHandler(Socket socket1, Socket socket2) throws IOException {
        this.socket1 = socket1;
        bufferedWriter1 = new BufferedWriter(new OutputStreamWriter(socket1.getOutputStream()));
        bufferedReader1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));

        this.socket2 = socket2;
        if (socket2 != null) {
            bufferedWriter2 = new BufferedWriter(new OutputStreamWriter(socket2.getOutputStream()));
            bufferedReader2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));
        }

        bufferedWriter1.write("1");
        bufferedWriter1.newLine();
        bufferedWriter1.flush();

        bufferedWriter2.write("2");
        bufferedWriter2.newLine();
        bufferedWriter2.flush();
    }


    @Override
    public void run() {
        createContent();
        int i = 1;
        while (socket1.isConnected() && socket2.isConnected()) {
            if (processMove(i % 2 * 2 - 1)) {
                i++;
            }
        }
    }
}
