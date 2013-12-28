package com.saba.foosball;

import com.saba.foosball.model.GameState;

public interface GameStateListener {

    public GameState getGameState();

    public void notifyGameStateUpdate();
}
