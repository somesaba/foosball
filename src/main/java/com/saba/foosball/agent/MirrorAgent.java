package com.saba.foosball.agent;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;

import com.saba.foosball.model.PlayerAngle;

/*
 * This agent will mirror what the opposing player does
 */
public class MirrorAgent extends AbstractFoosballAgent {

    public void performAction() {
        if (gameState.getPlayerThatScored() == null) {
            List<Integer> intendedYPositions = new ArrayList<Integer>();
            List<PlayerAngle> intendedPlayerAngles = new ArrayList<PlayerAngle>();
            intendedPlayerAngles.add(gameState.getPlayerAngleForRow(0));
            intendedPlayerAngles.add(gameState.getPlayerAngleForRow(2));
            intendedYPositions.add(gameState.getRowYPosition(0));
            intendedYPositions.add(gameState.getRowYPosition(2));
            // System.out.println("YShit=" + intendedPlayerAngles);
            usbWriter.setPlayerPositions(intendedYPositions, intendedPlayerAngles);
        } else {
            List<Integer> intendedYPositions = new ArrayList<Integer>();
            List<PlayerAngle> intendedPlayerAngles = new ArrayList<PlayerAngle>();
            intendedPlayerAngles.add(PlayerAngle.VERTICAL);
            intendedPlayerAngles.add(PlayerAngle.VERTICAL);
            intendedYPositions.add(180);
            intendedYPositions.add(180);
            usbWriter.setPlayerPositions(intendedYPositions, intendedPlayerAngles);
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
    }
}
