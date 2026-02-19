package com.toguzkorgool.engine;

import com.toguzkorgool.exception.InvalidMoveException;
import com.toguzkorgool.model.GameState;
import com.toguzkorgool.model.enums.PlayerSide;

/**
 * Stateless game engine implementing Toguz Korgool rules.
 * All methods operate on a GameState passed as parameter.
 * Thread safety is the caller's responsibility (synchronized on GameState).
 */
public class ToguzKorgoolEngine {

    private static final int HOLES_PER_PLAYER = 9;
    private static final int TOTAL_HOLES = 18;
    private static final int WIN_SCORE = 82;
    private static final int DRAW_SCORE = 81;
    private static final int TUZ_TRIGGER = 3;

    /**
     * Validates and executes a move.
     *
     * @param state     the current game state
     * @param side      the side making the move
     * @param holeIndex the absolute hole index (0-17)
     * @return a description of the move for history
     */
    public static String makeMove(GameState state, PlayerSide side, int holeIndex) {
        validateMove(state, side, holeIndex);

        int[] holes = state.getHoles();
        int[] kazan = state.getKazan();
        int[] tuz = state.getTuz();
        int sideIdx = side.getIndex();
        int opponentIdx = side.opponent().getIndex();

        int stones = holes[holeIndex];
        holes[holeIndex] = 0;

        StringBuilder desc = new StringBuilder();
        desc.append(side == PlayerSide.WHITE ? "White" : "Black")
                .append(" moves from hole ").append(relativeHoleNumber(side, holeIndex));

        int currentPos;
        if (stones == 1) {
            // Special rule: if only 1 stone, move it to the next hole
            currentPos = (holeIndex + 1) % TOTAL_HOLES;
            addStonesToHole(holes, tuz, kazan, currentPos, 1);
        } else {
            // Leave 1 stone in the original hole, distribute the rest
            holes[holeIndex] = 1;
            stones--;
            currentPos = holeIndex;
            for (int i = 0; i < stones; i++) {
                currentPos = (currentPos + 1) % TOTAL_HOLES;
                addStonesToHole(holes, tuz, kazan, currentPos, 1);
            }
        }

        // Check tuz declaration (automatic)
        boolean tuzDeclared = checkAndDeclareTuz(state, side, currentPos);
        if (tuzDeclared) {
            desc.append(" [Tuz declared at hole ").append(currentPos).append("]");
        }

        // Check capture: last stone lands in opponent's hole and makes even count
        if (isOpponentHole(side, currentPos) && !isTuz(tuz, currentPos) && holes[currentPos] % 2 == 0) {
            int captured = holes[currentPos];
            holes[currentPos] = 0;
            kazan[sideIdx] += captured;
            desc.append(" [Captured ").append(captured).append(" stones]");
        }

        // Check atsyroo (opponent has no stones)
        checkAtsyroo(state);

        // Check win condition
        checkWinCondition(state);

        // Switch turn if game not over
        if (!state.isGameOver()) {
            state.setCurrentPlayer(side.opponent());
        }

        state.setMoveNumber(state.getMoveNumber() + 1);

        return desc.toString();
    }

    private static void validateMove(GameState state, PlayerSide side, int holeIndex) {
        if (state.isGameOver()) {
            throw new InvalidMoveException("Game is already over");
        }
        if (state.getCurrentPlayer() != side) {
            throw new InvalidMoveException("It is not your turn");
        }
        if (!isOwnHole(side, holeIndex)) {
            throw new InvalidMoveException("You can only move from your own holes");
        }
        if (state.getHoles()[holeIndex] == 0) {
            throw new InvalidMoveException("Cannot move from an empty hole");
        }
    }

    private static void addStonesToHole(int[] holes, int[] tuz, int[] kazan, int position, int count) {
        // If this position is a tuz, stones go to the tuz owner's kazan
        if (tuz[0] == position) {
            kazan[0] += count;
        } else if (tuz[1] == position) {
            kazan[1] += count;
        } else {
            holes[position] += count;
        }
    }

