package com.saba.foosball;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.saba.foosball.input.FoosballStateReader;
import com.saba.foosball.model.GameState;
import com.saba.foosball.model.Player;
import com.saba.foosball.model.PlayerAngle;
import com.saba.foosball.model.PotentialPositionRectangle;

public class NaiveGameStateUpdater implements Runnable, GameStateUpdater {
    private volatile boolean isShutdown;
    private GameState gameState;
    private FoosballStateReader stateReader;
    private List<GameStateListener> listeners = new ArrayList<GameStateListener>();
    private Thread gameStateUpdaterThread;
    private ExecutorService executor = Executors.newFixedThreadPool(5);

    public void start() {
        gameStateUpdaterThread = new Thread(this, "NaiveGameStateUpdaterThread");
        gameStateUpdaterThread.start();
    }

    public void run() {
        isShutdown = false;
        while (!isShutdown) {
            BufferedImage img = stateReader.readState();
            // Start Calculating Ball Positions
            List<Future<PotentialPositionRectangle>> potentialBallPositions = this.startCalculatingBallPositionsAsync(img);
            // Update Players positions in current thread
            this.updatePlayerPositions(img);
            // Wait for futures and update ball positions if not in end state
            this.updateBallPosition(potentialBallPositions);

            if (gameState.getPlayerThatScored() != null)
                gameState.updateBallPosition(gameState.getMaxX() / 2, gameState.getMaxY() / 2);

            notifyListeners();
        }
    }

    private void updatePlayerYPositions(BufferedImage img) {
        for (int row = 0; row < gameState.getNumOfRows(); row++) {
            List<PotentialPositionRectangle> potentialPositionRectangles = new ArrayList<PotentialPositionRectangle>();
            for (int x = gameState.getRowXPosition(row) - 5; x < gameState.getRowXPosition(row); x++) {
                for (int y = 140; y < img.getHeight(); y++) {
                    Color color = new Color(img.getRGB(x, y));
                    if (row == 1 || row == 3) {
                        if (color.getRed() - color.getGreen() > 20 && color.getRed() - color.getBlue() > 50
                                && color.getGreen() - color.getBlue() > 10) {
                            List<PotentialPositionRectangle> rectMemberships = new ArrayList<PotentialPositionRectangle>();
                            for (PotentialPositionRectangle rect : potentialPositionRectangles) {
                                if (rect.isPotentialMember(x, y)) {
                                    rectMemberships.add(rect);
                                    rect.addMember(x, y);
                                }
                            }
                            if (rectMemberships.size() == 0) {
                                PotentialPositionRectangle rect = new PotentialPositionRectangle(x, y);
                                potentialPositionRectangles.add(rect);
                                rectMemberships.add(rect);
                            }
                            if (rectMemberships.size() > 1) {
                                PotentialPositionRectangle masterRect = rectMemberships.get(0);
                                for (int i = 1; i < rectMemberships.size(); i++) {
                                    masterRect.addRectangle(rectMemberships.get(i));
                                }
                                potentialPositionRectangles.removeAll(rectMemberships.subList(1, rectMemberships.size()));
                            }
                        }
                    } else {
                        if ((color.getGreen() - color.getRed() <= 5 || color.getGreen() - color.getRed() >= 5) && color.getBlue() >= 20
                                && color.getBlue() > color.getGreen() && color.getBlue() > color.getRed()) {
                            List<PotentialPositionRectangle> rectMemberships = new ArrayList<PotentialPositionRectangle>();
                            for (PotentialPositionRectangle rect : potentialPositionRectangles) {
                                if (rect.isWithin(x, y, 3)) {
                                    rectMemberships.add(rect);
                                    rect.addMember(x, y);
                                }
                            }
                            if (rectMemberships.size() == 0) {
                                PotentialPositionRectangle rect = new PotentialPositionRectangle(x, y);
                                potentialPositionRectangles.add(rect);
                                rectMemberships.add(rect);
                            }
                            if (rectMemberships.size() > 1) {
                                PotentialPositionRectangle masterRect = rectMemberships.get(0);
                                for (int i = 1; i < rectMemberships.size(); i++) {
                                    masterRect.addRectangle(rectMemberships.get(i));
                                }
                                potentialPositionRectangles.removeAll(rectMemberships.subList(1, rectMemberships.size()));
                            }

                        }
                    }
                }
            }
            List<PotentialPositionRectangle> noisyMemberships = new ArrayList<PotentialPositionRectangle>();
            // Remove noisy memberships
            for (PotentialPositionRectangle rect : potentialPositionRectangles) {
                int yEnd = rect.getyEnd() >= gameState.getMaxY() ? gameState.getMaxY() - 1 : rect.getyEnd();
                int yStart = rect.getyStart() < 0 ? 0 : rect.getyStart();
                if (yEnd - yStart < 15) {
                    noisyMemberships.add(rect);
                }
            }
            potentialPositionRectangles.removeAll(noisyMemberships);
            PotentialPositionRectangle bestRect = null;
            double bestScore = Double.MAX_VALUE;
            for (PotentialPositionRectangle rect : potentialPositionRectangles) {
                double score = Math.abs(gameState.getRowYPosition(row) - rect.getyStart());
                if (score < bestScore) {
                    bestScore = score;
                    bestRect = rect;
                }
            }
            if (bestRect == null) {
                continue;
            }
            int yEnd = bestRect.getyEnd() >= gameState.getMaxY() ? gameState.getMaxY() - 1 : bestRect.getyEnd();
            int yStart = bestRect.getyStart() < 0 ? 0 : bestRect.getyStart();
            gameState.updateRowYPosition(row, yStart + (yEnd - yStart) / 2);
        }
    }

