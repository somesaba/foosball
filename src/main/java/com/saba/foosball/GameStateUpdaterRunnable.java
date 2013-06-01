package com.saba.foosball;

import com.saba.foosball.input.FoosballStateReader;
import com.saba.foosball.model.GameState;

public class GameStateUpdaterRunnable implements Runnable, GameStateUpdater {
    private volatile boolean isShutdown;
    private GameState gameState;
    private FoosballStateReader stateReader;
    
    public GameStateUpdaterRunnable(FoosballStateReader stateReader) {
        this.stateReader = stateReader;
    }
    
    public void run() {
        isShutdown = false;
        while(!isShutdown) {
            //TODO update shit
        }
        
    }
    public void setStateReader(FoosballStateReader stateReader) {
        this.stateReader = stateReader;
    }

    public void stop() {
        this.isShutdown = true;
    }
    
    public GameState getGameState() {
        return gameState;
    }

}
