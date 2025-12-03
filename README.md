# 3D Tic-Tac-Toe

A lightweight Java implementation of a dynamic 3×3×3 strategic Tic-Tac-Toe ruleset. The engine models layered movement, power cards, frozen cells, and FIFO removal when the per-player piece cap is exceeded.

## Features
- 3×3×3 board with configurable turn limit and per-player piece cap enforced via FIFO removal.
- Scoring that immediately awards a point for every new 3-in-a-row line formed by a player.
- Power cards: Empower, Layer Shift Up/Down, Time Rewind, and Freeze with action contexts.
- Freeze timers and empowered captures that respect adjacency and ownership.
- Console interface (`tictactoe.TicTacToe3DDemo`) that renders layers, offers cards on the prescribed schedule, and lets players place, capture, or pass.

## Running the demo
Compile and run the Java sources from the repository root:

```bash
javac -d out $(find src/main/java -name "*.java")
java -cp out tictactoe.TicTacToe3DDemo
```

When prompted, enter player names. Each turn the interface:

- Shows the current turn number, board by layers (top layer printed first), and scores.
- Follows the card draw rhythm: player 2 draws on turns 2, 4, 6, 8, 10, ...; player 1 draws on turns 3, 5, 7, 9, ...; each draw presents two cards and the chosen card is played immediately that turn.
- Accepts actions:
  - `place`: supply `x y z` coordinates (0-indexed) to drop a piece.
  - `capture`: move an empowered piece onto an adjacent enemy cell.
  - `pass`: end your turn without taking an action.
  - `status`: reprint the current board and score overview.
