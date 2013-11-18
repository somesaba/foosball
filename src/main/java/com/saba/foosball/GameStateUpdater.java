package com.saba.foosball;

import com.saba.foosball.input.FoosballStateReader;
import com.saba.foosball.model.GameState;

public interface GameStateUpdater {
	
	public void setGameState(GameState gameState);
	
    public void setStateReader(FoosballStateReader stateReader);
    
    public void start();
    
    public void stop();
        
    public void register(GameStateListener gameStateListener);
    
}
