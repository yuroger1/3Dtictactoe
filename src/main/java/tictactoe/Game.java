package tictactoe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Random;

/**
 * Core game controller implementing the dynamic 3D Tic-Tac-Toe ruleset.
 */
public class Game {
    private final Board board = new Board();
    private final List<Player> players;
    private final Map<Player, Set<String>> scoredLines = new HashMap<>();
    private final int pieceCap;
    private final int turnLimit;
    private final Random rng;
    private int currentRound;

    public Game(List<Player> players, int pieceCap, int turnLimit, Random rng) {
        this.players = new ArrayList<>(players);
        this.pieceCap = pieceCap;
        this.turnLimit = turnLimit;
        this.rng = rng;
        this.currentRound = 1;
        for (Player player : players) {
            scoredLines.put(player, new HashSet<>());
        }
    }

    public Board getBoard() {
        return board;
    }

    public int getCurrentTurn() {
        return currentRound;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public int getTurnLimit() {
        return turnLimit;
    }

    public boolean placePiece(Player player, Position pos) {
        if (!board.inBounds(pos) || board.isFrozen(pos) || !board.isEmpty(pos)) {
            return false;
        }
        enforcePieceCap(player);
        Piece piece = new Piece(player, currentRound);
        board.setPiece(pos, piece);
        player.getPiecesOnBoard().addLast(piece);
        scoreNewLines(player);
        return true;
    }

    private void enforcePieceCap(Player player) {
        if (player.getPiecesOnBoard().size() < pieceCap) {
            return;
        }
        Piece oldest = player.getPiecesOnBoard().removeFirst();
        Position pos = oldest.getPosition();
        if (pos != null) {
            board.removePiece(pos);
        }
    }

    private void scoreNewLines(Player player) {
        Set<String> alreadyScored = scoredLines.get(player);
        for (List<Position> line : board.listAllLines()) {
            boolean fullByPlayer = true;
            StringBuilder keyBuilder = new StringBuilder();
            for (Position pos : line) {
                Piece piece = board.getPiece(pos);
                if (piece == null || piece.getOwner() != player) {
                    fullByPlayer = false;
                    break;
                }
                keyBuilder.append(pos.toString());
            }
            if (fullByPlayer) {
                String key = keyBuilder.toString();
                if (!alreadyScored.contains(key)) {
                    alreadyScored.add(key);
                    player.addScore(1);
                }
            }
        }
    }

    public void advanceRound() {
        currentRound++;
        board.tickFreezes();
        for (Player player : players) {
            for (Piece piece : player.getPiecesOnBoard()) {
                piece.incrementAge();
            }
        }
    }

    public boolean empoweredCapture(Player player, Piece piece, Position target) {
        if (piece == null || piece.getOwner() != player) {
            return false;
        }
        Piece removed = board.empoweredCapture(piece, target);
        if (removed != null) {
            removed.getOwner().getPiecesOnBoard().remove(removed);
            scoreNewLines(player);
            return true;
        }
        return false;
    }

    public List<Card> offerCards() {
        List<Card> deck = List.of(
                new EmpowerCard(),
                new LayerShiftUpCard(),
                new LayerShiftDownCard(),
                new TimeRewindCard(),
                new FreezeCard()
        );
        int first = rng.nextInt(deck.size());
        int second;
        do {
            second = rng.nextInt(deck.size());
        } while (second == first);
        List<Card> options = new ArrayList<>(2);
        options.add(deck.get(first));
        options.add(deck.get(second));
        return options;
    }

    public boolean useCard(Card card, Player player, ActionContext ctx) {
        int previousScore = player.getScore();
        card.apply(board, player, ctx);
        if (card instanceof LayerShiftDownCard || card instanceof LayerShiftUpCard
                || card instanceof EmpowerCard) {
            scoreNewLines(player);
        }
        return player.getScore() != previousScore;
    }

    public boolean isGameOver() {
        return currentRound > turnLimit;
    }
}
