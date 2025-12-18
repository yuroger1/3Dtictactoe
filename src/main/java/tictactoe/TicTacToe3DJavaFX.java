package tictactoe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * JavaFX interface that renders an isometric 3×3×3 board with stacked planes,
 * highlights newly completed lines, and shows remaining piece life.
 */
public class TicTacToe3DJavaFX extends Application {
    private static final int PIECE_CAP = 5;
    private static final int TURN_LIMIT = 30;

    private final TextField placeX = new TextField();
    private final TextField placeY = new TextField();
    private final TextField placeZ = new TextField();
    private final TextField captureFrom = new TextField();
    private final TextField captureTo = new TextField();

    private final Label roundLabel = new Label();
    private final Label scoreLabel = new Label();
    private final Label hintLabel = new Label();

    private Game game;
    private List<Player> players;
    private IsoBoardView boardView;
    private boolean placedThisTurn = false;
    private int currentPlayerIdx = 0;

    @Override
    public void start(Stage stage) {
        this.players = promptPlayers();
        this.game = new Game(players, PIECE_CAP, TURN_LIMIT, new Random());
        this.boardView = new IsoBoardView();
        boardView.setGame(game);

        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setCenter(boardView);
        root.setRight(buildControls());

        Scene scene = new Scene(root, 1000, 720, true);
        stage.setTitle("3D Tic-Tac-Toe (JavaFX)");
        stage.setScene(scene);
        stage.show();

        refreshUi();
    }

    private VBox buildHeader() {
        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        roundLabel.setFont(Font.font(16));
        scoreLabel.setFont(Font.font(14));
        hintLabel.setFont(Font.font(13));
        header.getChildren().addAll(roundLabel, scoreLabel, hintLabel);
        return header;
    }

    private VBox buildControls() {
        VBox controls = new VBox(10);
        controls.setAlignment(Pos.TOP_CENTER);
        controls.setMinWidth(260);

        Label placeLabel = new Label("Place piece (x y z):");
        HBox placeRow = new HBox(6, placeX, placeY, placeZ);
        placeRow.setAlignment(Pos.CENTER);
        Button placeButton = new Button("Place");
        placeButton.setMaxWidth(Double.MAX_VALUE);
        placeButton.setOnAction(e -> handlePlacement());

        Label captureLabel = new Label("Capture (from x y z -> target x y z):");
        HBox captureRow = new HBox(6, captureFrom, captureTo);
        captureRow.setAlignment(Pos.CENTER);
        Button captureButton = new Button("Capture (card rounds)");
        captureButton.setMaxWidth(Double.MAX_VALUE);
        captureButton.setOnAction(e -> handleCapture());

        Button endTurnButton = new Button("End Turn");
        endTurnButton.setMaxWidth(Double.MAX_VALUE);
        endTurnButton.setOnAction(e -> advanceTurn());

        controls.getChildren().addAll(placeLabel, placeRow, placeButton,
                captureLabel, captureRow, captureButton, endTurnButton);
        return controls;
    }

    private void handlePlacement() {
        Position pos = parsePosition(placeX.getText(), placeY.getText(), placeZ.getText());
        if (pos == null) {
            hintLabel.setText("Enter numeric x y z.");
            return;
        }
        Player current = players.get(currentPlayerIdx);
        if (game.placePiece(current, pos)) {
            placedThisTurn = true;
            boardView.redraw();
            if (game.shouldOfferCard()) {
                offerAndPlayCard(current);
            }
            refreshUi();
        } else {
            hintLabel.setText("Cannot place there (occupied, frozen, or out of bounds).");
        }
    }

