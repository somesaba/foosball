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

    private List<Float> featureWeights = new ArrayList<Float>(Arrays.asList(new Float[] { 1f, 1f, 1f }));
    // Learning Rate
    private float alpha = .3f;
    // Percent Chance for Random Action
    private float epsilon = .1f;
    // Discount Factor
    private float gamma = .9f;

    // Previous Data
    private float prevQVal = 0;
    private List<Float> prevFeatureVals = new ArrayList<Float>(Arrays.asList(new Float[] { 0f, 0f, 0f }));

    public void performAction() {
        this.updateWeights();
        // Take Action IFF no one has scored
        if (gameState.getPlayerThatScored() == null)
            this.takeAction();
        else {
            this.resetRound();
        }
    }

    private void takeAction() {
        List<Integer> intendedYPositions = new ArrayList<Integer>(2);
        List<PlayerAngle> intendedPlayerAngles = new ArrayList<PlayerAngle>(2);
        if (Math.random() < epsilon) {
            // Random Actions
            intendedPlayerAngles.add(PlayerAngle.getRandomLimitedAngle());
            intendedPlayerAngles.add(PlayerAngle.getRandomLimitedAngle());
            intendedYPositions.add((int) (Math.random() * 40));
            intendedYPositions.add((int) (Math.random() * 40));
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
            for (int rowZeroYPos = 0; rowZeroYPos < 40; rowZeroYPos++) {
                for (int rowOneYPos = 0; rowOneYPos < 40; rowOneYPos++) {
                    for (PlayerAngle rowZeroAngle : PlayerAngle.getLimitedValues()) {
                        for (PlayerAngle rowOneAngle : PlayerAngle.getLimitedValues()) {
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
            for (int rowZeroYPos = 0; rowZeroYPos < 40; rowZeroYPos++) {
                for (int rowOneYPos = 0; rowOneYPos < 40; rowOneYPos++) {
                    for (PlayerAngle rowZeroAngle : PlayerAngle.getLimitedValues()) {
                        for (PlayerAngle rowOneAngle : PlayerAngle.getLimitedValues()) {
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

        int yPositionOfClosestRowOnePlayer = adjustedRowOneYPos;
        int yPositionOfClosestRowThreePlayer = adjustedRowThreeYPos;
        // Feature 1 - Actions that get the Y Pos of the players in line with the ball get a higher score
        int rowOneDistanceOfPlayerToBall = Integer.MAX_VALUE;
        for (int i = 0; i < gameState.getNumbersOfPlayersForRow(1); i++) {
            int yPositionOfCurrentPlayer = adjustedRowOneYPos - gameState.getDistanceBetweenPlayersForRow(1) * i;
            int currentRowsYDistanceToBall = Math.abs(yPosOfBall - yPositionOfCurrentPlayer);
            if (currentRowsYDistanceToBall < rowOneDistanceOfPlayerToBall) {
                rowOneDistanceOfPlayerToBall = currentRowsYDistanceToBall;
                yPositionOfClosestRowOnePlayer = yPositionOfCurrentPlayer;
            }
        }
        int rowThreeDistanceOfPlayerToBall = Integer.MAX_VALUE;
        for (int i = 0; i < gameState.getNumbersOfPlayersForRow(3); i++) {
            int yPositionOfCurrentPlayer = adjustedRowThreeYPos - gameState.getDistanceBetweenPlayersForRow(3) * i;
            int currentRowsYDistanceToBall = Math.abs(yPosOfBall - yPositionOfCurrentPlayer);
            if (currentRowsYDistanceToBall < rowThreeDistanceOfPlayerToBall) {
                rowThreeDistanceOfPlayerToBall = currentRowsYDistanceToBall;
                yPositionOfClosestRowThreePlayer = yPositionOfCurrentPlayer;
            }
        }
        // Accurate to 5pixels
        rowOneDistanceOfPlayerToBall /= 2;
        rowThreeDistanceOfPlayerToBall /= 2;
        if (rowOneDistanceOfPlayerToBall >= gameState.getMaxY() / 4) {
            rowOneDistanceOfPlayerToBall = gameState.getMaxY() / 4 - 1;
        }
        if (rowThreeDistanceOfPlayerToBall >= gameState.getMaxY() / 4) {
            rowThreeDistanceOfPlayerToBall = gameState.getMaxY() / 4 - 1;
        }
        featureValues.add((float) (((gameState.getMaxY() / 4) - (rowOneDistanceOfPlayerToBall + rowThreeDistanceOfPlayerToBall)))
                / (gameState.getMaxY() / 4));

        // Feature 2 If Ball is front of you score actions that might hit it higher
        if ((xPosOfBall <= gameState.getRowXPosition(1) && xPosOfBall > gameState.getRowXPosition(1) - 80)
                && ((gameState.getPlayerAngleForRow(1) == PlayerAngle.VERTICAL && rowOneAngle == PlayerAngle.FORWARD_ANGLED) || (gameState
                        .getPlayerAngleForRow(1) == PlayerAngle.BACKWARD_ANGLED && (rowOneAngle == PlayerAngle.FORWARD_ANGLED || rowOneAngle == PlayerAngle.VERTICAL)))) {
            featureValues.add(1f);
        } else if ((xPosOfBall <= gameState.getRowXPosition(3) && xPosOfBall > gameState.getRowXPosition(3) - 80)
                && ((gameState.getPlayerAngleForRow(3) == PlayerAngle.VERTICAL && rowThreeAngle == PlayerAngle.FORWARD_ANGLED) || (gameState
                        .getPlayerAngleForRow(3) == PlayerAngle.BACKWARD_ANGLED && (rowThreeAngle == PlayerAngle.FORWARD_ANGLED || rowThreeAngle == PlayerAngle.VERTICAL)))) {
            featureValues.add(1f);
        } else {
            featureValues.add(0f);
        }
        // Feature 3 If ball is behind you score actions that might move it in the y axis
        if ((xPosOfBall > gameState.getRowXPosition(1) && xPosOfBall < gameState.getRowXPosition(1) + 80)
                && (rowOneAngle == PlayerAngle.BACKWARD_ANGLED && (yPositionOfClosestRowOnePlayer > yPosOfBall + 5 || yPositionOfClosestRowOnePlayer < yPosOfBall - 5))) {
            featureValues.add(1f);
        } else if ((xPosOfBall > gameState.getRowXPosition(3) && xPosOfBall < gameState.getRowXPosition(3) + 80)
                && (rowThreeAngle == PlayerAngle.BACKWARD_ANGLED && (yPositionOfClosestRowThreePlayer > yPosOfBall + 5 || yPositionOfClosestRowThreePlayer < yPosOfBall - 5))) {
            featureValues.add(1f);
        } else {
            featureValues.add(0f);
        }
        return featureValues;
    }

    private float getRewardForCurrentState() {
        Player playerThatScored = gameState.getPlayerThatScored();
        if (playerThatScored == null) {
            int ballXPos = gameState.getBallXPosition();
            int prevBallXPos = gameState.getPrevBallPosition().x;
            // Time Delay of -1
            int reward = -1;
            // Score of +5 if ball is on left half -5 other wise
            if (ballXPos < gameState.getMaxX() / 2) {
                reward += 4;
            } else {
                reward -= 4;
            }
            // Score of +6 is ball is moving toward enemy goal -6 if it is
            if (ballXPos < prevBallXPos) {
                reward += 6;
            } else {
                reward -= 6;
            }
            return reward;
        } else if (playerThatScored == Player.SELF) {
            return 1000;
        } else {
            return -1000;
        }
    }
}
