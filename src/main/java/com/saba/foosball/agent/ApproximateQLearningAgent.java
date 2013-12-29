package com.saba.foosball.agent;

import java.io.Console;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.saba.foosball.model.Player;
import com.saba.foosball.model.PlayerAngle;

/*
 * This agent will mirror what the opposing player does
 */
public class ApproximateQLearningAgent extends AbstractFoosballAgent {
    private List<Float> featureWeights = new ArrayList<Float>(Arrays.asList(new Float[] { 1f, 1f, 1f, 1f, 1f, 1f, 1f }));
    // Learning Rate
    private float alpha = .8f;
    // Percent Chance for Random Action
    private float epsilon = .3f;
    // Discount Factor
    private float gamma = .8f;

    // Previous Data
    private float prevQVal = 0;
    private List<Float> prevFeatureVals = new ArrayList<Float>(Arrays.asList(new Float[] { 0f, 0f, 0f, 0f, 0f, 0f, 0f }));

    public void performAction() {
        this.updateWeights();
        // Take Action IFF no one has scored
        if (gameState.getPlayerThatScored() == null)
            this.takeAction();
        else {
            // Prompt User For New Round
            System.out.println("GOAL " + gameState.getPlayerThatScored() + "! Weights: " + featureWeights);
            Console console = System.console();
            console.readLine("Place the ball in the center and hit any key!");
            gameState.restartRound();
        }
    }

    private void takeAction() {
        List<Integer> intendedYPositions = new ArrayList<Integer>(2);
        List<PlayerAngle> intendedPlayerAngles = new ArrayList<PlayerAngle>(2);
        if (Math.random() < epsilon) {
            // Random Actions
            intendedPlayerAngles.add(PlayerAngle.getRandomAngle());
            intendedPlayerAngles.add(PlayerAngle.getRandomAngle());
            intendedYPositions.add((int) (Math.random() * 41));
            intendedYPositions.add((int) (Math.random() * 41));
            // Get QVal for this random action
            float qVal = 1;
            List<Float> featureVals = this.getFeatureValuesCurrentStateAndActions(intendedYPositions.get(0), intendedYPositions.get(1),
                    intendedPlayerAngles.get(0), intendedPlayerAngles.get(1));
            for (int i = 0; i < featureVals.size(); i++) {
                qVal += featureWeights.get(i) * featureVals.get(i);
            }
            prevFeatureVals = featureVals;
            prevQVal = qVal;
        } else {
            float maxQVal = Float.MIN_VALUE;
            List<Float> featureVals = null;
            // Max over all possible actions (40,000)
            for (int rowZeroYPos = 0; rowZeroYPos < 41; rowZeroYPos++) {
                for (int rowOneYPos = 0; rowOneYPos < 41; rowOneYPos++) {
                    for (PlayerAngle rowZeroAngle : PlayerAngle.values()) {
                        for (PlayerAngle rowOneAngle : PlayerAngle.values()) {
                            int qVal = 1;
                            featureVals = this.getFeatureValuesCurrentStateAndActions(rowZeroYPos, rowOneYPos, rowZeroAngle, rowOneAngle);
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
    }

    private void updateWeights() {
        if (gameState.getPlayerThatScored() == null) {
            int maxQVal = Integer.MIN_VALUE;
            // Max over all possible actions (40,000)
            for (int rowZeroYPos = 0; rowZeroYPos < 41; rowZeroYPos++) {
                for (int rowOneYPos = 0; rowOneYPos < 41; rowOneYPos++) {
                    for (PlayerAngle rowZeroAngle : PlayerAngle.values()) {
                        for (PlayerAngle rowOneAngle : PlayerAngle.values()) {
                            int qVal = 1;
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
            for (int i = 0; i < featureWeights.size(); i++) {
                featureWeights.add(i, featureWeights.get(i) + alpha * diff * prevFeatureVals.get(i));
            }
        } else {
            float diff = this.getRewardForCurrentState() - prevQVal;
            for (int i = 0; i < featureWeights.size(); i++) {
                featureWeights.add(i, featureWeights.get(i) + alpha * diff * prevFeatureVals.get(i));
            }
        }
    }

    private List<Float> getFeatureValuesCurrentStateAndActions(int rowZeroYPos, int rowOneYPos, PlayerAngle rowZeroAngle, PlayerAngle rowOneAngle) {
        List<Float> featureValues = new ArrayList<Float>(featureWeights.size());
        // Feature 1 -

        // Feature 2

        return featureValues;
    }

    private float getRewardForCurrentState() {
        Player playerThatScored = gameState.getPlayerThatScored();
        if (playerThatScored == null) {
            return -.1f;
        } else if (playerThatScored == Player.SELF) {
            return 1000;
        } else {
            return -1000;
        }
    }
}