    private void handleCapture() {
        if (!game.shouldOfferCard()) {
            hintLabel.setText("Captures only in card rounds after empowering.");
            return;
        }
        if (!placedThisTurn) {
            hintLabel.setText("Place a piece before capturing.");
            return;
        }
        Position from = parsePositionTriplet(captureFrom.getText());
        Position target = parsePositionTriplet(captureTo.getText());
        if (from == null || target == null) {
            hintLabel.setText("Enter coordinates like 0 1 2 | 1 1 2.");
            return;
        }
        Board board = game.getBoard();
        Piece piece = board.inBounds(from) ? board.getPiece(from) : null;
        Player current = players.get(currentPlayerIdx);
        if (piece == null || piece.getOwner() != current) {
            hintLabel.setText("No empowered piece there.");
            return;
        }
        if (!piece.isEmpowered()) {
            hintLabel.setText("Selected piece is not empowered.");
            return;
        }
        if (game.empoweredCapture(current, piece, target)) {
            boardView.redraw();
            refreshUi();
            hintLabel.setText("Capture resolved.");
        } else {
            hintLabel.setText("Invalid capture target.");
        }
    }

    private void offerAndPlayCard(Player player) {
        List<Card> offers = game.offerCards();
        ChoiceDialog<Card> dialog = new ChoiceDialog<>(offers.get(0), offers);
        dialog.setTitle("Card Offer");
        dialog.setHeaderText("Choose one card to play immediately");
        dialog.setContentText("Card:");
        dialog.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Card card) {
                return card == null ? "" : card.getName();
            }

