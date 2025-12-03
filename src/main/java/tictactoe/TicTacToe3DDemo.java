package tictactoe;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;

/**
 * Console-based interface for the 3×3×3 strategic Tic-Tac-Toe ruleset. It
 * renders the board by layers, lets users enter coordinates, pick cards when
 * offered, and shows the current game state each turn.
 */
public class TicTacToe3DDemo {
    private static final int PIECE_CAP = 5;
    private static final int TURN_LIMIT = 30;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter name for Player 1: ");
        Player p1 = new Player(scanner.nextLine().trim());
        System.out.print("Enter name for Player 2: ");
        Player p2 = new Player(scanner.nextLine().trim());

        Game game = new Game(List.of(p1, p2), PIECE_CAP, TURN_LIMIT, new Random());

        while (!game.isGameOver()) {
            Player current = currentPlayer(game);
            System.out.println("\n=== Turn " + game.getCurrentTurn() + " / " + game.getTurnLimit() + " ===");
            maybeOfferCard(scanner, game, current);
            printStatus(game);
            boolean turnComplete = false;
            while (!turnComplete) {
                System.out.print("Action (place/capture/pass/status): ");
                String action = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
                switch (action) {
                    case "place":
                        turnComplete = handlePlacement(scanner, game, current);
                        break;
                    case "capture":
                        turnComplete = handleCapture(scanner, game, current);
                        break;
                    case "status":
                        printStatus(game);
                        break;
                    case "pass":
                        turnComplete = true;
                        break;
                    default:
                        System.out.println("Unknown action. Try again.");
                        break;
                }
            }
            game.advanceTurn();
        }

