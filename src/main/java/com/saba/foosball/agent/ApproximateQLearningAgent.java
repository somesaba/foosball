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
    private boolean isReadyForNewState = true;
    private List<Float> featureWeights = new ArrayList<Float>(Arrays.asList(new Float[] { 1f, 1f, 1f, 1f, 1f, 1f, 1f }));
    // Learning Rate
    private float alpha = .8f;
    // Percent Chance for Random Action
    private float epsilon = .3f;
    // Discount Factor
    private float gamma = .8f;

    // Previous Data
    private float prevQVal = 0;
    private List<Integer> prevFeatureVals = new ArrayList<Integer>(Arrays.asList(new Integer[] { 0, 0, 0, 0, 0, 0, 0 }));

    // Method Must not block!
    public void notifyGameStateUpdate() {
        if (!isReadyForNewState) {
            return;
        } else {
            isReadyForNewState = false;
            // Register difference
            // Take new action
        }
    }

    public void takeAction() {
        List<Integer> intendedYPositions = new ArrayList<Integer>(2);
        List<PlayerAngle> intendedPlayerAngles = new ArrayList<PlayerAngle>(2);
        if (Math.random() < epsilon) {
            // Random Actions
            intendedPlayerAngles.add(PlayerAngle.getRandomAngle());
            intendedPlayerAngles.add(PlayerAngle.getRandomAngle());
            intendedYPositions.add((int) (Math.random() * 41));
            intendedYPositions.add((int) (Math.random() * 41));
            // Get QVal for this random action
            int qVal = 1;
            List<Integer> featureVals = this.getFeatureValuesCurrentStateAndActions(intendedYPositions.get(0),
                    intendedYPositions.get(1), intendedPlayerAngles.get(0), intendedPlayerAngles.get(1));
            for (int i = 0; i < featureVals.size(); i++) {
                qVal += featureWeights.get(i) * featureVals.get(i);
            }
            prevFeatureVals = featureVals;
            prevQVal = qVal;
        } else {
            float maxQVal = Float.MIN_VALUE;
            List<Integer> featureVals = null;
            // Max over all possible actions (40,000)
            for (int rowZeroYPos = 0; rowZeroYPos < 41; rowZeroYPos++) {
                for (int rowOneYPos = 0; rowOneYPos < 41; rowOneYPos++) {
                    for (PlayerAngle rowZeroAngle : PlayerAngle.values()) {
                        for (PlayerAngle rowOneAngle : PlayerAngle.values()) {
                            int qVal = 1;
                            featureVals = this.getFeatureValuesCurrentStateAndActions(rowZeroYPos, rowOneYPos,
                                    rowZeroAngle, rowOneAngle);
                            for (int i = 0; i < featureVals.size(); i++) {
                                qVal += featureWeights.get(i) * featureVals.get(i);
                            }
                            if (qVal > maxQVal) {
                                maxQVal = qVal;
                                intendedPlayerAngles.add(rowZeroAngle);
                                intendedPlayerAngles.add(rowOneAngle);
                                intendedYPositions.add(rowZeroYPos);
                                intendedYPositions.add(rowOneYPos);
                            }
                        }
                    }
                }
            }
            prevFeatureVals = featureVals;
            prevQVal = maxQVal;
        }
        usbWriter.setPlayerPositions(intendedYPositions, intendedPlayerAngles);

        // Signal Ready For New State
        isReadyForNewState = true;
    }

    public void updateWeights() {
        int maxQVal = Integer.MIN_VALUE;
        // Max over all possible actions (40,000)
        for (int rowZeroYPos = 0; rowZeroYPos < 41; rowZeroYPos++) {
            for (int rowOneYPos = 0; rowOneYPos < 41; rowOneYPos++) {
                for (PlayerAngle rowZeroAngle : PlayerAngle.values()) {
                    for (PlayerAngle rowOneAngle : PlayerAngle.values()) {
                        int qVal = 1;
                        List<Integer> featureVals = this.getFeatureValuesCurrentStateAndActions(rowZeroYPos,
                                rowOneYPos, rowZeroAngle, rowOneAngle);
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
        for (int i = 0; i < featureWeights.size(); i++) {
            featureWeights.add(i, featureWeights.get(i) + alpha * diff * prevFeatureVals.get(i));
        }
    }

    public List<Integer> getFeatureValuesCurrentStateAndActions(int rowZeroYPos, int rowOneYPos,
            PlayerAngle rowZeroAngle, PlayerAngle rowOneAngle) {
        List<Integer> featureValues = new ArrayList<Integer>(featureWeights.size());
        // Feature 1

        // Feature 2

        return featureValues;
    }

    public int getRewardForCurrentState() {
        Player playerThatScored = gameState.getPlayerThatScored();
        if (playerThatScored == null) {
            return 1;
        } else if (playerThatScored == Player.SELF) {
            System.out.println("WIN! Weights: " + featureWeights);
            return 500;
        } else {
            System.out.println("FAIL! Weights: " + featureWeights);
            return -500;
        }
    }
}
