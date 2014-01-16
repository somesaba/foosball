package com.saba.foosball.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.saba.foosball.model.Action;
import com.saba.foosball.model.Player;
import com.saba.foosball.model.PlayerAngle;

/*
 * This agent will mirror what the opposing player does
 */
public class ApproximateQLearningAgent extends AbstractFoosballAgent {
    private static final double yPositionToByteFactor = 40d / 75d;

    private List<Float> featureWeights = new ArrayList<Float>(Arrays.asList(new Float[] { 1f, 1f, 1f, 1f }));
    // Learning Rate
    private float alpha = .3f;
    // Percent Chance for Random Action
    private float epsilon = .1f;
    // Discount Factor
    private float gamma = .9f;

    // Previous Data
    private float prevQVal = 0;
    private List<Float> prevFeatureVals = new ArrayList<Float>(Arrays.asList(new Float[] { 0f, 0f, 0f, 0f }));

    public void performAction() {
        this.updateWeights();
        // Take Action if no one has scored
        if (gameState.getPlayerThatScored() == null)
            this.takeAction();
        else {
            this.resetRound();
        }
    }

    private void takeAction() {

        if (Math.random() < epsilon) {
            // Random Actions
            List<Integer> intendedYPositions = new ArrayList<Integer>(2);
            List<PlayerAngle> intendedPlayerAngles = new ArrayList<PlayerAngle>(2);
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
            usbWriter.setHardPlayerPositions(intendedYPositions, intendedPlayerAngles);
        } else {

            float maxQVal = -Float.MAX_VALUE;
            // List<Float> featureVals = null;
            List<List<Float>> bestFeatureVals = new ArrayList<List<Float>>();
            List<Action> bestActions = new ArrayList<Action>();
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
                                // Clear Lists
                                bestFeatureVals.clear();
                                bestActions.clear();

                                List<Integer> intendedYPositions = new ArrayList<Integer>(2);
                                List<PlayerAngle> intendedPlayerAngles = new ArrayList<PlayerAngle>(2);
                                intendedPlayerAngles.add(rowZeroAngle);
                                intendedPlayerAngles.add(rowOneAngle);
                                intendedYPositions.add(rowZeroYPos);
                                intendedYPositions.add(rowOneYPos);

                                // Add to lists
                                bestFeatureVals.add(featureVals);
                                bestActions.add(new Action(intendedYPositions, intendedPlayerAngles));
                            } else if (qVal == maxQVal) {
                                List<Integer> intendedYPositions = new ArrayList<Integer>(2);
                                List<PlayerAngle> intendedPlayerAngles = new ArrayList<PlayerAngle>(2);
                                intendedPlayerAngles.add(rowZeroAngle);
                                intendedPlayerAngles.add(rowOneAngle);
                                intendedYPositions.add(rowZeroYPos);
                                intendedYPositions.add(rowOneYPos);

                                // Add to lists
                                bestFeatureVals.add(featureVals);
                                bestActions.add(new Action(intendedYPositions, intendedPlayerAngles));
                            }
                        }
                    }
                }
            }
            int randomIndex = (int) (bestActions.size() * Math.random());
            prevFeatureVals = bestFeatureVals.get(randomIndex);
            prevQVal = maxQVal;
            System.out.println("ACTION Best of size=" + bestActions.size() + " Feature Vals=" + prevFeatureVals + "\t Weights" + this.featureWeights);
            // if (bestFeatureVals == null) {
            // float qVal = 0;
            // featureVals = this.getFeatureValuesCurrentStateAndActions(20, 20, PlayerAngle.VERTICAL,
            // PlayerAngle.VERTICAL);
            // for (int i = 0; i < featureVals.size(); i++) {
            // qVal += featureWeights.get(i) * featureVals.get(i);
            // }
            // System.out.println("QVal for null=" + qVal + "\t features=" + featureVals);
            // }
            usbWriter.setHardPlayerPositions(bestActions.get(randomIndex).getIntendedYPositions(), bestActions.get(randomIndex)
                    .getIntendedPlayerAngles());

        }
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
        int yPosOfBall = gameState.getBallYPosition() / 3;
        int xPosOfBall = gameState.getBallXPosition();
        List<Float> featureValues = new ArrayList<Float>(featureWeights.size());

        // Feature 1 - Actions that get the Y Pos of a player in line with the ball get a higher score
        featureValues.add(.5f);
        boolean willRowOneBeNearTheBall = false;
        for (int player = 0; player < gameState.getNumbersOfPlayersForRow(0); player++) {
            int yPosOfPlayer = (adjustedRowOneYPos - player * gameState.getDistanceBetweenPlayersForRow(0)) / 3;
            if (yPosOfBall == yPosOfPlayer) {
                if (rowOneAngle == PlayerAngle.VERTICAL) {
                    featureValues.set(0, featureValues.get(0) + .5f);
                }
                willRowOneBeNearTheBall = true;
                break;
            }
        }
        boolean willRowThreeBeNearTheBall = false;
        for (int player = 0; player < gameState.getNumbersOfPlayersForRow(3); player++) {
            int yPosOfPlayer = (adjustedRowThreeYPos - player * gameState.getDistanceBetweenPlayersForRow(3)) / 3;
            if (yPosOfBall == yPosOfPlayer) {
                if (rowThreeAngle == PlayerAngle.VERTICAL) {
                    featureValues.set(0, featureValues.get(0) + .5f);
                }
                willRowThreeBeNearTheBall = true;
                break;
            }
        }
        // Feature 2 If Ball is front of you score actions that might hit it forward
        if (this.isBallInFrontAndWithinReachOfRow(0, xPosOfBall)
                && ((gameState.getPlayerAngleForRow(1) == PlayerAngle.VERTICAL && rowOneAngle == PlayerAngle.FORWARD_ANGLED) || (gameState
                        .getPlayerAngleForRow(1) == PlayerAngle.BACKWARD_ANGLED && (rowOneAngle == PlayerAngle.FORWARD_ANGLED || rowOneAngle == PlayerAngle.VERTICAL)))) {
            featureValues.add(1f);
        } else if (this.isBallInFrontAndWithinReachOfRow(3, xPosOfBall)
                && ((gameState.getPlayerAngleForRow(3) == PlayerAngle.VERTICAL && rowThreeAngle == PlayerAngle.FORWARD_ANGLED) || (gameState
                        .getPlayerAngleForRow(3) == PlayerAngle.BACKWARD_ANGLED && (rowThreeAngle == PlayerAngle.FORWARD_ANGLED || rowThreeAngle == PlayerAngle.VERTICAL)))) {
            featureValues.add(1f);
        } else {
            featureValues.add(0f);
        }
        // Feature 3 If ball is behind row 0 score actions that might move it in the y axis
        if (this.isBallInBehindAndWithinReachOfRow(0, xPosOfBall)
                && (rowOneAngle == PlayerAngle.BACKWARD_ANGLED || rowOneAngle == PlayerAngle.BACKWARD_HORIZONTAL) && !willRowOneBeNearTheBall) {
            featureValues.add(1f);
        } else {
            featureValues.add(0f);
        }
        // Feature 4 if ball is behind row 3
        if (this.isBallInBehindAndWithinReachOfRow(3, xPosOfBall)
                && (rowThreeAngle == PlayerAngle.BACKWARD_ANGLED || rowThreeAngle == PlayerAngle.BACKWARD_HORIZONTAL) && !willRowThreeBeNearTheBall) {
            featureValues.add(1f);
        } else {
            featureValues.add(0f);
        }
        return featureValues;
    }

    private float getRewardForCurrentState() {
        Player playerThatScored = gameState.getPlayerThatScored();
        if (playerThatScored == null) {
            // Game is still live
            int prevBallXPos = gameState.getPrevBallPosition().x;
            int ballXPos = gameState.getBallXPosition();
            int yPositionOfRowThree = gameState.getRowYPosition(3);
            int ballYPos = gameState.getBallYPosition() / 3;
            if (this.isBallWithinReachOfRow(1, prevBallXPos) || this.isBallWithinReachOfRow(3, prevBallXPos)) {
                // Ball was within a player's control
                // Time delay
                // PotentialPositionRectangle currentBallPosRect = new PotentialPositionRectangle(ballXPos,
                // gameState.getBallYPosition());
                // PotentialPositionRectangle prevBallPosRect = new PotentialPositionRectangle(prevBallXPos,
                // gameState.getPrevBallPosition().y);
                // if (currentBallPosRect.doesCollide(prevBallPosRect)) {
                // return 0;
                // }
                if (ballXPos < prevBallXPos) {
                    return 10;
                }
                if (ballXPos > prevBallXPos && ballXPos > gameState.getMaxX() / 2) {
                    for (int player = 0; player < gameState.getNumbersOfPlayersForRow(3); player++) {
                        int yPosOfPlayer = (yPositionOfRowThree - player * gameState.getDistanceBetweenPlayersForRow(3)) / 3;
                        if (ballYPos == yPosOfPlayer) {
                            return 10;
                        }
                    }
                }
                return 0;
            } else {
                // No rewards if the ball is not within the player's control
                return 0;
            }
        } else if (playerThatScored == Player.SELF) {
            return 1000;
        } else {
            return -1000;
        }
    }

    private boolean isBallInFrontAndWithinReachOfRow(int row, int xPosOfBall) {
        return (xPosOfBall <= gameState.getRowXPosition(row) && xPosOfBall > gameState.getRowXPosition(row) - 30);
    }

    private boolean isBallInBehindAndWithinReachOfRow(int row, int xPosOfBall) {
        return (xPosOfBall > gameState.getRowXPosition(row) && xPosOfBall < gameState.getRowXPosition(row) + 30);
    }

    private boolean isBallWithinReachOfRow(int row, int xPosOfBall) {
        return this.isBallInFrontAndWithinReachOfRow(row, xPosOfBall) || this.isBallInBehindAndWithinReachOfRow(row, xPosOfBall);
    }
}
