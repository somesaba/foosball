package com.saba.foosball;

import com.saba.foosball.input.FoosballStateReader;
import com.saba.foosball.model.GameState;

public interface GameStateUpdater {
    
    public GameState getGameState();
    
    public void setStateReader(FoosballStateReader stateReader);
    
    public void run();
    
    public void stop();
    
}
