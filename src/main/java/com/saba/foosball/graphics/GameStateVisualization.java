package com.saba.foosball.graphics;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferStrategy;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.saba.foosball.model.GameState;
import com.saba.foosball.model.PlayerAngle;
/**
 * Creates a 2D grid representing the gameState
 * @author saba
 *
 */
public class GameStateVisualization extends Canvas {
    private static final long serialVersionUID = 1L;
    private GameState gameState;
    private BufferStrategy strategy; 
    private int gameStateToPixelMagnification;
    private int xBounds;
    private int yBounds;

    public GameStateVisualization(GameState gameState, int gameStateToPixelMagnification) {
        if(gameState == null) {
            throw new IllegalArgumentException("gameState cannot be null");
        }
        if(gameStateToPixelMagnification <= 0) {
            throw new IllegalArgumentException("gameStateToPixelMagnification must be > 0");
        }
        this.gameStateToPixelMagnification = gameStateToPixelMagnification;
        this.gameState = gameState;
    }

    public void init() {
        JFrame window = new JFrame("Foosball Visualization");
        this.xBounds = (int) ((gameState.getMaxX())*gameStateToPixelMagnification);
        this.yBounds = (int) ((gameState.getMaxY())*gameStateToPixelMagnification);

        JPanel panel = (JPanel) window.getContentPane();
        panel.setPreferredSize(new Dimension(xBounds, yBounds));
        panel.setLayout(null);

        // setup our canvas size and put it into the content of the frame
        setBounds(0,0, xBounds, yBounds);
        panel.add(this);

        //Ignore repaint as we will do it ourselves
        setIgnoreRepaint(true);

        //window.setSize(xSize,ySize);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.pack();
        window.setResizable(false);
        window.setVisible(true);

        // create the buffering strategy which will allow AWT
        // to manage our accelerated graphics
        this.createBufferStrategy(2);
        this.strategy = getBufferStrategy();

        this.refreshGraphics();
    }

    public void refreshGraphics() {
        Graphics2D graphics = (Graphics2D) strategy.getDrawGraphics();
        //Clear canvas
        graphics.setColor(Color.GREEN);
        graphics.fillRect(0,0,xBounds,yBounds);
        
        paintGrids(graphics);       
        paintBall(graphics);
        paintPlayers(graphics);
        strategy.show();
    }

    private void paintGrids(Graphics2D graphics) {
        graphics.setColor(Color.BLACK);
        for (int i = 0; i <= xBounds; i+= gameStateToPixelMagnification)
            graphics.drawLine (i, 0, i, yBounds);

        for (int i = 0; i <= yBounds; i+= gameStateToPixelMagnification)
            graphics.drawLine (0, i, xBounds, i);
    }

    private void paintBall(Graphics2D graphics) {
        Point ballPosition = gameState.getBallPosition();
        this.convertGameStatePointToGraphicsPoint(ballPosition);
        Shape circle = new Ellipse2D.Double(ballPosition.getX(), ballPosition.getY(),
                gameStateToPixelMagnification*1.5, gameStateToPixelMagnification*1.5);
        graphics.setColor(Color.orange);
        graphics.fill(circle);
    }
    
    private void paintPlayers(Graphics2D graphics) {
        for(int row = 0; row < gameState.getNumOfRows(); row++) {
            graphics.setColor(Color.GRAY);
            graphics.fillRect(gameState.getRowXPosition(row)*gameStateToPixelMagnification-gameStateToPixelMagnification*3/4, 0, gameStateToPixelMagnification/2, yBounds);
            if(row % 2 == 0) {
                graphics.setColor(Color.RED);
            } else {
                graphics.setColor(Color.BLUE);
            }
            int xOffset = 0;
            int xLen = 0;
            PlayerAngle angle = gameState.getPlayerAngleForRow(row);
            if(angle == PlayerAngle.DOWN) {
                xLen = gameStateToPixelMagnification;
            } else if(angle == PlayerAngle.BACKWARD_DOWN) {
                xOffset = gameStateToPixelMagnification;
                xLen = gameStateToPixelMagnification*2;
            } else if(angle == PlayerAngle.BACKWARD) {
                xOffset = gameStateToPixelMagnification*2;
                xLen = gameStateToPixelMagnification*3;
            } else if(angle == PlayerAngle.FORWARD_DOWN) {
                xLen = gameStateToPixelMagnification*2;
            } else {
                xLen = gameStateToPixelMagnification*3;
            }
            int yPosition = (gameState.getRowYPosition(row)-1)*gameStateToPixelMagnification;
            int xPosition = (gameState.getRowXPosition(row)-1)*gameStateToPixelMagnification - xOffset;
            for(int player = 0; player < gameState.getNumbersOfPlayersForRow(row); player++) {
                graphics.fillRect(xPosition, yPosition + player*gameState.getDistanceBetweenPlayersForRow(row)*gameStateToPixelMagnification, xLen, gameStateToPixelMagnification);
            }
        }
    }

    private void convertGameStatePointToGraphicsPoint(Point p) {
        p.setLocation((int) p.getX()*gameStateToPixelMagnification, (int)p.getY()*gameStateToPixelMagnification); 
    }

   
    public static void main(String[] args) throws InterruptedException {
        Map<Integer, Integer> rowToPlayerCountMap = new HashMap<Integer, Integer>();
        Map<Integer, Integer> rowToPlayerDistanceMap = new HashMap<Integer, Integer>();
        Map<Integer, Integer> rowToXPositionMap = new HashMap<Integer, Integer>();
        for(int i = 0; i < 4; i++) {
            rowToPlayerCountMap.put(i, 3);
            rowToPlayerDistanceMap.put(i, 5);
            rowToXPositionMap.put(i, i*10 + 5);
        }
        GameState gameState = new GameState(40,20, rowToPlayerCountMap, rowToPlayerDistanceMap, rowToXPositionMap);
        GameStateVisualization visualization = new GameStateVisualization(gameState, 30);
        visualization.init();
        while(true) {
             for(int row = 0; row < gameState.getNumOfRows(); row++) {
                 int angle = (int) (Math.random()*5);
                 gameState.updateRowAngle(row, PlayerAngle.values()[angle]);
                 int y = (int) ((gameState.getMaxY() - rowToPlayerDistanceMap.get(row)*(rowToPlayerCountMap.get(row) - 1) + 1)*Math.random());
                 gameState.updateRowYPosition(row, y);
             }
            gameState.updateBallPosition((int) (Math.random()*gameState.getMaxX()),(int) (Math.random()*gameState.getMaxY()));
            visualization.refreshGraphics();
            Thread.sleep(200);
        }
    }

}