    /**
     * Check and automatically declare tuz if conditions are met:
     * - Last stone lands in opponent's hole making exactly 3
     * - The hole is not the 9th hole of the opponent (index 8 or 17)
     * - The player doesn't already have a tuz
     * - It's not symmetric to the opponent's tuz
     */
    private static boolean checkAndDeclareTuz(GameState state, PlayerSide side, int lastPos) {
        int[] holes = state.getHoles();
        int[] tuz = state.getTuz();
        int[] kazan = state.getKazan();
        int sideIdx = side.getIndex();

        if (!isOpponentHole(side, lastPos)) return false;
        if (holes[lastPos] != TUZ_TRIGGER) return false;
        if (tuz[sideIdx] != -1) return false; // already has a tuz

        // Cannot declare tuz on opponent's 9th hole (index 8 for white's side, 17 for black's side)
        int opponentNinthHole = (side.opponent() == PlayerSide.WHITE) ? 8 : 17;
        if (lastPos == opponentNinthHole) return false;

        // Cannot be symmetric to opponent's tuz
        int opponentIdx = side.opponent().getIndex();
        if (tuz[opponentIdx] != -1) {
            int myRelative = relativeIndex(side.opponent(), lastPos);
            int oppTuzRelative = relativeIndex(side, tuz[opponentIdx]);
            if (myRelative == oppTuzRelative) return false;
        }

        // Declare tuz
        tuz[sideIdx] = lastPos;
        kazan[sideIdx] += holes[lastPos];
        holes[lastPos] = 0;
        return true;
    }

    private static void checkAtsyroo(GameState state) {
        int[] holes = state.getHoles();
        int[] kazan = state.getKazan();

        // Check if either player has no stones in their holes
        for (int side = 0; side < 2; side++) {
            int start = side * HOLES_PER_PLAYER;
            boolean hasStones = false;
            for (int i = start; i < start + HOLES_PER_PLAYER; i++) {
                if (holes[i] > 0) {
                    hasStones = true;
                    break;
                }
            }
            if (!hasStones) {
                // The player with no stones: opponent collects all remaining
                int opponent = 1 - side;
                int oppStart = opponent * HOLES_PER_PLAYER;
                for (int i = oppStart; i < oppStart + HOLES_PER_PLAYER; i++) {
                    kazan[opponent] += holes[i];
                    holes[i] = 0;
                }
                state.setGameOver(true);
                determineWinner(state);
                return;
            }
        }
    }

    private static void checkWinCondition(GameState state) {
        if (state.isGameOver()) return;
        int[] kazan = state.getKazan();

        if (kazan[0] >= WIN_SCORE) {
            state.setGameOver(true);
            state.setWinner(String.valueOf(PlayerSide.WHITE.getIndex()));
        } else if (kazan[1] >= WIN_SCORE) {
            state.setGameOver(true);
            state.setWinner(String.valueOf(PlayerSide.BLACK.getIndex()));
        } else if (kazan[0] == DRAW_SCORE && kazan[1] == DRAW_SCORE) {
            state.setGameOver(true);
            state.setWinner("DRAW");
        }
    }

    private static void determineWinner(GameState state) {
        int[] kazan = state.getKazan();
        if (kazan[0] > kazan[1]) {
            state.setWinner(String.valueOf(PlayerSide.WHITE.getIndex()));
        } else if (kazan[1] > kazan[0]) {
            state.setWinner(String.valueOf(PlayerSide.BLACK.getIndex()));
        } else {
            state.setWinner("DRAW");
        }
    }

    private static boolean isOwnHole(PlayerSide side, int holeIndex) {
        int start = side.getIndex() * HOLES_PER_PLAYER;
        return holeIndex >= start && holeIndex < start + HOLES_PER_PLAYER;
    }

    private static boolean isOpponentHole(PlayerSide side, int holeIndex) {
        return !isOwnHole(side, holeIndex);
    }

    private static boolean isTuz(int[] tuz, int position) {
        return tuz[0] == position || tuz[1] == position;
    }

    /**
     * Returns the 0-based relative index within a player's side.
     */
    private static int relativeIndex(PlayerSide side, int absoluteIndex) {
        return absoluteIndex - (side.getIndex() * HOLES_PER_PLAYER);
    }

    /**
     * Returns the 1-based hole number for display purposes.
     */
    private static int relativeHoleNumber(PlayerSide side, int absoluteIndex) {
        return relativeIndex(side, absoluteIndex) + 1;
    }
}
