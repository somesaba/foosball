package com.saba.foosball.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 
 * @author saba
 * 
 *         A discrete representation of an NxN foosball table
 * 
 *         This game state updates are thread-safe to a degree (enough for our purposes)
 */
public class GameState {
    // Static Game State Properties
    // The number of discrete points in the x-axis, perpendicular to the players
    private final int xLength;
    // The number of discrete points in the y-axis, parallel to the players
    private final int yLength;
    // A Map of the number of the players each row of players has, row 0 is your goalie
    private final Map<Integer, Integer> rowToPlayerCountMap;
    // A Map of the distance between players each row of players, row 0 is your goalie
    private final Map<Integer, Integer> rowToPlayerDistanceMap;
    // A Map of the fixed x-position of the players for each row, row 0 is your goalie
    private final Map<Integer, Integer> rowToXPositionMap;

    // Dynamic Game State Properties
    private volatile int prevBallXPosition;
    private volatile int prevBallYPosition;
    private volatile int ballXPosition;
    private volatile int ballYPosition;
    private final ConcurrentMap<Integer, Integer> rowToYPositionMap;
    private final ConcurrentMap<Integer, PlayerAngle> rowToAngleMap;

    private volatile Player playerThatScored = null;

    public GameState(int maxX, int maxY, Map<Integer, Integer> rowToPlayerCountMap, Map<Integer, Integer> rowToPlayerDistanceMap,
            Map<Integer, Integer> rowToXPositionMap) throws IllegalArgumentException {
        this.xLength = maxX;
        this.yLength = maxY;
        this.rowToPlayerCountMap = rowToPlayerCountMap;
        this.rowToPlayerDistanceMap = rowToPlayerDistanceMap;
        this.rowToXPositionMap = rowToXPositionMap;
        this.rowToYPositionMap = new ConcurrentHashMap<Integer, Integer>();
        this.rowToAngleMap = new ConcurrentHashMap<Integer, PlayerAngle>();

        // Make sure these configurations pass basic checks...
        if (maxX < 1 || maxY < 1) {
            throw new IllegalArgumentException("maxX and maxY must >= 1");
        }

        // row maps cannot be null
        if (rowToPlayerCountMap == null || rowToPlayerDistanceMap == null || rowToYPositionMap == null) {
            throw new IllegalArgumentException("row maps cannot be null");
        }

        // row maps must have same num of rows
        if (rowToPlayerCountMap.size() != rowToXPositionMap.size() || rowToPlayerCountMap.size() != rowToPlayerDistanceMap.size()) {
            throw new IllegalArgumentException("rowToPlayerCountMap.size() must equal rowToXPositionMap.size() and rowToPlayerDistanceMap.size()");
        }

        // x positions cannot be > maxX
        for (int x : rowToXPositionMap.values()) {
            if (x >= maxX || x < 0) {
                throw new IllegalArgumentException(x + "is >= maxX or < 0");
            }
        }
        // y positions cannot be > maxY
        for (int row : rowToPlayerDistanceMap.keySet()) {
            int positionOfLastPlayer = rowToPlayerCountMap.get(row) * rowToPlayerDistanceMap.get(row);
            if (positionOfLastPlayer >= maxY) {
                throw new IllegalArgumentException("row " + row + ": distanceBetweenPlayers*numOfPlayers exceeds maxY");
            }
        }
        // Init ball to the center
        this.ballXPosition = maxX / 2;
        this.ballYPosition = maxY / 2;

        // Init Players down and in the middle
        for (int row : rowToXPositionMap.keySet()) {
            rowToAngleMap.put(row, PlayerAngle.VERTICAL);
            int middleY = (maxY - rowToPlayerDistanceMap.get(row) * (rowToPlayerCountMap.get(row) - 1)) / 2;
            rowToYPositionMap.put(row, middleY);
        }
    }

    /**
     * 
     * @return The player that scored or null if no one has scored in this round
     */
    public Player getPlayerThatScored() {
        return playerThatScored;
    }

    /**
     * 
     * @param player
     *            set player that scored or null if the round has restarted
     */
    public void setScoringPlayer(Player player) {
        playerThatScored = player;
    }

    public void restartRound() {
        playerThatScored = null;
    }

    /**
     * Updates the ball's (x,y) position of the table.
     * 
     * @param x
     * @param y
     */
    public void updateBallPosition(int x, int y) throws IllegalArgumentException {
        if (x >= xLength || x < 0) {
            throw new IllegalArgumentException("x must be <= maxX and >= 0");
        } else {
            prevBallXPosition = ballXPosition;
            ballXPosition = x;
        }
        if (y >= yLength || y < 0) {
            throw new IllegalArgumentException("y must be <= mayY and >= 0");
        } else {
            prevBallYPosition = ballYPosition;
            ballYPosition = y;
        }
    }