        System.out.println("\nGame over! Final scores:");
        for (Player player : game.getPlayers()) {
            System.out.println(player.getName() + ": " + player.getScore());
        }
    }

    private static Player currentPlayer(Game game) {
        List<Player> players = game.getPlayers();
        return players.get((game.getCurrentTurn() - 1) % players.size());
    }

    private static boolean handlePlacement(Scanner scanner, Game game, Player current) {
        Position pos = readPosition(scanner, "Enter x y z (0-indexed): ");
        if (pos == null) {
            return false;
        }
        if (game.placePiece(current, pos)) {
            return true;
        }
        System.out.println("Cannot place there (occupied, frozen, or out of bounds).");
        return false;
    }

    private static ActionContext buildContextForCard(Scanner scanner, Game game, Player current, Card card) {
        if (card instanceof LayerShiftUpCard) {
            Integer layer = readLayer(scanner, "Layer to shift up (0 = bottom, 2 = top): ");
            return layer == null ? new ActionContext(0) : new ActionContext(layer);
        }
        if (card instanceof LayerShiftDownCard) {
            Integer layer = readLayer(scanner, "Layer to shift down (0 = bottom, 2 = top): ");
            return layer == null ? new ActionContext(0) : new ActionContext(layer);
        }
        if (card instanceof TimeRewindCard) {
            Piece piece = pickOwnPiece(scanner, game, current, "Enter coordinates of your piece to rewind (x y z): ");
            return new ActionContext(piece);
        }
        if (card instanceof FreezeCard) {
            Position pos = readPosition(scanner, "Enter empty cell to freeze (x y z): ");
            return new ActionContext(pos);
        }
        if (card instanceof EmpowerCard) {
            Piece piece = pickOwnPiece(scanner, game, current, "Enter coordinates of your piece to empower (x y z): ");
            return new ActionContext(piece);
        }
        return new ActionContext(0);
    }

    private static boolean handleCapture(Scanner scanner, Game game, Player current) {
        Piece piece = pickOwnPiece(scanner, game, current, "Enter your empowered piece coordinates (x y z): ");
        if (piece == null || !piece.isEmpowered()) {
            System.out.println("That piece is not empowered.");
            return false;
        }
        Position target = readPosition(scanner, "Enter adjacent enemy cell to capture (x y z): ");
        if (target == null) {
            return false;
        }
        if (game.empoweredCapture(current, piece, target)) {
            return true;
        }
        System.out.println("Invalid capture target.");
        return false;
    }

    private static Piece pickOwnPiece(Scanner scanner, Game game, Player player, String prompt) {
        Position pos = readPosition(scanner, prompt);
        if (pos == null) {
            return null;
        }
        Piece piece = game.getBoard().inBounds(pos) ? game.getBoard().getPiece(pos) : null;
        if (piece == null || piece.getOwner() != player) {
            System.out.println("No such piece.");
            return null;
        }
        return piece;
    }

    private static void maybeOfferCard(Scanner scanner, Game game, Player current) {
        if (!shouldOfferCard(game, current)) {
            return;
        }
        List<Card> offers = game.offerCards();
        System.out.println("Card offer: choose one to play immediately this turn");
        for (int i = 0; i < offers.size(); i++) {
            System.out.println("  [" + i + "] " + offers.get(i).getName());
        }
        Integer idx = null;
        while (idx == null) {
            System.out.print("Select: ");
            String line = scanner.nextLine().trim();
            try {
                idx = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Enter 0 or 1 to choose a card.");
                continue;
            }
            if (idx < 0 || idx >= offers.size()) {
                System.out.println("Selection out of range.");
                idx = null;
            }
        }

        Card card = offers.get(idx);
        ActionContext ctx = buildContextForCard(scanner, game, current, card);
        game.useCard(card, current, ctx);
        if (card instanceof EmpowerCard) {
            System.out.println("Piece empowered. Use 'capture' to move it onto an adjacent enemy.");
        }
    }

    private static boolean shouldOfferCard(Game game, Player current) {
        List<Player> players = game.getPlayers();
        int playerIndex = players.indexOf(current);
        if (playerIndex == 1) { // second player draws starting turn 2 (even turns)
            return game.getCurrentTurn() % 2 == 0;
        }
        if (playerIndex == 0) { // first player draws starting turn 3 (odd turns from 3)
            return game.getCurrentTurn() >= 3 && game.getCurrentTurn() % 2 == 1;
        }
        return false;
    }

    private static Position readPosition(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String line = scanner.nextLine().trim();
        if (line.isEmpty()) {
            return null;
        }
        String[] parts = line.split("\\s+");
        if (parts.length != 3) {
            System.out.println("Need three integers.");
            return null;
        }
        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            return new Position(x, y, z);
        } catch (NumberFormatException e) {
            System.out.println("Invalid coordinates.");
            return null;
        }
    }

    private static Integer readLayer(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String line = scanner.nextLine().trim();
        if (line.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            System.out.println("Invalid layer.");
            return null;
        }
    }

    private static void printStatus(Game game) {
        Board board = game.getBoard();
        for (int z = Board.SIZE - 1; z >= 0; z--) {
            System.out.println("Layer z=" + z + ":");
            for (int y = 0; y < Board.SIZE; y++) {
                StringBuilder row = new StringBuilder();
                for (int x = 0; x < Board.SIZE; x++) {
                    Position pos = new Position(x, y, z);
                    Piece piece = board.getPiece(pos);
                    if (piece != null) {
                        String mark = piece.getOwner().getName().isEmpty()
                                ? "?"
                                : piece.getOwner().getName().substring(0, 1).toUpperCase(Locale.ROOT);
                        if (piece.isEmpowered()) {
                            mark += "*";
                        }
                        row.append(String.format("%3s", mark));
                    } else if (board.isFrozen(pos)) {
                        row.append(String.format("%3s", "F" + board.frozenTurnsRemaining(pos)));
                    } else {
                        row.append("  .");
                    }
                }
                System.out.println(row);
            }
            System.out.println();
        }

        System.out.println("Scores:");
        for (Player player : game.getPlayers()) {
            System.out.println("  " + player.getName() + ": " + player.getScore());
        }
    }
}
