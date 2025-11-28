package tictactoe;

/**
 * Base type for all power cards. Cards mutate the board or a specific piece
 * based on the supplied {@link ActionContext}.
 */
public abstract class Card {
    private final String name;

    protected Card(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract void apply(Board board, Player player, ActionContext ctx);
}

class ActionContext {
    public int layer;
    public Position pos;
    public Position target;
    public Piece piece;

    public ActionContext(int layer) {
        this.layer = layer;
    }

    public ActionContext(Piece piece) {
        this.piece = piece;
    }

    public ActionContext(Position pos) {
        this.pos = pos;
    }

    public ActionContext(Piece piece, Position target) {
        this.piece = piece;
        this.target = target;
    }
}

class EmpowerCard extends Card {
    public EmpowerCard() {
        super("Empower");
    }

    @Override
    public void apply(Board board, Player player, ActionContext ctx) {
        if (ctx.piece == null) {
            return;
        }
        ctx.piece.setEmpowered(true);
    }
}

class LayerShiftUpCard extends Card {
    public LayerShiftUpCard() {
        super("Layer Shift Up");
    }

    @Override
    public void apply(Board board, Player player, ActionContext ctx) {
        board.shiftLayerUp(ctx.layer);
    }
}

class LayerShiftDownCard extends Card {
    public LayerShiftDownCard() {
        super("Layer Shift Down");
    }

    @Override
    public void apply(Board board, Player player, ActionContext ctx) {
        board.shiftLayerDown(ctx.layer);
    }
}

class TimeRewindCard extends Card {
    public TimeRewindCard() {
        super("Time Rewind");
    }

    @Override
    public void apply(Board board, Player player, ActionContext ctx) {
        if (ctx.piece == null) {
            return;
        }
        ctx.piece.resetAge();
        player.getPiecesOnBoard().remove(ctx.piece);
        player.getPiecesOnBoard().addLast(ctx.piece);
    }
}

class FreezeCard extends Card {
    public FreezeCard() {
        super("Freeze");
    }

    @Override
    public void apply(Board board, Player player, ActionContext ctx) {
        if (ctx.pos == null) {
            return;
        }
        board.freezeCell(ctx.pos, 2);
    }
}
