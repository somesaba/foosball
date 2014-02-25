package com.saba.foosball.agent;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.saba.foosball.GameStateListener;
import com.saba.foosball.model.GameState;
import com.saba.foosball.model.PlayerAngle;
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
        agentThread.setPriority(Thread.MAX_PRIORITY);
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

    public void run() {
        isShutdown = false;
        while (!isShutdown) {
            if (didGameStateChange.compareAndSet(true, false)) {
                this.performAction();
            }
        }
    }

    public void resetRound() {
        List<Integer> intendedYPositions = new ArrayList<Integer>();
        List<PlayerAngle> intendedPlayerAngles = new ArrayList<PlayerAngle>();
        intendedPlayerAngles.add(PlayerAngle.VERTICAL);
        intendedPlayerAngles.add(PlayerAngle.VERTICAL);
        intendedYPositions.add(20);
        intendedYPositions.add(20);
        usbWriter.setHardPlayerPositions(intendedYPositions, intendedPlayerAngles);
        // Prompt User For New Round
        System.out.println("GOAL " + gameState.getPlayerThatScored());
        System.out.print("Round Restarts in...");
        for (int i = 10; i > 0; i--) {
            System.out.print(i + "..");
            try {
                Toolkit.getDefaultToolkit().beep();
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("0");
        gameState.restartRound();
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

}
