package tictactoe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

/**
 * Swing-based 3D Tic-Tac-Toe interface that mirrors the console ruleset but
 * renders stacked layers for a visual play experience.
 */
public class TicTacToe3DGui extends JFrame {
    private static final int PIECE_CAP = 5;
    private static final int TURN_LIMIT = 30;
    private static final Color BOARD_COLOR = new Color(245, 230, 200);
    private static final Font CELL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 26);

    private final Game game;
    private final List<Player> players;
    private final CellButton[][][] cells = new CellButton[Board.SIZE][Board.SIZE][Board.SIZE];

    private int currentPlayerIdx = 0;
    private boolean placedThisTurn = false;

    private final JLabel roundLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JLabel scoreLabel = new JLabel();

    private TicTacToe3DGui(List<Player> players) {
        super("3D Tic-Tac-Toe");
        this.players = players;
        this.game = new Game(players, PIECE_CAP, TURN_LIMIT, new Random());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBoard(), BorderLayout.CENTER);
        add(buildControls(), BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
        refreshBoard();
        refreshStatus();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        roundLabel.setAlignmentX(CENTER_ALIGNMENT);
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);
        scoreLabel.setAlignmentX(CENTER_ALIGNMENT);
        header.add(Box.createRigidArea(new Dimension(0, 6)));
        header.add(roundLabel);
        header.add(Box.createRigidArea(new Dimension(0, 4)));
        header.add(statusLabel);
        header.add(Box.createRigidArea(new Dimension(0, 4)));
        header.add(scoreLabel);
        header.add(Box.createRigidArea(new Dimension(0, 6)));
        return header;
    }

    private JPanel buildBoard() {
        JPanel layers = new JPanel();
        layers.setLayout(new BoxLayout(layers, BoxLayout.Y_AXIS));
        for (int z = Board.SIZE - 1; z >= 0; z--) {
            JPanel layerPanel = new JPanel(new GridLayout(Board.SIZE, Board.SIZE));
            layerPanel.setBorder(BorderFactory.createTitledBorder("Layer z=" + z));
            for (int y = 0; y < Board.SIZE; y++) {
                for (int x = 0; x < Board.SIZE; x++) {
                    Position pos = new Position(x, y, z);
                    CellButton btn = new CellButton(pos);
                    btn.addActionListener(e -> handleCellClick(btn.getPosition()));
                    btn.setPreferredSize(new Dimension(70, 70));
                    btn.setBackground(BOARD_COLOR);
                    btn.setOpaque(true);
                    btn.setFont(CELL_FONT);
                    btn.setBorder(new LineBorder(Color.BLACK));
                    cells[x][y][z] = btn;
                    layerPanel.add(btn);
                }
            }
            layers.add(layerPanel);
            layers.add(Box.createRigidArea(new Dimension(0, 6)));
        }
        return layers;
    }

    private JPanel buildControls() {
        JPanel controls = new JPanel();
        JButton captureButton = new JButton("Capture (card rounds)");
        JButton endTurnButton = new JButton("End Turn");

        captureButton.addActionListener(e -> attemptCapture());
        endTurnButton.addActionListener(e -> advanceTurn());

        controls.add(captureButton);
        controls.add(endTurnButton);
        return controls;
    }

    private void handleCellClick(Position pos) {
        if (game.isGameOver()) {
            return;
        }
        if (placedThisTurn) {
            JOptionPane.showMessageDialog(this, "You've already placed this round.");
            return;
        }
        Player current = players.get(currentPlayerIdx);
        if (game.placePiece(current, pos)) {
            placedThisTurn = true;
            refreshBoard();
            refreshStatus();
            if (game.shouldOfferCard()) {
                handleCardOffer(current);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Cannot place there (occupied, frozen, or out of bounds).");
        }
    }

    private void handleCardOffer(Player player) {
        List<Card> offers = game.offerCards();
        Object[] optionLabels = offers.stream().map(Card::getName).toArray();
        int choice = JOptionPane.showOptionDialog(
                this,
                "Choose a card to play immediately",
                "Card Offer",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                optionLabels,
                optionLabels[0]);
        if (choice < 0 || choice >= offers.size()) {
            JOptionPane.showMessageDialog(this, "No card selected; skipping.");
            return;
        }

        Card card = offers.get(choice);
        ActionContext ctx = buildContext(card, player);
        game.useCard(card, player, ctx);
        refreshBoard();
        refreshStatus();
        if (card instanceof EmpowerCard) {
            JOptionPane.showMessageDialog(this, "Piece empowered. Use Capture to move it onto an adjacent enemy.");
        }
    }

    private ActionContext buildContext(Card card, Player current) {
        if (card instanceof LayerShiftUpCard) {
            Integer layer = promptLayer("Layer to shift up (0 = bottom, 2 = top):");
            return new ActionContext(layer == null ? 0 : layer);
        }
        if (card instanceof LayerShiftDownCard) {
            Integer layer = promptLayer("Layer to shift down (0 = bottom, 2 = top):");
            return new ActionContext(layer == null ? 0 : layer);
        }
        if (card instanceof FreezeCard) {
            Position pos = promptPosition("Enter empty cell to freeze as x y z:");
            return new ActionContext(pos);
        }
        if (card instanceof TimeRewindCard) {
            Piece piece = promptOwnPiece(current, "Enter your piece to rewind as x y z:");
            return new ActionContext(piece);
        }
        if (card instanceof EmpowerCard) {
            Piece piece = promptOwnPiece(current, "Enter your piece to empower as x y z:");
            return new ActionContext(piece);
        }
        return new ActionContext(0);
    }

    private void attemptCapture() {
        if (!game.shouldOfferCard()) {
            JOptionPane.showMessageDialog(this, "Captures are only available in card-draw rounds.");
            return;
        }
        if (!placedThisTurn) {
            JOptionPane.showMessageDialog(this, "Place a piece before capturing.");
            return;
        }
        Player current = players.get(currentPlayerIdx);
        Piece piece = promptOwnPiece(current, "Enter your empowered piece (x y z):");
        if (piece == null || !piece.isEmpowered()) {
            JOptionPane.showMessageDialog(this, "That piece is not empowered.");
            return;
        }
        Position target = promptPosition("Enter adjacent enemy cell to capture (x y z):");
        if (target == null) {
            return;
        }
        if (game.empoweredCapture(current, piece, target)) {
            refreshBoard();
            refreshStatus();
        } else {
            JOptionPane.showMessageDialog(this, "Invalid capture target.");
        }
    }

    private void advanceTurn() {
        if (!placedThisTurn) {
            JOptionPane.showMessageDialog(this, "You must place a piece before ending your turn.");
            return;
        }
        currentPlayerIdx = (currentPlayerIdx + 1) % players.size();
        placedThisTurn = false;
        if (currentPlayerIdx == 0) {
            game.advanceRound();
        }
        if (game.isGameOver()) {
            showGameOver();
        }
        refreshBoard();
        refreshStatus();
    }

    private void refreshBoard() {
        Board board = game.getBoard();
        for (int x = 0; x < Board.SIZE; x++) {
            for (int y = 0; y < Board.SIZE; y++) {
                for (int z = 0; z < Board.SIZE; z++) {
                    Position pos = new Position(x, y, z);
                    CellButton btn = cells[x][y][z];
                    Piece piece = board.getPiece(pos);
                    if (piece != null) {
                        String symbol = getPlayerSymbol(piece.getOwner());
                        if (piece.isEmpowered()) {
                            symbol += "*";
                        }
                        btn.setText(symbol);
                        btn.setForeground(symbol.startsWith("X") ? Color.RED : Color.BLUE);
                    } else if (board.isFrozen(pos)) {
                        btn.setText("F" + board.frozenTurnsRemaining(pos));
                        btn.setForeground(Color.GRAY.darker());
                    } else {
                        btn.setText("");
                        btn.setForeground(Color.BLACK);
                    }
                }
            }
        }
    }

    private void refreshStatus() {
        Player current = players.get(currentPlayerIdx);
        roundLabel.setText("Round " + game.getCurrentRound() + " of " + game.getTurnLimit());
        String actionHint = placedThisTurn
                ? "Optional capture (card rounds) or end turn."
                : "Click a cell to place.";
        statusLabel.setText("Current: " + current.getName() + " — " + actionHint);
        scoreLabel.setText(scoreSummary());
    }

    private String scoreSummary() {
        StringBuilder sb = new StringBuilder("Scores: ");
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            Player p = players.get(i);
            sb.append(getPlayerSymbol(p)).append("=").append(p.getScore());
        }
        return sb.toString();
    }

    private void showGameOver() {
        Player winner = players.stream().max((a, b) -> Integer.compare(a.getScore(), b.getScore())).orElse(null);
        String message;
        if (winner == null || players.stream().allMatch(p -> p.getScore() == players.get(0).getScore())) {
            message = "Game over! It's a tie.";
        } else {
            message = "Game over! Winner: " + winner.getName();
        }
        JOptionPane.showMessageDialog(this, message + "\n" + scoreSummary());
    }

    private Integer promptLayer(String prompt) {
        String input = JOptionPane.showInputDialog(this, prompt);
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid layer.");
            return null;
        }
    }

    private Position promptPosition(String prompt) {
        String input = JOptionPane.showInputDialog(this, prompt);
        if (input == null || input.isBlank()) {
            return null;
        }
        String[] parts = input.trim().split("\\s+");
        if (parts.length != 3) {
            JOptionPane.showMessageDialog(this, "Please enter x y z.");
            return null;
        }
        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            return new Position(x, y, z);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid coordinates.");
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
            JOptionPane.showMessageDialog(this, "Out of bounds.");
            return null;
        }
        Piece piece = board.getPiece(pos);
        if (piece == null || piece.getOwner() != player) {
            JOptionPane.showMessageDialog(this, "No such piece.");
            return null;
        }
        return piece;
    }

    private String getPlayerSymbol(Player player) {
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

    private static List<Player> promptPlayers() {
        String p1Name = JOptionPane.showInputDialog(null, "Enter name for Player 1 (X):", "Player 1", JOptionPane.PLAIN_MESSAGE);
        if (p1Name == null || p1Name.isBlank()) {
            p1Name = "Player 1";
        }
        String p2Name = JOptionPane.showInputDialog(null, "Enter name for Player 2 (O):", "Player 2", JOptionPane.PLAIN_MESSAGE);
        if (p2Name == null || p2Name.isBlank()) {
            p2Name = "Player 2";
        }
        return List.of(new Player(p1Name.trim()), new Player(p2Name.trim()));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            List<Player> players = promptPlayers();
            TicTacToe3DGui frame = new TicTacToe3DGui(players);
            frame.setVisible(true);
        });
    }

    private static class CellButton extends JButton {
        private final Position position;

        CellButton(Position position) {
            this.position = position;
        }

        Position getPosition() {
            return position;
        }
    }
}