    private void updatePlayerPositions(BufferedImage img) {
        this.updatePlayerYPositions(img);
        this.updatePlayerAngles(img);
    }

    private void updatePlayerAngles(BufferedImage img) {
        int numOfRows = gameState.getNumOfRows();
        for (int row = 0; row < numOfRows; row++) {
            int numOfPixelsRight = 0;
            int numOfPixelsLeft = 0;
            // Right Side
            int xStart = gameState.getRowXPosition(row) + 10;
            if (row == 0) {
                xStart += 10;
            }
            if (row == 1) {
                xStart += 5;
            }
            for (int x = xStart; x < xStart + 12; x++) {
                for (int y = gameState.getRowYPosition(row) - 15; y < Math.min(gameState.getRowYPosition(row) + 9, img.getHeight()); y++) {
                    Color color = new Color(img.getRGB(x, y));
                    if (row == 1 || row == 3) {
                        if (color.getRed() - color.getGreen() > 20 && color.getRed() - color.getBlue() > 50
                                && color.getGreen() - color.getBlue() > 10) {
                            numOfPixelsRight++;
                        }
                    } else {
                        if ((color.getGreen() - color.getRed() <= 5 || color.getGreen() - color.getRed() >= 5) && color.getBlue() >= 20
                                && color.getBlue() > color.getGreen() && color.getBlue() > color.getRed()) {
                            numOfPixelsRight++;

                        }
                    }
                }
            }
            // Left Side
            xStart = gameState.getRowXPosition(row) - 25;
            if (row == 3) {
                xStart -= 10;
            }
            for (int x = xStart; x < xStart + 12; x++) {
                for (int y = gameState.getRowYPosition(row) - 15; y < Math.min(gameState.getRowYPosition(row) + 9, img.getHeight()); y++) {
                    Color color = new Color(img.getRGB(x, y));
                    if (row == 1 || row == 3) {
                        if (color.getRed() - color.getGreen() > 20 && color.getRed() - color.getBlue() > 50
                                && color.getGreen() - color.getBlue() > 10) {
                            numOfPixelsLeft++;
                        }
                    } else {
                        if ((color.getGreen() - color.getRed() <= 5 || color.getGreen() - color.getRed() >= 5) && color.getBlue() >= 20
                                && color.getBlue() > color.getGreen() && color.getBlue() > color.getRed()) {
                            numOfPixelsLeft++;

                        }
                    }
                }
            }
            numOfPixelsLeft -= 25;
            numOfPixelsRight -= 25;
            if (numOfPixelsLeft <= 0 && numOfPixelsRight <= 0) {
                gameState.updateRowAngle(row, PlayerAngle.VERTICAL);
            } else if (numOfPixelsLeft > numOfPixelsRight) {
                xStart = gameState.getRowXPosition(row) - 25;
                if (row == 3) {
                    xStart -= 10;
                }
                // Check next 10 pixels
                int numOfPixelsFarLeft = 0;
                for (int x = xStart - 10; x < xStart; x++) {
                    for (int y = gameState.getRowYPosition(row) - 15; y < Math.min(gameState.getRowYPosition(row) + 9, img.getHeight()); y++) {
                        Color color = new Color(img.getRGB(x, y));
                        if (row == 1 || row == 3) {
                            if (color.getRed() - color.getGreen() > 20 && color.getRed() - color.getBlue() > 50
                                    && color.getGreen() - color.getBlue() > 10) {
                                numOfPixelsFarLeft++;
                            }
                        } else {
                            if ((color.getGreen() - color.getRed() <= 5 || color.getGreen() - color.getRed() >= 5) && color.getBlue() >= 20
                                    && color.getBlue() > color.getGreen() && color.getBlue() > color.getRed()) {
                                numOfPixelsFarLeft++;

                            }
                        }
                    }
                }
                numOfPixelsFarLeft -= 20;
                if (numOfPixelsFarLeft > 0) {
                    gameState.updateRowAngle(row, PlayerAngle.BACKWARD_HORIZONTAL);

                } else {
                    gameState.updateRowAngle(row, PlayerAngle.BACKWARD_ANGLED);
                }
            } else if (numOfPixelsLeft < numOfPixelsRight) {
                // Check next 10 pixels
                xStart = gameState.getRowXPosition(row) + 10;
                if (row == 0) {
                    xStart += 10;
                }
                if (row == 1) {
                    xStart += 5;
                }
                int numOfPixelsFarRight = 0;
                for (int x = xStart + 12; x < xStart + 22; x++) {
                    for (int y = gameState.getRowYPosition(row) - 15; y < Math.min(gameState.getRowYPosition(row) + 9, img.getHeight()); y++) {
                        Color color = new Color(img.getRGB(x, y));
                        if (row == 1 || row == 3) {
                            if (color.getRed() - color.getGreen() > 20 && color.getRed() - color.getBlue() > 50
                                    && color.getGreen() - color.getBlue() > 10) {
                                numOfPixelsFarRight++;
                            }
                        } else {
                            if ((color.getGreen() - color.getRed() <= 5 || color.getGreen() - color.getRed() >= 5) && color.getBlue() >= 20
                                    && color.getBlue() > color.getGreen() && color.getBlue() > color.getRed()) {
                                numOfPixelsFarRight++;
                            }
                        }
                    }
                }
                numOfPixelsFarRight -= 20;
                if (numOfPixelsFarRight > 0) {
                    gameState.updateRowAngle(row, PlayerAngle.FORWARD_HORIZONTAL);
                } else {
                    gameState.updateRowAngle(row, PlayerAngle.FORWARD_ANGLED);
                }
            }

        }
    }

