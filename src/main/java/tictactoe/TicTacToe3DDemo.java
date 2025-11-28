package tictactoe;

import java.util.List;
import java.util.Random;

/**
 * Simple console driver that exercises the 3x3x3 strategic Tic-Tac-Toe rules.
 */
public class TicTacToe3DDemo {
    public static void main(String[] args) {
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");
        Game game = new Game(List.of(alice, bob), 5, 20, new Random());

        // Sample opening
        game.placePiece(alice, new Position(0, 0, 0));
        game.placePiece(bob, new Position(1, 0, 0));
        game.advanceTurn();

        // Alice freezes a critical spot
        Card freeze = new FreezeCard();
        freeze.apply(game.getBoard(), alice, new ActionContext(new Position(1, 1, 0)));
        game.advanceTurn();

        // Bob gains an Empower card and uses it to capture
        Card empower = new EmpowerCard();
        Piece bobPiece = game.getBoard().getPiece(new Position(1, 0, 0));
        empower.apply(game.getBoard(), bob, new ActionContext(bobPiece, new Position(0, 0, 0)));
        game.empoweredCapture(bob, bobPiece, new Position(0, 0, 0));
        game.advanceTurn();

        // Alice shifts a layer upward to complete a line
        game.placePiece(alice, new Position(0, 1, 0));
        game.placePiece(alice, new Position(0, 2, 0));
        Card shiftUp = new LayerShiftUpCard();
        shiftUp.apply(game.getBoard(), alice, new ActionContext(0));
        game.advanceTurn();

        System.out.println("Alice score: " + alice.getScore());
        System.out.println("Bob score: " + bob.getScore());
    }
}
