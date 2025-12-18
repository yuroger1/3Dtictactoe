# 3D Tic-Tac-Toe

A lightweight Java implementation of a dynamic 3×3×3 strategic Tic-Tac-Toe ruleset. The engine models layered movement, power cards, frozen cells, and FIFO removal when the per-player piece cap is exceeded.

## Features
- 3×3×3 board with configurable turn limit and per-player piece cap enforced via FIFO removal.
- Scoring that immediately awards a point for every new 3-in-a-row line formed by a player.
- Power cards: Empower, Layer Shift Up/Down, Time Rewind, and Freeze with action contexts.
- Freeze timers and empowered captures that respect adjacency and ownership.
- Console interface (`tictactoe.TicTacToe3DDemo`) that renders layers, offers cards starting round 3 (every two rounds), and lets players place, capture, or end their turn.
- Swing GUI (`tictactoe.TicTacToe3DGui`) that presents stacked 3×3 boards with X/O styling, card prompts, and capture support on card rounds.
- JavaFX GUI (`tictactoe.TicTacToe3DJavaFX`) that draws an isometric cube with stacked planes, highlights completed lines, and annotates each piece with remaining life.

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

## Running the GUI

Compile the sources as above, then run the Swing interface:

```bash
javac -d out $(find src/main/java -name "*.java")
java -cp out tictactoe.TicTacToe3DGui
```

Enter player names when prompted. Click a cell to place a piece each round, choose one of two cards on draw rounds (starting at round 3 and every other round), optionally capture with empowered pieces in card rounds, then end the turn so the next player can place.

## Running the JavaFX GUI

You need a JavaFX runtime on your machine. If `$PATH_TO_FX` points to the JavaFX SDK directory, compile and run like:

```bash
javac --module-path "$PATH_TO_FX/lib" --add-modules javafx.controls,javafx.graphics -d out $(find src/main/java -name "*.java")
java --module-path "$PATH_TO_FX/lib" --add-modules javafx.controls,javafx.graphics -cp out tictactoe.TicTacToe3DJavaFX
```

The JavaFX view renders the transparent cube in isometric perspective. Enter coordinates to place pieces, choose cards on draw rounds starting at round 3 (every other round), and watch completed lines highlight with your color. Each piece shows its remaining life in turns.