    private List<Future<PotentialPositionRectangle>> startCalculatingBallPositionsAsync(BufferedImage img) {
        List<Future<PotentialPositionRectangle>> potentialBallPositions = new ArrayList<Future<PotentialPositionRectangle>>(5);
        int lastXEnd = 0;
        for (int row = 0; row < gameState.getNumOfRows(); row++) {
            int midPoint = gameState.getRowXPosition(row);
            potentialBallPositions.add(executor.submit(new BallPositionCallable(img, lastXEnd, midPoint)));
        }
        potentialBallPositions.add(executor.submit(new BallPositionCallable(img, lastXEnd, img.getWidth())));
        return potentialBallPositions;
    }

    private void updateBallPosition(List<Future<PotentialPositionRectangle>> potentialBallPositions) {
        List<PotentialPositionRectangle> potentialBallPositionRectangles = new ArrayList<PotentialPositionRectangle>(5);
        for (Future<PotentialPositionRectangle> potentialRectFuture : potentialBallPositions) {
            try {
                PotentialPositionRectangle rect = potentialRectFuture.get();
                if (rect != null) {
                    potentialBallPositionRectangles.add(rect);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Get the rectangle that is closest to a 25x25 square
        int ballXPosition = gameState.getBallXPosition();
        int ballYPosition = gameState.getBallYPosition();
        double bestScore = Double.MAX_VALUE;
        for (PotentialPositionRectangle rect : potentialBallPositionRectangles) {
            int xEnd = rect.getxEnd() >= gameState.getMaxX() ? gameState.getMaxX() - 1 : rect.getxEnd();
            int xStart = rect.getxStart() < 0 ? 0 : rect.getxStart();
            int yEnd = rect.getyEnd() >= gameState.getMaxY() ? gameState.getMaxY() - 1 : rect.getyEnd();
            int yStart = rect.getyStart() < 0 ? 0 : rect.getyStart();
            int xPosition = xStart + (xEnd - xStart) / 2;
            int yPosition = yStart + (yEnd - yStart) / 2;
            // Score = distance from perfect 22x22 square and distance from last ball position
            double score = Math.sqrt(Math.pow(22 - rect.getXLen(), 2) + Math.pow(22 - rect.getYLen(), 2))
                    + (Math.sqrt(Math.pow(gameState.getBallXPosition() - xPosition, 2) + Math.pow(gameState.getBallYPosition() - yPosition, 2))) / 3;
            if (score < bestScore) {
                bestScore = score;
                ballXPosition = xPosition;
                ballYPosition = yPosition;
            }
        }
        // System.out.println("Best Rec Size=" + (xEnd - xStart) + "," + (yEnd - yStart) + "\t dist=" + bestScore);
        gameState.updateBallPosition(ballXPosition, ballYPosition);
        if (ballXPosition < 20) {
            gameState.setScoringPlayer(Player.SELF);
        }
        if (ballXPosition > 295) {
            gameState.setScoringPlayer(Player.ENEMY);
        }
    }

    private class BallPositionCallable implements Callable<PotentialPositionRectangle> {

        private BufferedImage img;
        private int xStart;
        private int xEnd;

        public BallPositionCallable(BufferedImage img, int xStart, int xEnd) {
            super();
            this.img = img;
            this.xStart = xStart;
            this.xEnd = xEnd;
        }

        public PotentialPositionRectangle call() throws Exception {
            List<PotentialPositionRectangle> potentialBallPositionRectangles = new ArrayList<PotentialPositionRectangle>();
            for (int x = xStart; x < xEnd; x++) {
                for (int y = 0; y < img.getHeight(); y++) {
                    Color color = new Color(img.getRGB(x, y));
                    if (color.getRed() > 200 && color.getGreen() > 200 && color.getBlue() > 200) {
                        List<PotentialPositionRectangle> rectMemberships = new ArrayList<PotentialPositionRectangle>();
                        for (PotentialPositionRectangle rect : potentialBallPositionRectangles) {
                            if (rect.isPotentialMember(x, y)) {
                                rectMemberships.add(rect);
                                rect.addMember(x, y);
                            }
                        }
                        if (rectMemberships.size() == 0) {
                            PotentialPositionRectangle rect = new PotentialPositionRectangle(x, y);
                            potentialBallPositionRectangles.add(rect);
                            rectMemberships.add(rect);
                        }
                        if (rectMemberships.size() > 1) {
                            PotentialPositionRectangle masterRect = rectMemberships.get(0);
                            for (int i = 1; i < rectMemberships.size(); i++) {
                                masterRect.addRectangle(rectMemberships.get(i));
                            }
                            potentialBallPositionRectangles.removeAll(rectMemberships.subList(1, rectMemberships.size()));
                        }
                    }
                }
            }
            // Get the rectangle that is closest to a 25x25 square
            double bestScore = Double.MAX_VALUE;
            PotentialPositionRectangle bestRect = null;
            for (PotentialPositionRectangle rect : potentialBallPositionRectangles) {
                int xEnd = rect.getxEnd() >= gameState.getMaxX() ? gameState.getMaxX() - 1 : rect.getxEnd();
                int xStart = rect.getxStart() < 0 ? 0 : rect.getxStart();
                int yEnd = rect.getyEnd() >= gameState.getMaxY() ? gameState.getMaxY() - 1 : rect.getyEnd();
                int yStart = rect.getyStart() < 0 ? 0 : rect.getyStart();
                int xPosition = xStart + (xEnd - xStart) / 2;
                int yPosition = yStart + (yEnd - yStart) / 2;
                // Score = distance from perfect 22x22 square and distance from last ball position
                double score = Math.sqrt(Math.pow(22 - rect.getXLen(), 2) + Math.pow(22 - rect.getYLen(), 2))
                        + (Math.sqrt(Math.pow(gameState.getBallXPosition() - xPosition, 2) + Math.pow(gameState.getBallYPosition() - yPosition, 2)))
                        / 3;
                if (score < bestScore) {
                    bestScore = score;
                    bestRect = rect;
                }
            }
            return bestRect;
        }

    }

    private void updateBallPosition(BufferedImage img) {
        // Dont track ball until round restarts
        if (gameState.getPlayerThatScored() != null) {
            return;
        }
        List<PotentialPositionRectangle> potentialBallPositionRectangles = new ArrayList<PotentialPositionRectangle>();
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                Color color = new Color(img.getRGB(x, y));
                if (color.getRed() > 200 && color.getGreen() > 200 && color.getBlue() > 200) {
                    List<PotentialPositionRectangle> rectMemberships = new ArrayList<PotentialPositionRectangle>();
                    for (PotentialPositionRectangle rect : potentialBallPositionRectangles) {
                        if (rect.isPotentialMember(x, y)) {
                            rectMemberships.add(rect);
                            rect.addMember(x, y);
                        }
                    }
                    if (rectMemberships.size() == 0) {
                        PotentialPositionRectangle rect = new PotentialPositionRectangle(x, y);
                        potentialBallPositionRectangles.add(rect);
                        rectMemberships.add(rect);
                    }
                    if (rectMemberships.size() > 1) {
                        PotentialPositionRectangle masterRect = rectMemberships.get(0);
                        for (int i = 1; i < rectMemberships.size(); i++) {
                            masterRect.addRectangle(rectMemberships.get(i));
                        }
                        potentialBallPositionRectangles.removeAll(rectMemberships.subList(1, rectMemberships.size()));
                    }
                }
            }
        }
        // Get the rectangle that is closest to a 25x25 square
        int ballXPosition = gameState.getBallXPosition();
        int ballYPosition = gameState.getBallYPosition();
        double bestScore = Double.MAX_VALUE;
        for (PotentialPositionRectangle rect : potentialBallPositionRectangles) {
            int xEnd = rect.getxEnd() >= gameState.getMaxX() ? gameState.getMaxX() - 1 : rect.getxEnd();
            int xStart = rect.getxStart() < 0 ? 0 : rect.getxStart();
            int yEnd = rect.getyEnd() >= gameState.getMaxY() ? gameState.getMaxY() - 1 : rect.getyEnd();
            int yStart = rect.getyStart() < 0 ? 0 : rect.getyStart();
            int xPosition = xStart + (xEnd - xStart) / 2;
            int yPosition = yStart + (yEnd - yStart) / 2;
            // Score = distance from perfect 22x22 square and distance from last ball position
            double score = Math.sqrt(Math.pow(22 - rect.getXLen(), 2) + Math.pow(22 - rect.getYLen(), 2))
                    + (Math.sqrt(Math.pow(gameState.getBallXPosition() - xPosition, 2) + Math.pow(gameState.getBallYPosition() - yPosition, 2))) / 3;
            if (score < bestScore) {
                bestScore = score;
                ballXPosition = xPosition;
                ballYPosition = yPosition;
            }
        }
        // System.out.println("Best Rec Size=" + (xEnd - xStart) + "," + (yEnd - yStart) + "\t dist=" + bestScore);
        gameState.updateBallPosition(ballXPosition, ballYPosition);
        if (ballXPosition < 10) {
            gameState.setScoringPlayer(Player.SELF);
        }
        if (ballXPosition > 310) {
            gameState.setScoringPlayer(Player.ENEMY);
        }

    }

    private void notifyListeners() {
        for (GameStateListener listener : listeners) {
            listener.notifyGameStateUpdate();
        }
    }

    public void setStateReader(FoosballStateReader stateReader) {
        this.stateReader = stateReader;
    }

    public void stop() {
        this.isShutdown = true;
        try {
            gameStateUpdaterThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void register(GameStateListener gameStateListener) {
        listeners.add(gameStateListener);
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public void setListeners(List<GameStateListener> listeners) {
        this.listeners = listeners;
    }

}
