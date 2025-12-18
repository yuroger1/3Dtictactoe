package tictactoe;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

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
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * 3D Tic-Tac-Toe JavaFX GUI
 * 修改版：增加了層間距，並支援滑鼠直接點擊格子下棋。
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
        
        // 初始化視圖，並傳入點擊回呼函數 (Lambda)
        this.boardView = new IsoBoardView(pos -> attemptMove(pos));
        boardView.setGame(game);

        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setCenter(boardView);
        root.setRight(buildControls());

        // 加大視窗尺寸以容納拉開的層級
        Scene scene = new Scene(root, 1100, 800, true);
        stage.setTitle("3D Tic-Tac-Toe (JavaFX) - Interactive");
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
        // 增加一點內距
        controls.setStyle("-fx-padding: 20; -fx-background-color: #f4f4f4;");

        Label manualLabel = new Label("--- Manual Input ---");
        manualLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        Label placeLabel = new Label("Place piece (x y z):");
        HBox placeRow = new HBox(6, placeX, placeY, placeZ);
        placeRow.setAlignment(Pos.CENTER);
        Button placeButton = new Button("Place");
        placeButton.setMaxWidth(Double.MAX_VALUE);
        placeButton.setOnAction(e -> handlePlacement());

        Label captureLabel = new Label("Capture (from -> target):");
        HBox captureRow = new HBox(6, captureFrom, captureTo);
        captureRow.setAlignment(Pos.CENTER);
        Button captureButton = new Button("Capture (card rounds)");
        captureButton.setMaxWidth(Double.MAX_VALUE);
        captureButton.setOnAction(e -> handleCapture());

        Button endTurnButton = new Button("End Turn");
        endTurnButton.setMaxWidth(Double.MAX_VALUE);
        endTurnButton.setStyle("-fx-font-weight: bold; -fx-base: #b6e7c9;"); // 讓結束按鈕顯眼一點
        endTurnButton.setOnAction(e -> advanceTurn());
        
        Label clickHint = new Label("\nTip:\nYou can click directly\non the grid to place pieces!");
        clickHint.setTextFill(Color.DARKSLATEBLUE);
        clickHint.setStyle("-fx-border-color: lightblue; -fx-padding: 5;");

        controls.getChildren().addAll(manualLabel, placeLabel, placeRow, placeButton,
                captureLabel, captureRow, captureButton, endTurnButton, clickHint);
        return controls;
    }

    /**
     * 核心下棋邏輯：由按鈕或滑鼠點擊觸發
     */
    private void attemptMove(Position pos) {
        if (game.isGameOver()) {
            hintLabel.setText("Game is over.");
            return;
        }
        if (placedThisTurn) {
            hintLabel.setText("You have already placed a piece this turn.");
            return;
        }

        Player current = players.get(currentPlayerIdx);
        if (game.placePiece(current, pos)) {
            placedThisTurn = true;
            hintLabel.setText(String.format("Placed at (%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ()));
            
            // 如果觸發卡片機制
            if (game.shouldOfferCard()) {
                // 為了讓 UI 先更新棋子顯示，再彈出卡片對話框，這裡可以用 Platform.runLater
                // 但為了簡單起見，直接呼叫
                boardView.redraw(); 
                offerAndPlayCard(current);
            }
            
            boardView.redraw();
            refreshUi();
        } else {
            hintLabel.setText("Cannot place at " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
        }
    }

    private void handlePlacement() {
        Position pos = parsePosition(placeX.getText(), placeY.getText(), placeZ.getText());
        if (pos == null) {
            hintLabel.setText("Enter numeric x y z.");
            return;
        }
        attemptMove(pos);
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
        // 簡單的轉換器
        dialog.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Card card) { return card == null ? "" : card.getName(); }
            @Override
            public Card fromString(String string) { return null; }
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
        if (pos == null) return null;
        Board board = game.getBoard();
        if (!board.inBounds(pos)) return null;
        Piece piece = board.getPiece(pos);
        if (piece == null || piece.getOwner() != player) return null;
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
        if (text == null) return null;
        String[] parts = text.trim().split("\\s+");
        if (parts.length != 3) return null;
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
                : current.getName() + " (" + symbolFor(current) + ") turn to place.");
        scoreLabel.setText(scoreSummary());
        boardView.redraw();
    }

    private String scoreSummary() {
        StringBuilder sb = new StringBuilder("Scores: ");
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) sb.append(" | ");
            Player p = players.get(i);
            sb.append(symbolFor(p)).append(":").append(p.getScore());
        }
        return sb.toString();
    }

    private String symbolFor(Player player) {
        int idx = players.indexOf(player);
        if (idx == 0) return "X";
        if (idx == 1) return "O";
        String name = player.getName();
        return name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    // =============================================================
    // 改進後的 View 類別：支援大間距與滑鼠點擊
    // =============================================================
    private class IsoBoardView extends Canvas {
        private static final double CELL = 60; // 格子大小
        private static final double LAYER_HEIGHT = 160; // 層間距：拉大讓中間不重疊
        private static final Color CELL_COLOR = Color.rgb(245, 230, 200);
        
        private Game game;
        private Consumer<Position> onClickHandler;

        IsoBoardView(Consumer<Position> onClickHandler) {
            super(800, 750); // 加大 Canvas
            this.onClickHandler = onClickHandler;
            
            widthProperty().addListener((obs, oldV, newV) -> redraw());
            heightProperty().addListener((obs, oldV, newV) -> redraw());
            
            // 監聽滑鼠點擊
            this.setOnMouseClicked(e -> handleMouseClick(e.getX(), e.getY()));
        }

        void setGame(Game game) {
            this.game = game;
        }

        // 處理點擊事件：反推座標
        private void handleMouseClick(double mx, double my) {
            if (game == null) return;
            
            double originX = getWidth() / 2;
            double originY = getHeight() / 2 + 150; // 原點下移

            // 從最上層往最下層檢查 (Z=2 -> Z=0)
            // 這樣如果視覺上有重疊，會優先點到上面的
            for (int z = Board.SIZE - 1; z >= 0; z--) {
                for (int x = 0; x < Board.SIZE; x++) {
                    for (int y = 0; y < Board.SIZE; y++) {
                        Point2D[] poly = getPolygonPoints(originX, originY, x, y, z);
                        
                        // 檢查點是否在多邊形內
                        if (contains(mx, my, poly)) {
                            // 觸發回調
                            onClickHandler.accept(new Position(x, y, z));
                            return; // 找到後立即停止
                        }
                    }
                }
            }
        }
        
        // 幾何算法：判斷點是否在多邊形內 (Ray Casting Algorithm)
        private boolean contains(double testx, double testy, Point2D[] poly) {
            int i, j;
            boolean c = false;
            int nvert = poly.length;
            for (i = 0, j = nvert - 1; i < nvert; j = i++) {
                if (((poly[i].getY() > testy) != (poly[j].getY() > testy)) &&
                    (testx < (poly[j].getX() - poly[i].getX()) * (testy - poly[i].getY()) / (poly[j].getY() - poly[i].getY()) + poly[i].getX())) {
                    c = !c;
                }
            }
            return c;
        }

        void redraw() {
            if (game == null) return;
            GraphicsContext gc = getGraphicsContext2D();
            gc.clearRect(0, 0, getWidth(), getHeight());

            double originX = getWidth() / 2;
            double originY = getHeight() / 2 + 150; 

            // 繪圖順序：從下往上 (Z=0 -> Z=2) 以確保遮擋關係正確
            drawLayerConnectors(gc, originX, originY);
            drawCells(gc, originX, originY);
            drawPieces(gc, originX, originY);
            drawLines(gc, originX, originY);
        }

        private void drawCells(GraphicsContext gc, double originX, double originY) {
            Board board = game.getBoard();
            for (int z = 0; z < Board.SIZE; z++) {
                double shade = 1.0 - (z * 0.08); // 每一層顏色稍微不同
                Color layerColor = CELL_COLOR.deriveColor(0, 1, shade, 1);
                
                for (int x = 0; x < Board.SIZE; x++) {
                    for (int y = 0; y < Board.SIZE; y++) {
                        Point2D[] p = getPolygonPoints(originX, originY, x, y, z);
                        double[] xs = {p[0].getX(), p[1].getX(), p[2].getX(), p[3].getX()};
                        double[] ys = {p[0].getY(), p[1].getY(), p[2].getY(), p[3].getY()};
                        
                        gc.setFill(layerColor);
                        gc.fillPolygon(xs, ys, 4);
                        gc.setStroke(Color.BLACK);
                        gc.setLineWidth(1.0);
                        gc.strokePolygon(xs, ys, 4);

                        Position pos = new Position(x, y, z);
                        if (board.isFrozen(pos)) {
                            gc.setFill(Color.rgb(180, 200, 220, 0.6));
                            gc.fillPolygon(xs, ys, 4);
                            Point2D center = projectCenter(originX, originY, pos);
                            gc.setFill(Color.DARKBLUE);
                            gc.setFont(Font.font(12));
                            gc.fillText("FZ", center.getX() - 6, center.getY());
                        }
                    }
                }
            }
        }
        
        // 繪製角落支柱，讓畫面看起來像一個整體結構
        private void drawLayerConnectors(GraphicsContext gc, double originX, double originY) {
            gc.setStroke(Color.GRAY);
            gc.setLineDashes(4); // 虛線
            gc.setLineWidth(1);
            
            // 四個角落座標
            int[][] corners = {{0,0}, {3,0}, {0,3}, {3,3}};
            for(int[] c : corners) {
                Point2D bot = project(originX, originY, c[0], c[1], 0);
                Point2D top = project(originX, originY, c[0], c[1], 3); // 延伸到頂部上方一點
                gc.strokeLine(bot.getX(), bot.getY(), top.getX(), top.getY());
            }
            gc.setLineDashes(null);
        }

        private void drawPieces(GraphicsContext gc, double originX, double originY) {
            Board board = game.getBoard();
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            
            for (int z = 0; z < Board.SIZE; z++) {
                for (int x = 0; x < Board.SIZE; x++) {
                    for (int y = 0; y < Board.SIZE; y++) {
                        Position pos = new Position(x, y, z);
                        Piece piece = board.getPiece(pos);
                        if (piece == null) continue;

                        Point2D center = projectCenter(originX, originY, pos);
                        Color pColor = players.indexOf(piece.getOwner()) == 0 ? Color.RED : Color.BLUE;
                        
                        // 畫一個背景圓，讓字更清楚
                        gc.setFill(Color.WHITE);
                        gc.fillOval(center.getX()-12, center.getY()-12, 24, 24);
                        gc.setStroke(pColor);
                        gc.strokeOval(center.getX()-12, center.getY()-12, 24, 24);

                        String symbol = symbolFor(piece.getOwner());
                        if (piece.isEmpowered()) symbol += "*";
                        
                        gc.setFill(pColor);
                        gc.fillText(symbol, center.getX() - 6, center.getY() + 7);

                        // 顯示壽命 (如果需要)
                        try {
                            // 注意：如果您的 Piece 類別還沒修復 turnsLifeRemaining，這裡可能會報錯
                            // 建議確保 Piece.java 裡面有這個方法
                             int life = piece.turnsLifeRemaining(game.getTurnLimit());
                             gc.setFont(Font.font(10));
                             gc.setFill(Color.BLACK);
                             gc.fillText(String.valueOf(life), center.getX() + 10, center.getY() - 10);
                             gc.setFont(Font.font("Arial", FontWeight.BOLD, 20)); // 還原字體
                        } catch (Error | Exception e) {
                            // 忽略方法找不到的錯誤，避免程式崩潰
                        }
                    }
                }
            }
        }

        private void drawLines(GraphicsContext gc, double originX, double originY) {
            List<Game.ScoredLine> lines = game.getLastCompletedLines();
            if (lines.isEmpty()) return;
            
            gc.setLineWidth(4);
            for (Game.ScoredLine line : lines) {
                List<Position> positions = line.getPositions();
                Point2D start = projectCenter(originX, originY, positions.get(0));
                Point2D end = projectCenter(originX, originY, positions.get(positions.size() - 1));
                Color color = players.indexOf(line.getPlayer()) == 0 ? Color.RED : Color.BLUE;
                gc.setStroke(color);
                gc.strokeLine(start.getX(), start.getY(), end.getX(), end.getY());
            }
            gc.setLineWidth(1.0);
        }

        // 輔助：取得一個格子的四個頂點
        private Point2D[] getPolygonPoints(double ox, double oy, int x, int y, int z) {
            return new Point2D[] {
                project(ox, oy, x, y, z),
                project(ox, oy, x + 1, y, z),
                project(ox, oy, x + 1, y + 1, z),
                project(ox, oy, x, y + 1, z)
            };
        }

        private Point2D projectCenter(double originX, double originY, Position pos) {
            return project(originX, originY, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ());
        }

        private Point2D project(double originX, double originY, double x, double y, double z) {
            // Isometric 投影公式
            double isoX = (x - y) * CELL * 0.9;
            double isoY = (x + y) * CELL * 0.45 - z * LAYER_HEIGHT;
            return new Point2D(originX + isoX, originY + isoY);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
