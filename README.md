# 3D Tic-Tac-Toe

A lightweight Java implementation of a dynamic 3×3×3 strategic Tic-Tac-Toe ruleset. The engine models layered movement, power cards, frozen cells, and FIFO removal when the per-player piece cap is exceeded.

## Features
- 3×3×3 board with configurable turn limit and per-player piece cap enforced via FIFO removal.
- Scoring that immediately awards a point for every new 3-in-a-row line formed by a player.
- Power cards: Empower, Layer Shift Up/Down, Time Rewind, and Freeze with action contexts.
- Freeze timers and empowered captures that respect adjacency and ownership.
- Simple console demo (`tictactoe.TicTacToe3DDemo`) showing sample turns and card usage.

## Running the demo
Compile and run the Java sources from the repository root:

```bash
javac -d out $(find src/main/java -name "*.java")
java -cp out tictactoe.TicTacToe3DDemo
```