    public void updateRowYPosition(int row, int y) throws IllegalArgumentException {
        if (y >= yLength || y < 0) {
            throw new IllegalArgumentException("x must be < maxX and >= 0");
        }
        if (rowToYPositionMap.containsKey(row)) {
            // Make sure this position is plausible
            int positionOfLastPlayer = rowToPlayerCountMap.get(row) * rowToPlayerDistanceMap.get(row);
            if (positionOfLastPlayer >= yLength) {
                throw new IllegalArgumentException("row " + row + ": distanceBetweenPlayers*numOfPlayers exceeds maxX");
            }
            rowToYPositionMap.put(row, y);
        } else {
            throw new IllegalArgumentException("row " + row + " does not exist");
        }
    }

    public void updateRowAngle(int row, PlayerAngle angle) throws IllegalArgumentException {
        if (rowToAngleMap.containsKey(row)) {
            rowToAngleMap.put(row, angle);
        } else {
            throw new IllegalArgumentException("row " + row + " does not exist");
        }
    }

    public int getBallXPosition() {
        return ballXPosition;
    }

    public int getBallYPosition() {
        return ballYPosition;
    }

    public int getNumOfRows() {
        return rowToAngleMap.size();
    }

    public int getNumbersOfPlayersForRow(int row) {
        if (rowToPlayerCountMap.containsKey(row)) {
            return rowToPlayerCountMap.get(row);
        } else {
            throw new IllegalArgumentException("row " + row + " does not exist");
        }
    }

    public int getDistanceBetweenPlayersForRow(int row) {
        if (rowToPlayerDistanceMap.containsKey(row)) {
            return rowToPlayerDistanceMap.get(row);
        } else {
            throw new IllegalArgumentException("row " + row + " does not exist");
        }
    }

    public PlayerAngle getPlayerAngleForRow(int row) {
        if (rowToAngleMap.containsKey(row)) {
            return rowToAngleMap.get(row);
        } else {
            throw new IllegalArgumentException("row " + row + " does not exist");
        }
    }

    public int getRowXPosition(int row) throws IllegalArgumentException {
        if (rowToXPositionMap.containsKey(row)) {
            return rowToXPositionMap.get(row);
        } else {
            throw new IllegalArgumentException("row " + row + " does not exist");
        }
    }

    public int getRowYPosition(int row) throws IllegalArgumentException {
        if (rowToYPositionMap.containsKey(row)) {
            return rowToYPositionMap.get(row);
        } else {
            throw new IllegalArgumentException("row " + row + " does not exist");
        }
    }

    public Point getBallPosition() {
        return new Point(ballXPosition, ballYPosition);
    }

    public Point getPrevBallPosition() {
        return new Point(prevBallXPosition, prevBallYPosition);
    }

    public List<Point> getPlayerPositionsForRow(int row) {
        if (!rowToYPositionMap.containsKey(row)) {
            throw new IllegalArgumentException("row " + row + " does not exist");
        }
        List<Point> positions = new ArrayList<Point>();
        int y = rowToYPositionMap.get(row);
        int x = rowToXPositionMap.get(row);
        for (int player = 1; player <= rowToPlayerCountMap.get(row); player++) {
            positions.add(new Point(x * player, y));
        }
        return positions;
    }

    public Map<Integer, List<Point>> getPlayerPositionMap() {
        Map<Integer, List<Point>> positionMap = new HashMap<Integer, List<Point>>();
        for (int row : rowToAngleMap.keySet()) {
            List<Point> positions = new ArrayList<Point>();
            int y = rowToYPositionMap.get(row);
            int x = rowToXPositionMap.get(row);
            for (int player = 1; player <= rowToPlayerCountMap.get(row); player++) {
                positions.add(new Point(x * player, y));
            }
            positionMap.put(row, positions);
        }
        return positionMap;
    }

    public Integer getMaxX() {
        return xLength;
    }

    public Integer getMaxY() {
        return yLength;
    }

    public Map<Integer, Integer> getRowToPlayerCountMap() {
        return rowToPlayerCountMap;
    }

    public Map<Integer, Integer> getRowToPlayerDistanceMap() {
        return rowToPlayerDistanceMap;
    }

    public Map<Integer, Integer> getRowToXPositionMap() {
        return rowToXPositionMap;
    }

    public Map<Integer, Integer> getRowToYPositionMap() {
        return rowToYPositionMap;
    }

    public Map<Integer, PlayerAngle> getRowToAngleMap() {
        return rowToAngleMap;
    }

}
