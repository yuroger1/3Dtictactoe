package tictactoe;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Player model containing their name, score, and the FIFO queue of pieces
 * currently on the board.
 */
public class Player {
    private final String name;
    private int score;
    private final Deque<Piece> piecesOnBoard = new ArrayDeque<>();

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
}