            @Override
            public Card fromString(String string) {
                return null;
            }
        });
        Optional<Card> selection = dialog.showAndWait();
        if (selection.isEmpty()) {
            hintLabel.setText("No card selected; skipping.");
            return;
        }

        Card card = selection.get();
        ActionContext ctx = buildContextForCard(card, player);
        game.useCard(card, player, ctx);
        boardView.redraw();
        refreshUi();
        if (card instanceof EmpowerCard) {
            hintLabel.setText("Piece empowered: capture available this round.");
        } else {
            hintLabel.setText("Card played: " + card.getName());
        }
    }

    private ActionContext buildContextForCard(Card card, Player current) {
        if (card instanceof LayerShiftUpCard) {
            Integer layer = promptLayer("Layer to shift up (0 bottom, 2 top):");
            return new ActionContext(layer == null ? 0 : layer);
        }
        if (card instanceof LayerShiftDownCard) {
            Integer layer = promptLayer("Layer to shift down (0 bottom, 2 top):");
            return new ActionContext(layer == null ? 0 : layer);
        }
        if (card instanceof FreezeCard) {
            Position pos = promptPosition("Enter empty cell to freeze (x y z):");
            return new ActionContext(pos);
        }
        if (card instanceof TimeRewindCard) {
            Piece piece = promptOwnPiece(current, "Enter your piece to rewind (x y z):");
            return new ActionContext(piece);
        }
        if (card instanceof EmpowerCard) {
            Piece piece = promptOwnPiece(current, "Enter your piece to empower (x y z):");
            return new ActionContext(piece);
        }
        return new ActionContext(0);
    }

    private Position promptPosition(String message) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Coordinates");
        dialog.setHeaderText(message);
        dialog.setContentText("x y z:");
        Optional<String> result = dialog.showAndWait();
        return result.map(this::parsePositionTriplet).orElse(null);
    }

    private Integer promptLayer(String message) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Layer");
        dialog.setHeaderText(message);
        dialog.setContentText("Layer index:");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(result.get().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Piece promptOwnPiece(Player player, String prompt) {
        Position pos = promptPosition(prompt);
        if (pos == null) {
            return null;
        }
        Board board = game.getBoard();
        if (!board.inBounds(pos)) {
            return null;
        }
        Piece piece = board.getPiece(pos);
        if (piece == null || piece.getOwner() != player) {
            return null;
        }
        return piece;
    }

    private void advanceTurn() {
        if (!placedThisTurn) {
            hintLabel.setText("Place a piece before ending your turn.");
            return;
        }
        currentPlayerIdx = (currentPlayerIdx + 1) % players.size();
        placedThisTurn = false;
        if (currentPlayerIdx == 0) {
            game.advanceRound();
        }
        if (game.isGameOver()) {
            hintLabel.setText("Game over.");
        }
        boardView.redraw();
        refreshUi();
    }

    private Position parsePosition(String xStr, String yStr, String zStr) {
        try {
            int x = Integer.parseInt(xStr.trim());
            int y = Integer.parseInt(yStr.trim());
            int z = Integer.parseInt(zStr.trim());
            return new Position(x, y, z);
        } catch (Exception e) {
            return null;
        }
    }

    private Position parsePositionTriplet(String text) {
        if (text == null) {
            return null;
        }
        String[] parts = text.trim().split("\\s+");
        if (parts.length != 3) {
            return null;
        }
        return parsePosition(parts[0], parts[1], parts[2]);
    }

    private List<Player> promptPlayers() {
        TextInputDialog p1Dialog = new TextInputDialog("Player 1");
        p1Dialog.setTitle("Players");
        p1Dialog.setHeaderText("Enter name for Player 1 (X)");
        String p1 = p1Dialog.showAndWait().orElse("Player 1");

        TextInputDialog p2Dialog = new TextInputDialog("Player 2");
        p2Dialog.setTitle("Players");
        p2Dialog.setHeaderText("Enter name for Player 2 (O)");
        String p2 = p2Dialog.showAndWait().orElse("Player 2");

        return List.of(new Player(p1.trim()), new Player(p2.trim()));
    }

    private void refreshUi() {
        Player current = players.get(currentPlayerIdx);
        roundLabel.setText("Round " + game.getCurrentRound() + " / " + game.getTurnLimit());
        hintLabel.setText(placedThisTurn
                ? "Capture (card rounds) or end your turn."
                : "" + current.getName() + " place a piece.");
        scoreLabel.setText(scoreSummary());
        boardView.redraw();
    }

    private String scoreSummary() {
        StringBuilder sb = new StringBuilder("Scores: ");
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            Player p = players.get(i);
            sb.append(symbolFor(p)).append(":").append(p.getScore());
        }
        return sb.toString();
    }

    private String symbolFor(Player player) {
        int idx = players.indexOf(player);
        if (idx == 0) {
            return "X";
        }
        if (idx == 1) {
            return "O";
        }
        String name = player.getName();
        return name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private class IsoBoardView extends Canvas {
        private static final double CELL = 70;
        private static final double LAYER_HEIGHT = 50;
        private static final Color CELL_COLOR = Color.rgb(245, 230, 200);
        private Game game;

        IsoBoardView() {
            super(700, 620);
            widthProperty().addListener((obs, oldV, newV) -> redraw());
            heightProperty().addListener((obs, oldV, newV) -> redraw());
        }

        void setGame(Game game) {
            this.game = game;
        }

        void redraw() {
            if (game == null) {
                return;
            }
            GraphicsContext gc = getGraphicsContext2D();
            gc.clearRect(0, 0, getWidth(), getHeight());

            double originX = getWidth() / 2;
            double originY = getHeight() / 2 + 80;

            drawCells(gc, originX, originY);
            drawFrame(gc, originX, originY);
            drawPieces(gc, originX, originY);
            drawLines(gc, originX, originY);
        }

        private void drawCells(GraphicsContext gc, double originX, double originY) {
            Board board = game.getBoard();
            for (int z = 0; z < Board.SIZE; z++) {
                double shade = 1.0 - (z * 0.06);
                Color layerColor = CELL_COLOR.deriveColor(0, 1, shade, 1);
                for (int x = 0; x < Board.SIZE; x++) {
                    for (int y = 0; y < Board.SIZE; y++) {
                        Point2D p1 = project(originX, originY, x, y, z);
                        Point2D p2 = project(originX, originY, x + 1, y, z);
                        Point2D p3 = project(originX, originY, x + 1, y + 1, z);
                        Point2D p4 = project(originX, originY, x, y + 1, z);
                        double[] xs = {p1.getX(), p2.getX(), p3.getX(), p4.getX()};
                        double[] ys = {p1.getY(), p2.getY(), p3.getY(), p4.getY()};
                        gc.setFill(layerColor);
                        gc.fillPolygon(xs, ys, 4);
                        gc.setStroke(Color.BLACK);
                        gc.setLineWidth(1.0);
                        gc.strokePolygon(xs, ys, 4);

                        Position pos = new Position(x, y, z);
                        if (board.isFrozen(pos)) {
                            gc.setFill(Color.rgb(180, 200, 220, 0.6));
                            gc.fillPolygon(xs, ys, 4);
                            gc.setFill(Color.DARKBLUE);
                            Point2D center = projectCenter(originX, originY, pos);
                            gc.setFont(Font.font(12));
                            gc.fillText("F" + board.frozenTurnsRemaining(pos), center.getX() - 6, center.getY());
                        }
                    }
                }
            }
        }

        private void drawPieces(GraphicsContext gc, double originX, double originY) {
            Board board = game.getBoard();
            gc.setFont(Font.font(22));
            for (int z = 0; z < Board.SIZE; z++) {
                for (int x = 0; x < Board.SIZE; x++) {
                    for (int y = 0; y < Board.SIZE; y++) {
                        Position pos = new Position(x, y, z);
                        Piece piece = board.getPiece(pos);
                        if (piece == null) {
                            continue;
                        }
                        Point2D center = projectCenter(originX, originY, pos);
                        String symbol = symbolFor(piece.getOwner());
                        if (piece.isEmpowered()) {
                            symbol += "*";
                        }
                        Color color = players.indexOf(piece.getOwner()) == 0 ? Color.RED : Color.BLUE;
                        gc.setFill(color);
                        gc.fillText(symbol, center.getX() - 8, center.getY());

                        gc.setFill(Color.DARKGRAY);
                        gc.setFont(Font.font(12));
                        int life = piece.turnsLifeRemaining(game.getTurnLimit());
                        gc.fillText("Life:" + life, center.getX() - 14, center.getY() + 14);
                    }
                }
            }
        }

        private void drawLines(GraphicsContext gc, double originX, double originY) {
            List<Game.ScoredLine> lines = game.getLastCompletedLines();
            if (lines.isEmpty()) {
                return;
            }
            gc.setLineWidth(4);
            for (Game.ScoredLine line : lines) {
                List<Position> positions = line.getPositions();
                Position start = positions.get(0);
                Position end = positions.get(positions.size() - 1);
                Point2D a = projectCenter(originX, originY, start);
                Point2D b = projectCenter(originX, originY, end);
                Color color = players.indexOf(line.getPlayer()) == 0 ? Color.RED : Color.BLUE;
                gc.setStroke(color);
                gc.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
            }
            gc.setLineWidth(1.0);
        }

        private void drawFrame(GraphicsContext gc, double originX, double originY) {
            gc.setStroke(Color.DARKGRAY);
            gc.setLineWidth(2.0);
            Point2D[] corners = new Point2D[]{
                    project(originX, originY, 0, 0, 0),
                    project(originX, originY, Board.SIZE, 0, 0),
                    project(originX, originY, Board.SIZE, Board.SIZE, 0),
                    project(originX, originY, 0, Board.SIZE, 0),
                    project(originX, originY, 0, 0, Board.SIZE),
                    project(originX, originY, Board.SIZE, 0, Board.SIZE),
                    project(originX, originY, Board.SIZE, Board.SIZE, Board.SIZE),
                    project(originX, originY, 0, Board.SIZE, Board.SIZE)
            };
            int[][] edges = {
                    {0, 1}, {1, 2}, {2, 3}, {3, 0},
                    {4, 5}, {5, 6}, {6, 7}, {7, 4},
                    {0, 4}, {1, 5}, {2, 6}, {3, 7}
            };
            for (int[] edge : edges) {
                Point2D a = corners[edge[0]];
                Point2D b = corners[edge[1]];
                gc.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
            }
            gc.setLineWidth(1.0);
        }

        private Point2D projectCenter(double originX, double originY, Position pos) {
            return project(originX, originY, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ());
        }

        private Point2D project(double originX, double originY, double x, double y, double z) {
            double isoX = (x - y) * CELL * 0.9;
            double isoY = (x + y) * CELL * 0.45 - z * LAYER_HEIGHT;
            return new Point2D(originX + isoX, originY + isoY);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
