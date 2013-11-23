package com.saba.foosball;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.saba.foosball.input.FoosballStateReader;
import com.saba.foosball.model.GameState;
import com.saba.foosball.model.PlayerAngle;

public class GameStateUpdaterImpl implements Runnable, GameStateUpdater {
	private volatile boolean isShutdown;
	private GameState gameState;
	private FoosballStateReader stateReader;
	private List<GameStateListener> listeners = new ArrayList<GameStateListener>();
	private Thread gameStateUpdaterThread;
	private float [][] ballPositionDistribution;

	public void start() {
		ballPositionDistribution = new float[gameState.getMaxX()][gameState.getMaxY()];
		gameStateUpdaterThread = new Thread(this);
		gameStateUpdaterThread.start();
	}

	public void run() {
		isShutdown = false;
		while(!isShutdown) {
			BufferedImage img = stateReader.readState();
			updatePlayerPositions(img);
			updateBallPosition(img);
			notifyListeners();
		}

	}

	private void updatePlayerPositions(BufferedImage img) {
		int width = img.getWidth();
		int height = img.getHeight();
		int numOfRows = gameState.getNumOfRows();
		for(int row = 0; row < numOfRows; row++) {
			int totalX = 0;
			int totalY = 0;
			int numPixels = 0;
			for(int x = row*width/numOfRows; x < (row + 1)*width/numOfRows;  x++) {
				for(int y = 0; y < height/2; y++) {
					Color color = new Color(img.getRGB(x, y));
					if(row == 0 || row == 2) {
						if((color.getRed() > 170 && color.getBlue() < 60 && color.getGreen() < 50) ||
								color.getRed() > 200 && color.getBlue() < 80 && color.getGreen() < 80) {
							//System.out.println("Got R=" + color.getRed() + " G="+color.getGreen()+ "B="+color.getBlue());
							totalX += x;
							totalY += y;
							numPixels++;
						}
					} else {
						if((color.getRed() < 70 && color.getBlue() > 160 && color.getGreen() < 70) ||
								(color.getRed() < 20 && color.getBlue() > 110 && color.getGreen() < 20)) {
							//System.out.println("Got R=" + color.getRed() + " G="+color.getGreen()+ "B="+color.getBlue());
							totalX += x;
							totalY += y;
							numPixels++;
						}
						}
					}
				}
			if(numPixels != 0) {
				gameState.updateRowYPosition(row, (int) totalY/numPixels);
				int avgX = totalX/numPixels;
				int distFromRowXPosition = gameState.getRowXPosition(row) - avgX;
				if(row == 3)
					System.out.println(distFromRowXPosition);
//				if() {
//					gameState.updateRowAngle(row, PlayerAngle.VERTICAL);
//				} else {
//					int playerMidPoint = (maxX + minX)/2;
//					if(playerMidPoint < gameState.getRowXPosition(row)) {
//						if(playerWidth < 40) {
//							gameState.updateRowAngle(row, PlayerAngle.BACKWARD_ANGLED);
//						} else {
//							gameState.updateRowAngle(row, PlayerAngle.BACKWARD_HORIZONTAL);
//						}
//					} else {
//						if(playerWidth < 40) {
//							gameState.updateRowAngle(row, PlayerAngle.FORWARD_ANGLED);
//						} else {
//							gameState.updateRowAngle(row, PlayerAngle.FORWARD_HORIZONTAL);
//						}
//					}
//				}
			}
		}
	}
	
	private void updatePlayerPositions2(BufferedImage img) {
		int width = img.getWidth();
		int height = img.getHeight();
		int numOfRows = gameState.getNumOfRows();
		for(int row = 0; row < numOfRows; row++) {
			int minX = width;
			int minY = height;
			int maxX = 0;
			for(int x = row*width/numOfRows; x < (row + 1)*width/numOfRows;  x++) {
				for(int y = 0; y < height/2; y++) {
					Color color = new Color(img.getRGB(x, y));
					if(row == 0 || row == 2) {
						if((color.getRed() > 170 && color.getBlue() < 60 && color.getGreen() < 50) ||
								color.getRed() > 200 && color.getBlue() < 80 && color.getGreen() < 80) {
							//System.out.println("Got R=" + color.getRed() + " G="+color.getGreen()+ "B="+color.getBlue());
							minX = Math.min(minX, x);
							minY = Math.min(minY, y);
							maxX = Math.max(maxX, x);
						}
					} else {
						if((color.getRed() < 70 && color.getBlue() > 160 && color.getGreen() < 70) ||
								(color.getRed() < 20 && color.getBlue() > 110 && color.getGreen() < 20)) {
							//System.out.println("Got R=" + color.getRed() + " G="+color.getGreen()+ "B="+color.getBlue());
							minX = Math.min(minX, x);
							minY = Math.min(minY, y);
							maxX = Math.max(maxX, x);
						}
					}
				}
			}
			if(minY == height)
				minY = 0;
			gameState.updateRowYPosition(row, minY);
			int playerWidth = maxX - minX;
			if(playerWidth < 27) {
				gameState.updateRowAngle(row, PlayerAngle.VERTICAL);
			} else {
				int playerMidPoint = (maxX + minX)/2;
				if(playerMidPoint < gameState.getRowXPosition(row)) {
					if(playerWidth < 40) {
						gameState.updateRowAngle(row, PlayerAngle.BACKWARD_ANGLED);
					} else {
						gameState.updateRowAngle(row, PlayerAngle.BACKWARD_HORIZONTAL);
					}
				} else {
					if(playerWidth < 40) {
						gameState.updateRowAngle(row, PlayerAngle.FORWARD_ANGLED);
					} else {
						gameState.updateRowAngle(row, PlayerAngle.FORWARD_HORIZONTAL);
					}
				}
			}
		}
	}

	private void updateBallPosition(BufferedImage img) {
		int numPoints = 0;
		long xTotal = 0;
		long yTotal = 0;
		for(int x = 0 ; x < img.getWidth(); x++) {
			for(int y = 0; y < img.getHeight(); y++) {
				Color color = new Color(img.getRGB(x, y));
				if(color.getRed() > 200 && color.getGreen() > 200 && color.getBlue() > 200) {
					xTotal += x;
					yTotal += y;
					numPoints++;
				}
			}
		}
		if(numPoints > 0)
			gameState.updateBallPosition((int) xTotal/numPoints, (int) yTotal/numPoints);
	}

	private void notifyListeners() {
		for(GameStateListener listener : listeners) {
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
