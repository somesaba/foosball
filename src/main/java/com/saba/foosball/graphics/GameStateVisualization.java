package com.saba.foosball.graphics;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.saba.foosball.GameStateListener;
import com.saba.foosball.input.FoosballStateReader;
import com.saba.foosball.model.GameState;
import com.saba.foosball.model.PlayerAngle;
import com.saba.foosball.model.PotentialPositionRectangle;

/**
 * Creates a 2D grid representing the gameState
 * 
 * @author saba
 * 
 */
public class GameStateVisualization extends Canvas implements GameStateListener, MouseListener, Runnable {
    private static final long serialVersionUID = 1L;
    private GameState gameState;
    private BufferStrategy strategy;
    private int xBounds;
    private int yBounds;
    private FoosballStateReader stateReader;
    private volatile boolean isShutdown = false;
    private AtomicBoolean didGameStateChange = new AtomicBoolean(false);
    private Thread graphicsUpdaterThread;

    public GameStateVisualization(GameState gameState, FoosballStateReader stateReader) {
        if (gameState == null) {
            throw new IllegalArgumentException("gameState cannot be null");
        }
        this.gameState = gameState;
        this.stateReader = stateReader;
        this.addMouseListener(this);
    }

    public void start() {
        JFrame window = new JFrame("Foosball Visualization");
        this.xBounds = gameState.getMaxX() * 2;
        this.yBounds = gameState.getMaxY();

        JPanel panel = (JPanel) window.getContentPane();
        panel.setPreferredSize(new Dimension(xBounds, yBounds));
        panel.setLayout(null);

        // setup our canvas size and put it into the content of the frame
        setBounds(0, 0, xBounds, yBounds);
        panel.add(this);

        // Ignore repaint as we will do it ourselves
        setIgnoreRepaint(true);
        window.setLocation(300, 300);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.pack();
        window.setResizable(false);
        window.setVisible(true);

        // create the buffering strategy which will allow AWT
        // to manage our accelerated graphics
        this.createBufferStrategy(2);
        this.strategy = getBufferStrategy();

        this.graphicsUpdaterThread = new Thread(this, "GraphicsUpdaterThread");
        this.graphicsUpdaterThread.start();
    }

