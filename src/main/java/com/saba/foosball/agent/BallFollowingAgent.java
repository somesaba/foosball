package com.saba.foosball.agent;

import java.util.ArrayList;
import java.util.List;

import com.saba.foosball.model.PlayerAngle;

/*
 * This agent will mirror what the opposing player does
 */
public class BallFollowingAgent extends AbstractFoosballAgent {

    public void performAction() {
        if (gameState.getPlayerThatScored() == null) {
            List<Integer> intendedYPositions = new ArrayList<Integer>();
            List<PlayerAngle> intendedPlayerAngles = new ArrayList<PlayerAngle>();
            intendedPlayerAngles.add(PlayerAngle.FORWARD_ANGLED);
            intendedPlayerAngles.add(PlayerAngle.FORWARD_ANGLED);
            intendedYPositions.add(20);
            int yPosOfBall = gameState.getBallYPosition() / 3;
            for (int rowThreeYPos = 0; rowThreeYPos < 40; rowThreeYPos++) {
                int adjustedRowThreeYPos = (int) (-(rowThreeYPos / (40f / 75f)) + 226);
                for (int player = 0; player < gameState.getNumbersOfPlayersForRow(3); player++) {
                    int yPosOfPlayer = (adjustedRowThreeYPos - player * gameState.getDistanceBetweenPlayersForRow(3)) / 3;
                    System.out.println("Player=" + player + "\tyadj=" + adjustedRowThreeYPos + "\tPosOfPlayer=" + yPosOfPlayer + "\tBall="
                            + yPosOfBall);
                    if (player == 0) {
                        yPosOfPlayer -= 3;
                    }
                    if (player == 2) {
                        yPosOfPlayer += 3;
                    }
                    if (yPosOfBall == yPosOfPlayer) {
                        intendedYPositions.add(rowThreeYPos);
                        System.out.println("Math found player=" + player + "\tyPos=" + yPosOfPlayer + "\thard=" + rowThreeYPos);
                        break;
                    }
                }
            }
            if (intendedYPositions.size() == 1) {
                System.out.println("Error");
                intendedYPositions.add(20);

            }
            System.out.println("Ypos of ball=" + yPosOfBall + "\ty=" + intendedYPositions.get(1));
            usbWriter.setHardPlayerPositions(intendedYPositions, intendedPlayerAngles);
        } else {
            this.resetRound();
        }
    }

    public static void main(String[] args) {
        int yPosOfBall = 47;
        for (int rowThreeYPos = 0; rowThreeYPos < 40; rowThreeYPos++) {
            int adjustedRowThreeYPos = (int) (-(rowThreeYPos / (40f / 75f)) + 226);
            for (int player = 0; player < 3; player++) {
                int yPosOfPlayer = (adjustedRowThreeYPos - player * 68) / 3;
                System.out.println("Player=" + player + "\tyadj=" + adjustedRowThreeYPos + "\tPosOfPlayer=" + yPosOfPlayer + "\tBall=" + yPosOfBall);
                if (player == 0) {
                    yPosOfPlayer -= 3;
                }
                if (player == 2) {
                    yPosOfPlayer += 3;
                }
                if (yPosOfBall == yPosOfPlayer) {
                }
            }
        }
    }
}
