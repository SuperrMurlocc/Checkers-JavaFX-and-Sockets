package com.bedi.warcaby;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;

public class Piece extends StackPane {
    private PieceType pieceType;

    private double mouseX, mouseY;
    private double oldX, oldY;

    public PieceType getPieceType() {
        return pieceType;
    }

    public Piece(PieceType pieceType, int x, int y) {
        this.pieceType = pieceType;

        oldX = x * ChessboardClient.TILE_SIZE;
        oldY = y * ChessboardClient.TILE_SIZE;
        relocate(oldX, oldY);

        Ellipse ellipseBackground = new Ellipse(ChessboardClient.TILE_SIZE * 0.3125, ChessboardClient.TILE_SIZE * 0.26);

        ellipseBackground.setFill(Color.BLACK);

        ellipseBackground.setStroke(Color.BLACK);
        ellipseBackground.setStrokeWidth(ChessboardClient.TILE_SIZE * 0.03);

        ellipseBackground.setTranslateX((ChessboardClient.TILE_SIZE - ChessboardClient.TILE_SIZE * 0.3125 * 2) / 2);
        ellipseBackground.setTranslateY((ChessboardClient.TILE_SIZE - ChessboardClient.TILE_SIZE * 0.26 * 2) / 2 + ChessboardClient.TILE_SIZE * 0.07);

        Ellipse ellipse = new Ellipse(ChessboardClient.TILE_SIZE * 0.3125, ChessboardClient.TILE_SIZE * 0.26);

        ellipse.setFill(pieceType == PieceType.RED ? Color.valueOf("#c40003") : Color.valueOf("#fff9f4"));

        ellipse.setStroke(Color.BLACK);
        ellipse.setStrokeWidth(ChessboardClient.TILE_SIZE * 0.03);

        ellipse.setTranslateX((ChessboardClient.TILE_SIZE - ChessboardClient.TILE_SIZE * 0.3125 * 2) / 2);
        ellipse.setTranslateY((ChessboardClient.TILE_SIZE - ChessboardClient.TILE_SIZE * 0.26 * 2) / 2);

        getChildren().addAll(ellipseBackground, ellipse);

        setOnMousePressed(e -> {
            mouseX = e.getSceneX();
            mouseY = e.getSceneY();
        });

        setOnMouseDragged(e -> {
            relocate(e.getSceneX() - mouseX + oldX, e.getSceneY() - mouseY + oldY);
        });

    }

    public double getOldX() {
        return oldX;
    }

    public double getOldY() {
        return oldY;
    }

    public void move(int x, int y) {
        oldX = x * ChessboardClient.TILE_SIZE;
        oldY = y * ChessboardClient.TILE_SIZE;

        relocate(oldX, oldY);
    }

    public void abortMove() {
        relocate(oldX, oldY);
    }

    public void promote() {
        promoteImage();
        pieceType.moveDir = (pieceType == PieceType.RED) ? PieceType.RED_SUP.moveDir : PieceType.WHITE_SUP.moveDir;
    }

    public void promoteImage() {
        Ellipse doubleEllipse = new Ellipse(ChessboardClient.TILE_SIZE * 0.3125 * 0.5, ChessboardClient.TILE_SIZE * 0.26 * 0.5);

        doubleEllipse.setFill(pieceType == PieceType.RED ? Color.valueOf("#c40003") : Color.valueOf("#fff9f4"));

        doubleEllipse.setStroke(Color.BLACK);
        doubleEllipse.setStrokeWidth(ChessboardClient.TILE_SIZE * 0.03);

        doubleEllipse.setTranslateX((ChessboardClient.TILE_SIZE - ChessboardClient.TILE_SIZE * 0.3125 * 2) / 2);
        doubleEllipse.setTranslateY((ChessboardClient.TILE_SIZE - ChessboardClient.TILE_SIZE * 0.26 * 2) / 2);

        getChildren().addAll(doubleEllipse);
    }
}