    public void stop() {
        isShutdown = false;
        try {
            graphicsUpdaterThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        this.refreshGraphics();
        isShutdown = false;
        while (!isShutdown) {
            if (didGameStateChange.compareAndSet(true, false)) {
                this.refreshGraphics();
            } else {
                try {
                    Thread.sleep(8);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void notifyGameStateUpdate() {
        this.didGameStateChange.set(true);
    }

    private void refreshGraphics() {
        Graphics2D graphics = (Graphics2D) strategy.getDrawGraphics();
        // Clear canvas
        graphics.setColor(Color.GREEN);
        graphics.fillRect(0, 0, xBounds / 2, yBounds);
        paintCameraFrame(graphics);
        paintBall(graphics);
        paintPlayers(graphics);
        strategy.show();
    }

    private void paintCameraFrame(Graphics2D graphics) {
        BufferedImage img = stateReader.readState();
        graphics.drawImage(img, xBounds / 2, 0, null);
        paintCameraPlayerRects(img, graphics);
        paintCameraPlayerLegRects(img, graphics);

        paintCameraBallRects(img, graphics);
    }

    private void paintCameraPlayerRects(BufferedImage img, Graphics2D graphics) {
        for (int row = 0; row < gameState.getNumOfRows(); row++) {
            int midPoint = gameState.getRowXPosition(row);
            graphics.setColor(Color.WHITE);
            graphics.drawRect(midPoint - 5 + xBounds / 2, 0, 10, img.getHeight());
            if (row == 1 || row == 3) {
                graphics.setColor(Color.RED);
            } else {
                graphics.setColor(Color.BLUE);
            }
            List<PotentialPositionRectangle> potentialPositionRectangles = new ArrayList<PotentialPositionRectangle>();
            for (int x = gameState.getRowXPosition(row) - 5; x < gameState.getRowXPosition(row) + 5; x++) {
                for (int y = 140; y < img.getHeight(); y++) {
                    Color color = new Color(img.getRGB(x, y));
                    if (row == 1 || row == 3) {
                        if (color.getRed() - color.getGreen() > 20 && color.getRed() - color.getBlue() > 50) {
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
                        if ((color.getGreen() - color.getRed() <= 5 || color.getGreen() - color.getRed() >= 5) && color.getBlue() >= 10
                                && color.getBlue() > color.getGreen() && color.getBlue() > color.getRed()) {
                            // graphics.fillRect(x + xBounds / 2, y, 1, 1);
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

            for (PotentialPositionRectangle rect : potentialPositionRectangles) {
                graphics.drawRect(rect.getxStart() + xBounds / 2, rect.getyStart(), (rect.getxEnd() - rect.getxStart()),
                        rect.getyEnd() - rect.getyStart());
                int yEnd = rect.getyEnd() >= gameState.getMaxY() ? gameState.getMaxY() - 1 : rect.getyEnd();
                int yStart = rect.getyStart() < 0 ? 0 : rect.getyStart();
                if (yEnd - yStart < 20) {
                    noisyMemberships.add(rect);
                }
            }
            potentialPositionRectangles.removeAll(noisyMemberships);
            PotentialPositionRectangle bestRect = null;
            double bestScore = Double.MIN_VALUE;
            for (PotentialPositionRectangle rect : potentialPositionRectangles) {
                double score = rect.getyStart() + rect.getYLen();
                if (score > bestScore) {
                    bestScore = score;
                    bestRect = rect;
                }
            }
            if (bestRect != null) {
                // graphics.drawRect(bestRect.getxStart() + xBounds / 2, bestRect.getyStart(),
                // (bestRect.getxEnd() - bestRect.getxStart()), bestRect.getyEnd() - bestRect.getyStart());

            }
        }

    }

    private void paintCameraPlayerLegRects(BufferedImage img, Graphics2D graphics) {
        int numOfRows = gameState.getNumOfRows();
        for (int row = 0; row < numOfRows; row++) {
            if (row == 1 || row == 3) {
                graphics.setColor(Color.RED);
            } else {
                graphics.setColor(Color.BLUE);
            }
            // Right Side
            int xStart = gameState.getRowXPosition(row) + 10;
            if (row == 0) {
                xStart += 10;
            }
            if (row == 1) {
                xStart += 5;
            }
            graphics.drawRect(xStart + xBounds / 2, gameState.getRowYPosition(row) - 15, 12, 24);
            for (int x = xStart; x < xStart + 12; x++) {
                for (int y = gameState.getRowYPosition(row) - 15; y < Math.min(gameState.getRowYPosition(row) + 9, img.getHeight()); y++) {
                    Color color = new Color(img.getRGB(x, y));
                    if (row == 1 || row == 3) {
                        if (color.getRed() - color.getGreen() > 20 && color.getRed() - color.getBlue() > 50
                                && color.getGreen() - color.getBlue() > 10) {
                            graphics.fillRect(x + xBounds / 2, y, 1, 1);
                        }
                    } else {
                        if ((color.getGreen() - color.getRed() <= 5 || color.getGreen() - color.getRed() >= 5) && color.getBlue() >= 20
                                && color.getBlue() > color.getGreen() && color.getBlue() > color.getRed()) {
                            graphics.fillRect(x + xBounds / 2, y, 1, 1);

                        }
                    }
                }
            }
            // Right Side
            xStart = gameState.getRowXPosition(row) - 25;
            if (row == 3) {
                xStart -= 10;
            }
            graphics.drawRect(xStart + xBounds / 2, gameState.getRowYPosition(row) - 15, 12, 24);
            for (int x = xStart; x < xStart + 12; x++) {
                for (int y = gameState.getRowYPosition(row) - 15; y < Math.min(gameState.getRowYPosition(row) + 9, img.getHeight()); y++) {
                    Color color = new Color(img.getRGB(x, y));
                    if (row == 1 || row == 3) {
                        if (color.getRed() - color.getGreen() > 20 && color.getRed() - color.getBlue() > 50
                                && color.getGreen() - color.getBlue() > 10) {
                            graphics.fillRect(x + xBounds / 2, y, 1, 1);
                        }
                    } else {
                        if ((color.getGreen() - color.getRed() <= 5 || color.getGreen() - color.getRed() >= 5) && color.getBlue() >= 20
                                && color.getBlue() > color.getGreen() && color.getBlue() > color.getRed()) {
                            graphics.fillRect(x + xBounds / 2, y, 1, 1);

                        }
                    }
                }
            }

        }
    }

    private void paintBall(Graphics2D graphics) {
        Point ballPosition = gameState.getBallPosition();
        Shape circle = new Ellipse2D.Double(ballPosition.getX() - 10, ballPosition.getY() - 10, 20, 20);
        graphics.setColor(Color.WHITE);
        graphics.fill(circle);
        Point prevBallPosition = gameState.getPrevBallPosition();
        int diffX = (int) (ballPosition.getX() - prevBallPosition.getX());
        int diffY = (int) (ballPosition.getY() - prevBallPosition.getY());
        if (diffX != 0 || diffY != 0) {
            diffX *= 5;
            diffY *= 5;
            graphics.setColor(Color.BLACK);
            graphics.drawLine((int) ballPosition.getX(), (int) ballPosition.getY(), (int) ballPosition.getX() + diffX, (int) ballPosition.getY()
                    + diffY);
        }
    }

    private void paintPlayers(Graphics2D graphics) {
        for (int row = 0; row < gameState.getNumOfRows(); row++) {
            graphics.setColor(Color.GRAY);
            graphics.fillRect(gameState.getRowXPosition(row) - 5, 0, 10, yBounds);
            if (row % 2 == 0) {
                graphics.setColor(Color.BLUE);
            } else {
                graphics.setColor(Color.RED);
            }
            int xOffset = 0;
            int xLen = 0;
            PlayerAngle angle = gameState.getPlayerAngleForRow(row);
            if (angle == PlayerAngle.VERTICAL) {
                xLen = 10;
            } else if (angle == PlayerAngle.BACKWARD_ANGLED) {
                xOffset = -10;
                xLen = 20;
            } else if (angle == PlayerAngle.BACKWARD_HORIZONTAL) {
                xOffset = -30;
                xLen = 40;
            } else if (angle == PlayerAngle.FORWARD_ANGLED) {
                xLen = 20;
            } else {
                xLen = 40;
            }
            int yPosition = (gameState.getRowYPosition(row));
            int xPosition = (gameState.getRowXPosition(row)) + xOffset - 5;
            for (int player = 0; player < gameState.getNumbersOfPlayersForRow(row); player++) {
                graphics.fillRect(xPosition, yPosition - 10 - player * gameState.getDistanceBetweenPlayersForRow(row), xLen, 20);
            }
        }
    }

    private void paintCameraBallRects(BufferedImage img, Graphics2D graphics) {
        graphics.setColor(Color.WHITE);
        List<PotentialPositionRectangle> potentialBallPositionRectangles = new ArrayList<PotentialPositionRectangle>();
        for (int x = 0; x < img.getWidth(); x++) {
            boolean skip = false;
            // Check to see if pixel is on the bar
            for (int row = 0; row < gameState.getNumOfRows(); row++) {
                int xPositionOfBar = gameState.getRowXPosition(row);
                if (x > xPositionOfBar - 6 && x < xPositionOfBar + 6) {
                    skip = true;
                    break;
                }
            }
            if (skip) {
                continue;
            }
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
        for (PotentialPositionRectangle rect : potentialBallPositionRectangles) {
            graphics.drawRect(rect.getxStart() + xBounds / 2, rect.getyStart(), (rect.getxEnd() - rect.getxStart()),
                    rect.getyEnd() - rect.getyStart());
        }
    }

    public void mouseClicked(MouseEvent e) {
        BufferedImage img = stateReader.readState();
        Color color = new Color(img.getRGB(e.getX() - xBounds / 2, e.getY()));
        System.out.println("R=" + color.getRed() + " G=" + color.getGreen() + " B=" + color.getBlue() + "Position=" + e.getPoint());
        // // Feature 3
        int yPosOfBall = gameState.getBallYPosition() / 3;
        List<Integer> yPositions = new ArrayList<Integer>();
        boolean isRowThreeNearTheBall = false;
        for (int player = 0; player < gameState.getNumbersOfPlayersForRow(3); player++) {
            int yPosOfPlayer = (gameState.getRowYPosition(3) - player * gameState.getDistanceBetweenPlayersForRow(3)) / 3;
            if (player == 0) {
                yPosOfPlayer -= 3;
            }
            if (player == 2) {
                yPosOfPlayer += 3;
            }
            yPositions.add(yPosOfPlayer);

            if (yPosOfBall == yPosOfPlayer) {
                isRowThreeNearTheBall = true;
            }
        }
        int xPosOfBall = gameState.getBallXPosition();
        boolean hit = (xPosOfBall < (gameState.getRowXPosition(3)) && xPosOfBall > gameState.getRowXPosition(3) - 50);
        System.out.println("is=" + isRowThreeNearTheBall + "\tList=" + yPositions + "\tball=" + yPosOfBall + "\thit=" + hit);
    }

    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public GameState getGameState() {
        return gameState;
    }

}
