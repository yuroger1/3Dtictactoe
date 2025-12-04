# 3D Tic-Tac-Toe

A lightweight Java implementation of a dynamic 3×3×3 strategic Tic-Tac-Toe ruleset. The engine models layered movement, power cards, frozen cells, and FIFO removal when the per-player piece cap is exceeded.

## Features
- 3×3×3 board with configurable turn limit and per-player piece cap enforced via FIFO removal.
- Scoring that immediately awards a point for every new 3-in-a-row line formed by a player.
- Power cards: Empower, Layer Shift Up/Down, Time Rewind, and Freeze with action contexts.
- Freeze timers and empowered captures that respect adjacency and ownership.
- Console interface (`tictactoe.TicTacToe3DDemo`) that renders layers, offers cards starting round 3 (every two rounds), and lets players place, capture, or end their turn.

## Running the demo
Compile and run the Java sources from the repository root:

```bash
javac -d out $(find src/main/java -name "*.java")
java -cp out tictactoe.TicTacToe3DDemo
```

When prompted, enter player names. Each round runs both players in order:

- Shows the current round number, board by layers (top layer printed first), and scores.
- Each round both players must place exactly one piece. Card draws start on round 3 and then occur every other round (3, 5, 7, ...); in those rounds a successful placement triggers two distinct random card offers and the player must play one immediately. In non-card rounds, players only place pieces.
- Accepts actions while taking a turn:
  - `place`: supply `x y z` coordinates (0-indexed) to drop a piece (mandatory once per round).
  - `capture`: move an empowered piece onto an adjacent enemy cell (optional after placing, only in card-draw rounds).
  - `status`: reprint the current board and score overview.
  - `end`: finish your actions after placing.
