package tictactoe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Player model containing their name, score, and the FIFO queue of pieces
 * currently on the board.
 */
public class Player {
    private final String name;
    private int score;
    private final Deque<Piece> piecesOnBoard = new ArrayDeque<>();
    private final List<Card> hand = new ArrayList<>();

    public Player(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int delta) {
        score += delta;
    }

    public Deque<Piece> getPiecesOnBoard() {
        return piecesOnBoard;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void addToHand(Card card) {
        hand.add(card);
    }

    public Card removeFromHand(int index) {
        return hand.remove(index);
    }
}
