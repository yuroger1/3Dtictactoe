package tictactoe;

/**
 * Represents a single piece placed by a player. The {@code placementIndex}
 * field is used to enforce FIFO removal when the piece cap is exceeded.
 */
public class Piece {
    private final Player owner;
    private int placementIndex;
    private boolean empowered;
    private int ageTurns;
    private Position position;

    public Piece(Player owner, int placementIndex) {
        this.owner = owner;
        this.placementIndex = placementIndex;
    }

    public Player getOwner() {
        return owner;
    }

    public int getPlacementIndex() {
        return placementIndex;
    }

    public void setPlacementIndex(int placementIndex) {
        this.placementIndex = placementIndex;
    }

    public boolean isEmpowered() {
        return empowered;
    }

    public void setEmpowered(boolean empowered) {
        this.empowered = empowered;
    }

    public int getAgeTurns() {
        return ageTurns;
    }

    public void incrementAge() {
        ageTurns++;
    }

    public void resetAge() {
        ageTurns = 0;
    }

    public int turnsLifeRemaining(int turnLimit) {
        return Math.max(0, turnLimit - ageTurns);
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }
}
