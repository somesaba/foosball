package com.saba.foosball.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.saba.foosball.model.Player;
import com.saba.foosball.model.PlayerAngle;

/*
 * This agent will mirror what the opposing player does
 */
public class ApproximateQLearningAgent extends AbstractFoosballAgent {
    private static final double yPositionToByteFactor = 45d / 70d;

    private List<Float> featureWeights = new ArrayList<Float>(Arrays.asList(new Float[] { 1f, 1f, 1f, 1f }));
    // Learning Rate
    private float alpha = .3f;
    // Percent Chance for Random Action
    private float epsilon = .3f;
    // Discount Factor
    private float gamma = .9f;

    // Previous Data
    private float prevQVal = 0;
    private List<Float> prevFeatureVals = new ArrayList<Float>(Arrays.asList(new Float[] { 0f, 0f, 0f, 0f }));

    public void performAction() {
        this.updateWeights();
        // Take Action IFF no one has scored
        if (gameState.getPlayerThatScored() == null)
            this.takeAction();
        else {
            this.resetPlayers();
            // Prompt User For New Round
            System.out.println("GOAL " + gameState.getPlayerThatScored() + "! Weights: " + featureWeights);
            System.out.print("Round Restarts in...");
            for (int i = 10; i > 0; i--) {
                System.out.print(i + "..");
                try {
                    // Toolkit.getDefaultToolkit().beep();
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println("0");
            gameState.restartRound();
        }
    }

    private void resetPlayers() {
        List<Integer> intendedYPositions = new ArrayList<Integer>(2);
        List<PlayerAngle> intendedPlayerAngles = new ArrayList<PlayerAngle>(2);
        intendedPlayerAngles.add(PlayerAngle.VERTICAL);
        intendedPlayerAngles.add(PlayerAngle.VERTICAL);
        intendedYPositions.add(20);
        intendedYPositions.add(20);
        usbWriter.setHardPlayerPositions(intendedYPositions, intendedPlayerAngles);
    }

    private void takeAction() {
        List<Integer> intendedYPositions = new ArrayList<Integer>(2);
        List<PlayerAngle> intendedPlayerAngles = new ArrayList<PlayerAngle>(2);
        if (Math.random() < epsilon) {
            // Random Actions
            intendedPlayerAngles.add(PlayerAngle.getRandomAngle());
            intendedPlayerAngles.add(PlayerAngle.getRandomAngle());
            intendedYPositions.add((int) (Math.random() * 43));
            intendedYPositions.add((int) (Math.random() * 43));
            // Get QVal for this random action
            float qVal = 0;
            List<Float> featureVals = this.getFeatureValuesCurrentStateAndActions(intendedYPositions.get(0), intendedYPositions.get(1),
                    intendedPlayerAngles.get(0), intendedPlayerAngles.get(1));
            for (int i = 0; i < featureVals.size(); i++) {
                qVal += featureWeights.get(i) * featureVals.get(i);
            }
            prevFeatureVals = featureVals;
            prevQVal = qVal;
            System.out.println("ACTION Random Feature Vals=" + featureVals + "\t Weights" + this.featureWeights);
        } else {
            float maxQVal = -Float.MAX_VALUE;
            List<Float> featureVals = null;
            List<Float> bestFeatureVals = null;
            intendedPlayerAngles.add(PlayerAngle.VERTICAL);
            intendedPlayerAngles.add(PlayerAngle.VERTICAL);
            intendedYPositions.add(20);
            intendedYPositions.add(20);
            // Max over all possible actions (40,000)
            for (int rowZeroYPos = 0; rowZeroYPos < 43; rowZeroYPos++) {
                for (int rowOneYPos = 0; rowOneYPos < 43; rowOneYPos++) {
                    for (PlayerAngle rowZeroAngle : PlayerAngle.values()) {
                        for (PlayerAngle rowOneAngle : PlayerAngle.values()) {
                            float qVal = 0;
                            featureVals = this.getFeatureValuesCurrentStateAndActions(rowZeroYPos, rowOneYPos, rowZeroAngle, rowOneAngle);
                            for (int i = 0; i < featureVals.size(); i++) {
                                qVal += featureWeights.get(i) * featureVals.get(i);
                            }
                            if (qVal > maxQVal) {
                                maxQVal = qVal;
                                bestFeatureVals = featureVals;
                                intendedPlayerAngles.set(0, rowZeroAngle);
                                intendedPlayerAngles.set(1, rowOneAngle);
                                intendedYPositions.set(0, rowZeroYPos);
                                intendedYPositions.set(1, rowOneYPos);
                            }
                        }
                    }
                }
            }
            prevFeatureVals = bestFeatureVals;
            prevQVal = maxQVal;
            System.out.println("ACTION Best Feature Vals=" + bestFeatureVals + "\t Weights" + this.featureWeights);
            if (bestFeatureVals == null) {
                float qVal = 0;
                featureVals = this.getFeatureValuesCurrentStateAndActions(20, 20, PlayerAngle.VERTICAL, PlayerAngle.VERTICAL);
                for (int i = 0; i < featureVals.size(); i++) {
                    qVal += featureWeights.get(i) * featureVals.get(i);
                }
                System.out.println("QVal for null=" + qVal + "\t features=" + featureVals);
            }
        }
        usbWriter.setHardPlayerPositions(intendedYPositions, intendedPlayerAngles);
    }

    private void updateWeights() {
        if (gameState.getPlayerThatScored() == null) {
            float maxQVal = -Float.MAX_VALUE;
            // Max over all possible actions (40,000)
            for (int rowZeroYPos = 0; rowZeroYPos < 43; rowZeroYPos++) {
                for (int rowOneYPos = 0; rowOneYPos < 43; rowOneYPos++) {
                    for (PlayerAngle rowZeroAngle : PlayerAngle.values()) {
                        for (PlayerAngle rowOneAngle : PlayerAngle.values()) {
                            float qVal = 0;
                            List<Float> featureVals = this.getFeatureValuesCurrentStateAndActions(rowZeroYPos, rowOneYPos, rowZeroAngle, rowOneAngle);
                            for (int i = 0; i < featureVals.size(); i++) {
                                qVal += featureWeights.get(i) * featureVals.get(i);
                            }
                            if (qVal > maxQVal) {
                                maxQVal = qVal;
                            }
                        }
                    }
                }
            }
            float diff = this.getRewardForCurrentState() + gamma * maxQVal - prevQVal;
            System.out.println("UPDATE WEIGHTS diff=" + diff + "\tbestQVal=" + maxQVal + "\tprevQ" + prevQVal + "\tprevFeatures=" + prevFeatureVals);
            for (int i = 0; i < featureWeights.size(); i++) {
                featureWeights.set(i, featureWeights.get(i) + alpha * diff * prevFeatureVals.get(i));
            }
            System.out.println("UPDATE WEIGHTS New Weights=" + featureWeights);
        } else {
            float diff = this.getRewardForCurrentState() - prevQVal;
            for (int i = 0; i < featureWeights.size(); i++) {
                featureWeights.set(i, featureWeights.get(i) + alpha * diff * prevFeatureVals.get(i));
            }
        }
    }

    /**
     * ALL FEATURES MUST BE < 1 otherwise the update will make them wildy diverge
     * 
     * @param rowOneYPos
     * @param rowThreeYPos
     * @param rowOneAngle
     * @param rowThreeAngle
     * @return
     */
    private List<Float> getFeatureValuesCurrentStateAndActions(int rowOneYPos, int rowThreeYPos, PlayerAngle rowOneAngle, PlayerAngle rowThreeAngle) {
        int adjustedRowOneYPos = (int) ((rowOneYPos / yPositionToByteFactor) + 152);
        int adjustedRowThreeYPos = (int) (-(rowThreeYPos / yPositionToByteFactor) + 223);
        int yPosOfBall = gameState.getBallYPosition();
        int xPosOfBall = gameState.getBallXPosition();
        List<Float> featureValues = new ArrayList<Float>(featureWeights.size());

        // Feature 1 - Actions that get the Y Pos of the players in line with the ball get a higher score
        int rowOneDistanceOfPlayerToBall = Integer.MAX_VALUE;
        for (int i = 0; i < gameState.getNumbersOfPlayersForRow(1); i++) {
            rowOneDistanceOfPlayerToBall = Math.min(rowOneDistanceOfPlayerToBall,
                    Math.abs(yPosOfBall - (adjustedRowOneYPos - gameState.getDistanceBetweenPlayersForRow(1) * i)));
        }
        int rowThreeDistanceOfPlayerToBall = Integer.MAX_VALUE;
        for (int i = 0; i < gameState.getNumbersOfPlayersForRow(3); i++) {
            rowThreeDistanceOfPlayerToBall = Math.min(rowThreeDistanceOfPlayerToBall,
                    Math.abs(yPosOfBall - (adjustedRowThreeYPos - gameState.getDistanceBetweenPlayersForRow(3) * i)));
        }
        // Accurate to 10pixels
        rowOneDistanceOfPlayerToBall /= 10;
        rowThreeDistanceOfPlayerToBall /= 10;
        if (rowOneDistanceOfPlayerToBall >= gameState.getMaxY() / 20) {
            rowOneDistanceOfPlayerToBall = gameState.getMaxY() / 20 - 1;
        }
        if (rowThreeDistanceOfPlayerToBall >= gameState.getMaxY() / 20) {
            rowThreeDistanceOfPlayerToBall = gameState.getMaxY() / 20 - 1;
        }
        featureValues.add((float) ((gameState.getMaxY() - (rowOneDistanceOfPlayerToBall + rowThreeDistanceOfPlayerToBall))) / gameState.getMaxY());

        // Feature 2 Row angle changes on players near ball (Active IFF ball is within 80 points)
        // Higher score for row Angle changes on players near ball
        if ((xPosOfBall < gameState.getRowXPosition(1) + 80 && xPosOfBall > gameState.getRowXPosition(1) - 80)
                && gameState.getPlayerAngleForRow(1) != rowOneAngle) {
            featureValues.add(1f);
        } else if ((xPosOfBall < gameState.getRowXPosition(3) + 80 && xPosOfBall > gameState.getRowXPosition(3) - 80)
                && gameState.getPlayerAngleForRow(3) != rowThreeAngle) {
            featureValues.add(1f);
        } else {
            featureValues.add(0f);
        }
        // Feature 3 Row angle down on players without ball
        featureValues.add(0f);
        if ((xPosOfBall > gameState.getRowXPosition(1) + 80 || xPosOfBall < gameState.getRowXPosition(1) - 80)
                && gameState.getPlayerAngleForRow(1) == PlayerAngle.VERTICAL) {
            featureValues.set(2, .5f);
        }
        if ((xPosOfBall > gameState.getRowXPosition(3) + 80 || xPosOfBall < gameState.getRowXPosition(3) - 80)
                && gameState.getPlayerAngleForRow(3) == PlayerAngle.VERTICAL) {
            featureValues.set(2, featureValues.get(2) + .5f);
        }
        // Feature 4 penalize being up on defence
        if (rowThreeAngle == PlayerAngle.BACKWARD_HORIZONTAL || rowThreeAngle == PlayerAngle.FORWARD_HORIZONTAL) {
            featureValues.add(.01f);
        } else {
            featureValues.add(1f);
        }
        return featureValues;
    }

    private float getRewardForCurrentState() {
        Player playerThatScored = gameState.getPlayerThatScored();
        if (playerThatScored == null) {
            return .1f;
        } else if (playerThatScored == Player.SELF) {
            return 1000;
        } else {
            return -1000;
        }
    }
}
