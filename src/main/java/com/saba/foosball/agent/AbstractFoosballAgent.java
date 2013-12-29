package com.saba.foosball.agent;

import java.util.concurrent.atomic.AtomicBoolean;

import com.saba.foosball.GameStateListener;
import com.saba.foosball.model.GameState;
import com.saba.foosball.output.USBWriter;

public abstract class AbstractFoosballAgent implements GameStateListener, Runnable {
    protected USBWriter usbWriter;
    protected GameState gameState;
    private volatile boolean isShutdown;
    private AtomicBoolean didGameStateChange = new AtomicBoolean(false);
    private Thread agentThread;

    public abstract void performAction();

    public void start() {
        agentThread = new Thread(this, "AgentThread");
        agentThread.start();
    }

    public void stop() {
        isShutdown = false;
        try {
            agentThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

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

    public void notifyGameStateUpdate() {
        this.didGameStateChange.set(true);
    }

    public void run() {
        isShutdown = false;
        while (!isShutdown) {
            if (didGameStateChange.compareAndSet(true, false)) {
                this.performAction();
            }
        }
    }

}
