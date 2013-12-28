package com.saba.foosball.agent;

import com.saba.foosball.GameStateListener;
import com.saba.foosball.model.GameState;
import com.saba.foosball.output.USBWriter;

public abstract class AbstractFoosballAgent implements GameStateListener {
    protected USBWriter usbWriter;
    protected GameState gameState;

    public USBWriter getUsbWriter() {
        return usbWriter;
    }

    public void setUsbWriter(USBWriter usbWriter) {
        this.usbWriter = usbWriter;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

}
