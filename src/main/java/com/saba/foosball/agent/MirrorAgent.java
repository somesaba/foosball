package com.saba.foosball.agent;

import java.util.ArrayList;
import java.util.List;

import com.saba.foosball.model.PlayerAngle;

/*
 * This agent will mirror what the opposing player does
 */
public class MirrorAgent extends AbstractFoosballAgent {

    public void notifyGameStateUpdate() {
        List<Integer> intendedYPositions = new ArrayList<Integer>();
        List<PlayerAngle> intendedPlayerAngles = new ArrayList<PlayerAngle>();
        intendedPlayerAngles.add(gameState.getPlayerAngleForRow(0));
        intendedPlayerAngles.add(gameState.getPlayerAngleForRow(2));
        intendedYPositions.add(gameState.getRowYPosition(0));
        intendedYPositions.add(gameState.getRowYPosition(2));
        // System.out.println("YShit=" + intendedPlayerAngles);
        usbWriter.setPlayerPositions(intendedYPositions, intendedPlayerAngles);
    }
}
