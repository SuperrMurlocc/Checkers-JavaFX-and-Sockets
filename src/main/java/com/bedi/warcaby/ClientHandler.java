package com.bedi.warcaby;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Tile[][] board = new Tile[ChessboardClient.WIDTH][ChessboardClient.HEIGHT];

    private final Socket socket1;
    private final BufferedWriter bufferedWriter1;
    private final BufferedReader bufferedReader1;
    private final Socket socket2;
    private final BufferedWriter bufferedWriter2;
    private final BufferedReader bufferedReader2;

    private int grayPieces = 0;
    private int whitePieces = 0;

    public ClientHandler(Socket socket1, Socket socket2) throws IOException {
        try {
            this.socket1 = socket1;
            bufferedWriter1 = new BufferedWriter(new OutputStreamWriter(socket1.getOutputStream()));
            bufferedReader1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
            bufferedWriter1.write("1");
            bufferedWriter1.newLine();
            bufferedWriter1.flush();

            this.socket2 = socket2;
            if (socket2 != null) {
                bufferedWriter2 = new BufferedWriter(new OutputStreamWriter(socket2.getOutputStream()));
                bufferedReader2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));
                bufferedWriter2.write("2");
                bufferedWriter2.newLine();
                bufferedWriter2.flush();
            } else {
                bufferedWriter2 = null;
                bufferedReader2 = null;
            }
        } catch (IOException e) {
            closeEverything();
            throw e;
        }
    }

    @Override
    public void run() {
        createContent();
        int i = 1;
        while (socket1.isConnected() && socket2.isConnected() && whitePieces * grayPieces > 0) {
            try {
                if (processMove(i % 2 * 2 - 1)) {
                    i++;
                }
            } catch (IOException e) {
                closeEverything();
                e.printStackTrace();
            }
        }
    }

    private void createContent() {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Tile tile = new Tile((x + y) % 2 == 0, x, y);
                board[x][y] = tile;

                Piece piece = null;
                if (y <= 2 && (x + y) % 2 != 0) {
                    piece = new Piece(PieceType.GRAY, x, y);
                    grayPieces++;
                } else if (y >= 5 && (x + y) % 2 != 0) {
                    piece = new Piece(PieceType.WHITE, x, y);
                    whitePieces++;
                }

                if (piece != null) {
                    tile.setPiece(piece);
                }
            }
        }
    }

    public static void ping(BufferedWriter bufferedWriter) throws IOException {
        bufferedWriter.write("PING");
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    private MoveResult tryMove(Piece piece, int newX, int newY) {
        if (board[newX][newY].hasPiece() || (newX + newY) % 2 == 0) {
            return new MoveResult(MoveType.NONE);
        }

        int oldX = Coder.pixelToBoard(piece.getOldX());
        int oldY = Coder.pixelToBoard(piece.getOldY());

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

    public boolean processMove(int moveDir) throws IOException {
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

            if (whitePieces == 0 || grayPieces == 0) {
                String endOfGameMessage;
                if (whitePieces == 0) {
                    endOfGameMessage = "1 2 3 4 END1";
                } else {
                    endOfGameMessage = "1 2 3 4 END2";
                }

                toBufferedWriter.write(endOfGameMessage);
                toBufferedWriter.newLine();
                toBufferedWriter.flush();

                fromBufferedWriter.write(endOfGameMessage);
                fromBufferedWriter.newLine();
                fromBufferedWriter.flush();
            }

            return moveResult.getMoveType() != MoveType.NONE;
        } catch (IOException e) {
            closeEverything();
            e.printStackTrace();
            throw e;
        }
    }

    public void makeMove(Piece piece, int newX, int newY, MoveResult moveResult) {
        MoveType moveType = moveResult.getMoveType();
        switch (moveType) {
            case NONE -> piece.abortMove();
            case NORMAL -> {
                board[Coder.pixelToBoard(piece.getOldX())][Coder.pixelToBoard(piece.getOldY())].setPiece(null);
                piece.move(newX, newY);
                board[newX][newY].setPiece(piece);
            }
            case KILL -> {
                board[Coder.pixelToBoard(piece.getOldX())][Coder.pixelToBoard(piece.getOldY())].setPiece(null);
                piece.move(newX, newY);
                board[newX][newY].setPiece(piece);
                if (piece.getPieceType() == PieceType.GRAY || piece.getPieceType() == PieceType.GRAY_SUP) {
                    whitePieces--;
                } else {
                    grayPieces--;
                }

                Piece otherPiece = moveResult.getPiece();
                board[Coder.pixelToBoard(otherPiece.getOldX())][Coder.pixelToBoard(otherPiece.getOldY())].setPiece(null);
                if ((newY == 7 && piece.getPieceType() == PieceType.GRAY) || (newY == 0 && piece.getPieceType() == PieceType.WHITE)) {
                    piece.promote();
                }
            }
        }
    }

    private void closeEverything() {
        try {
            if (bufferedReader1 != null) {
                bufferedReader1.close();
            }
            if (bufferedWriter1 != null) {
                bufferedWriter1.close();
            }
            if (socket1 != null) {
                socket1.close();
            }
            if (bufferedReader2 != null) {
                bufferedReader2.close();
            }
            if (bufferedWriter2 != null) {
                bufferedWriter2.close();
            }
            if (socket2 != null) {
                socket2.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
