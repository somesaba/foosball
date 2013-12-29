package com.saba.foosball.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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
            Scanner reader = new Scanner(System.in);
            System.out.println("Place the ball in the center and hit any key!");
            reader.next();
            reader.close();
            gameState.restartRound();
        }
    }
}
