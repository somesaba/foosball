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

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.saba.foosball.GameStateListener;
import com.saba.foosball.input.FoosballStateReader;
import com.saba.foosball.model.GameState;
import com.saba.foosball.model.PlayerAngle;
/**
 * Creates a 2D grid representing the gameState
 * @author saba
 *
 */
public class GameStateVisualization extends Canvas implements GameStateListener, MouseListener {
	private static final long serialVersionUID = 1L;
	private GameState gameState;
	private BufferStrategy strategy; 
	private int xBounds;
	private int yBounds;
	private FoosballStateReader stateReader;

	public GameStateVisualization(GameState gameState, FoosballStateReader stateReader) {
		if(gameState == null) {
			throw new IllegalArgumentException("gameState cannot be null");
		}
		this.gameState = gameState;
		this.stateReader = stateReader;
		this.addMouseListener(this);
	}

	public void init() {
		JFrame window = new JFrame("Foosball Visualization");
		this.xBounds = gameState.getMaxX()*2;
		this.yBounds = gameState.getMaxY();

		JPanel panel = (JPanel) window.getContentPane();
		panel.setPreferredSize(new Dimension(xBounds, yBounds));
		panel.setLayout(null);

		// setup our canvas size and put it into the content of the frame
		setBounds(0,0, xBounds, yBounds);
		panel.add(this);

		//Ignore repaint as we will do it ourselves
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

		this.refreshGraphics();
	}


	public void notifyGameStateUpdate() {
		this.refreshGraphics();
	}

	private void refreshGraphics() {
		Graphics2D graphics = (Graphics2D) strategy.getDrawGraphics();
		//Clear canvas
		graphics.setColor(Color.GREEN);
		graphics.fillRect(0,0,xBounds/2,yBounds);
		graphics.setColor(Color.GRAY);
		graphics.fillRect(xBounds/2,0,xBounds,yBounds);
		paintCameraFrame(graphics);
		paintBall(graphics);
		paintPlayers(graphics);
		strategy.show();
	}

	private void paintCameraFrame(Graphics2D graphics) { 
		BufferedImage img = stateReader.readState();
		graphics.drawImage(img, xBounds/2, 0, null);
		for(int x = 0 ; x < img.getWidth(); x++) {
			for(int y = 0; y < img.getHeight(); y++) {
				Color color = new Color(img.getRGB(x, y));
				if(color.getRed() > 200 && color.getGreen() > 200 && color.getBlue() > 200) {
					graphics.setColor(Color.WHITE);
					//graphics.fillRect(x + xBounds/2, y, 1, 1);
				}
			}
		}
		for(int row = 0; row < gameState.getNumOfRows(); row++) {
			int midPoint = gameState.getRowXPosition(row);
			graphics.setColor(Color.BLACK);
			graphics.drawRect(midPoint-5 + xBounds/2, 0, 10, img.getHeight());
		}
	}
	private void paintBall(Graphics2D graphics) {
		Point ballPosition = gameState.getBallPosition();
		Shape circle = new Ellipse2D.Double(ballPosition.getX(), ballPosition.getY(),
				20, 20);
		graphics.setColor(Color.WHITE);
		graphics.fill(circle);
	}

	private void paintPlayers(Graphics2D graphics) {
		for(int row = 0; row < gameState.getNumOfRows(); row++) {
			graphics.setColor(Color.GRAY);
			graphics.fillRect(gameState.getRowXPosition(row)-5, 0, 10, yBounds);
			if(row % 2 == 0) {
				graphics.setColor(Color.RED);
			} else {
				graphics.setColor(Color.BLUE);
			}
			int xOffset = 0;
			int xLen = 0;
			PlayerAngle angle = gameState.getPlayerAngleForRow(row);
			if(angle == PlayerAngle.VERTICAL) {
				xLen = 10;
			} else if(angle == PlayerAngle.BACKWARD_ANGLED) {
				xOffset = -10;
				xLen = 20;
			} else if(angle == PlayerAngle.BACKWARD_HORIZONTAL) {
				xOffset = -30;
				xLen = 40;
			} else if(angle == PlayerAngle.FORWARD_ANGLED) {
				xLen = 20;
			} else {
				xLen = 40;
			}
			int yPosition = (gameState.getRowYPosition(row));
			int xPosition = (gameState.getRowXPosition(row)) + xOffset - 5;
			for(int player = 0; player < gameState.getNumbersOfPlayersForRow(row); player++) {
				graphics.fillRect(xPosition, yPosition - 15 + player*gameState.getDistanceBetweenPlayersForRow(row), xLen, 20);
			}
		}
	}

	public void mouseClicked(MouseEvent e) {
		BufferedImage img = stateReader.readState();
		Color color = new Color(img.getRGB(e.getX() - xBounds/2, e.getY()));
		System.out.println("R="+color.getRed()+" G="+color.getGreen()+" B="+color.getBlue());
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

}
