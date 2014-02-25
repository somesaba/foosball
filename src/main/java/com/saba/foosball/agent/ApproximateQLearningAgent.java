package com.saba.foosball.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.saba.foosball.model.Action;
import com.saba.foosball.model.DynamicGameState;
import com.saba.foosball.model.Player;
import com.saba.foosball.model.PlayerAngle;

/*
 * This agent will mirror what the opposing player does
 */
public class ApproximateQLearningAgent extends AbstractFoosballAgent {
    private static final double yPositionToByteFactor = 40d / 75d;
    private ExecutorService executor = Executors.newFixedThreadPool(3);
    private List<Float> featureWeights = new ArrayList<Float>(Arrays.asList(new Float[] { 1f, 1f, 1f, 1f, 1f }));
    // Learning Rate
    private float alpha = .3f;
    // Percent Chance for Random Action
    private float epsilon = .05f;
    // Discount Factor
    private float gamma = .9f;

    // Previous Data
    private float prevQVal = 0;
    private List<Float> prevFeatureVals = new ArrayList<Float>(Arrays.asList(new Float[] { 0f, 0f, 0f, 0f, 0f }));

    private DynamicGameState previousGameState;
    private List<Integer> previousIntendedYPositions;
    private List<PlayerAngle> previousIntendedPlayerAngles = Arrays.asList(new PlayerAngle[] { PlayerAngle.VERTICAL, PlayerAngle.VERTICAL });

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
            this.takeRandomAction();
        } else {
            long start = System.currentTimeMillis();
            this.takeBestAction();
            System.out.println("Best Action took " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    private void takeRandomAction() {
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
        // System.out.println("ACTION Random Feature Vals=" + featureVals + "\t Weights" + this.featureWeights);
        usbWriter.setHardPlayerPositions(intendedYPositions, intendedPlayerAngles);
        previousIntendedYPositions = intendedYPositions;
        previousIntendedPlayerAngles = intendedPlayerAngles;
        previousGameState = new DynamicGameState(gameState);
    }

    private void takeBestAction() {
        float maxQVal = -Float.MAX_VALUE;
        // List<Float> featureVals = null;
        List<List<Float>> bestFeatureVals = new ArrayList<List<Float>>();
        List<Action> bestActions = new ArrayList<Action>();
        // Max over all possible actions (40,000)
        List<Future<QValueData>> futures = new ArrayList<Future<QValueData>>(40);
        for (PlayerAngle playerAngle : PlayerAngle.getLimitedValues()) {
            futures.add(executor.submit(new ExtendedQValueCalculator(playerAngle)));
        }
        try {
            for (Future<QValueData> future : futures) {
                QValueData data = future.get();
                float qVal = data.getMaxQVal();
                if (qVal > maxQVal) {
                    maxQVal = qVal;

                    // Replace lists
                    bestFeatureVals = data.getBestFeatureVals();
                    bestActions = data.getBestActions();
                } else if (qVal == maxQVal) {
                    // Add to lists
                    bestFeatureVals.addAll(data.getBestFeatureVals());
                    bestActions.addAll(data.getBestActions());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        // Take Random Best Action
        int randomIndex = (int) (bestActions.size() * Math.random());
        prevFeatureVals = bestFeatureVals.get(randomIndex);
        prevQVal = maxQVal;
        // System.out.println("ACTION Best of size=" + bestActions.size() + " Feature Vals=" + prevFeatureVals +
        // "\t Weights" + this.featureWeights);
        usbWriter
                .setHardPlayerPositions(bestActions.get(randomIndex).getIntendedYPositions(), bestActions.get(randomIndex).getIntendedPlayerAngles());
        previousIntendedYPositions = bestActions.get(randomIndex).getIntendedYPositions();
        previousIntendedPlayerAngles = bestActions.get(randomIndex).getIntendedPlayerAngles();
        previousGameState = new DynamicGameState(gameState);
    }

    private void updateWeights() {
        if (previousGameState == null || gameState.getPlayerThatScored() != null) {
            // Don't update weights on first pass or if game is over
            return;
        }
        float maxQVal = -Float.MAX_VALUE;
        List<Future<Float>> futureQValues = new ArrayList<Future<Float>>(3);
        for (PlayerAngle playerAngle : PlayerAngle.getLimitedValues()) {
            futureQValues.add(executor.submit(new QValueCalculator(playerAngle)));
        }
        for (Future<Float> futureQValue : futureQValues) {
            Float qVal = -Float.MAX_VALUE;
            try {
                qVal = futureQValue.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (qVal > maxQVal) {
                maxQVal = qVal;
            }
        }
        float diff = this.getReward() + gamma * maxQVal - prevQVal;
        System.out.println("UPDATE WEIGHTS diff=" + diff + "\tbestQVal=" + maxQVal + "\tprevQ" + prevQVal + "\tprevFeatures=" + prevFeatureVals);
        for (int i = 0; i < featureWeights.size(); i++) {
            featureWeights.set(i, featureWeights.get(i) + alpha * diff * prevFeatureVals.get(i));
        }
        System.out.println("UPDATE WEIGHTS New Weights=" + featureWeights);
    }

    /**
     * ALL FEATURES MUST BE <= 1
     * 
     * @param rowOneYPos
     * @param rowThreeYPos
     * @param rowOneAngle
     * @param rowThreeAngle
     * @return
     */
    private List<Float> getFeatureValuesCurrentStateAndActions(int rowOneYPos, int rowThreeYPos, PlayerAngle rowOneAngle, PlayerAngle rowThreeAngle) {
        int adjustedRowOneYPos = (int) ((rowOneYPos / yPositionToByteFactor) + 152);
        int adjustedRowThreeYPos = (int) (-(rowThreeYPos / yPositionToByteFactor) + 226);
        int yPosOfBall = gameState.getBallYPosition() / 2;
        List<Float> featureValues = new ArrayList<Float>(featureWeights.size());

        // Feature 1 - Actions that get the Y Pos of a player in line with the ball get a higher score
        featureValues.add(.0f);
        boolean willRowOneBeNearTheBall = false;
        for (int player = 0; player < gameState.getNumbersOfPlayersForRow(1); player++) {
            int yPosOfPlayer = (adjustedRowOneYPos - player * gameState.getDistanceBetweenPlayersForRow(1)) / 2;
            if (player == 0) {
                yPosOfPlayer -= 2;
            }
            if (player == 2) {
                yPosOfPlayer += 2;
            }
            if (yPosOfBall == yPosOfPlayer) {
                if (gameState.getBallXPosition() > (gameState.getMaxX() / 4) * 3) {
                    if (rowOneAngle == PlayerAngle.BACKWARD_ANGLED) {
                        featureValues.set(0, featureValues.get(0) + .5f);
                    }
                } else {
                    if (rowOneAngle == PlayerAngle.VERTICAL) {
                        featureValues.set(0, featureValues.get(0) + .5f);
                    }
                }
                willRowOneBeNearTheBall = true;
                break;
            }
        }
        boolean willRowThreeBeNearTheBall = false;
        for (int player = 0; player < gameState.getNumbersOfPlayersForRow(3); player++) {
            int yPosOfPlayer = (adjustedRowThreeYPos - player * gameState.getDistanceBetweenPlayersForRow(3)) / 2;
            if (player == 0) {
                yPosOfPlayer -= 4;
            }
            if (player == 2) {
                yPosOfPlayer += 4;
            }
            if (yPosOfBall == yPosOfPlayer) {
                if (rowThreeAngle == PlayerAngle.VERTICAL) {
                    featureValues.set(0, featureValues.get(0) + .5f);
                }
                willRowThreeBeNearTheBall = true;
                break;
            }
        }
        // Feature 2 If Ball is front of you characterize actions that might hit it forward
        if (willRowOneBeNearTheBall && this.isBallInFrontAndWithinReachOfRow(1, gameState.getBallXPosition())
                && rowOneAngle == PlayerAngle.FORWARD_ANGLED && (gameState.getPlayerAngleForRow(1) == PlayerAngle.VERTICAL)) {
            featureValues.add(1f);
        } else {
            featureValues.add(0f);

        }
        // Feature 3 same for row 3
        if (willRowThreeBeNearTheBall && this.isBallInFrontAndWithinReachOfRow(3, gameState.getBallXPosition())
                && rowThreeAngle == PlayerAngle.FORWARD_ANGLED && (gameState.getPlayerAngleForRow(3) == PlayerAngle.VERTICAL)) {
            featureValues.add(1f);
        } else {
            featureValues.add(0f);
        }
        // Feature 4 - try to hit the ball in the Y direction if its behind you
        boolean isRowOneNearTheBall = false;
        for (int player = 0; player < gameState.getNumbersOfPlayersForRow(1); player++) {
            int yPosOfPlayer = (gameState.getRowYPosition(1) - player * gameState.getDistanceBetweenPlayersForRow(1)) / 2;
            if (player == 0) {
                yPosOfPlayer -= 2;
            }
            if (player == 2) {
                yPosOfPlayer += 2;
            }
            if (yPosOfBall == yPosOfPlayer) {
                isRowOneNearTheBall = true;
                break;
            }
        }
        boolean isRowThreeNearTheBall = false;
        for (int player = 0; player < gameState.getNumbersOfPlayersForRow(3); player++) {
            int yPosOfPlayer = (gameState.getRowYPosition(3) - player * gameState.getDistanceBetweenPlayersForRow(3)) / 2;
            if (player == 0) {
                yPosOfPlayer -= 4;
            }
            if (player == 2) {
                yPosOfPlayer += 4;
            }
            if (yPosOfBall == yPosOfPlayer) {
                isRowThreeNearTheBall = true;
                break;
            }
        }
        if (!isRowOneNearTheBall && willRowOneBeNearTheBall && this.isBallInBehindAndWithinReachOfRow(1, gameState.getBallXPosition())
                && rowOneAngle == PlayerAngle.BACKWARD_ANGLED) {
            featureValues.add(1f);
        } else if (isRowOneNearTheBall && !willRowOneBeNearTheBall && this.isBallInBehindAndWithinReachOfRow(1, gameState.getBallXPosition())
                && rowOneAngle == PlayerAngle.BACKWARD_ANGLED) {
            featureValues.add(1f);
        } else if (!isRowThreeNearTheBall && willRowThreeBeNearTheBall && this.isBallInBehindAndWithinReachOfRow(3, gameState.getBallXPosition())
                && rowThreeAngle == PlayerAngle.BACKWARD_ANGLED) {
            featureValues.add(.5f);
        } else if (isRowThreeNearTheBall && !willRowThreeBeNearTheBall && this.isBallInBehindAndWithinReachOfRow(3, gameState.getBallXPosition())
                && rowThreeAngle == PlayerAngle.BACKWARD_ANGLED) {
            featureValues.add(1f);
        } else {
            featureValues.add(0f);
        }
        // Feature 5 anti jerky-ness
        featureValues.add(0f);
        if (adjustedRowOneYPos / 5 == gameState.getRowYPosition(1) / 5) {
            featureValues.set(4, featureValues.get(4) + .5f);
        }
        if (adjustedRowThreeYPos / 5 == gameState.getRowYPosition(3) / 5) {
            featureValues.set(4, featureValues.get(4) + .5f);
        }
        return featureValues;
    }

    // R(s,a,s')
    // Numbers change all the time
    private float getReward() {
        Player playerThatScored = gameState.getPlayerThatScored();
        if (playerThatScored == null) {
            int reward = 0;
            // Game is still live
            int prevBallXPos = gameState.getPrevBallPosition().x;
            int ballXPos = gameState.getBallXPosition();
            int yPositionOfRowThree = gameState.getRowYPosition(3);
            int yPositionOfRowOne = gameState.getRowYPosition(3);
            int ballYPos = gameState.getBallYPosition() / 3;

            // Ball is in AI control
            // Reward a hit -ignore actions due to non deterministic timing here
            if (this.isBallWithinReachOfRow(1, prevBallXPos) || this.isBallWithinReachOfRow(3, prevBallXPos)) {
                if (ballXPos + 3 < prevBallXPos) {
                    reward += 20;
                }
                // Reward a future block
                if (ballXPos > prevBallXPos && ballXPos > gameState.getMaxX() / 2) {
                    for (int player = 0; player < gameState.getNumbersOfPlayersForRow(3); player++) {
                        int yPosOfPlayer = (yPositionOfRowThree - player * gameState.getDistanceBetweenPlayersForRow(3)) / 3;
                        if (ballYPos == yPosOfPlayer) {
                            reward += 10;
                        }
                    }
                }
                // Reward the forwards for staying away from the ball if its moving to the left
                if (ballXPos + 3 < prevBallXPos && prevBallXPos >= gameState.getMaxX() / 2) {
                    reward += 2;
                }
            } else {
                // Reward players for staying down
                if (this.previousIntendedPlayerAngles.get(0) == PlayerAngle.VERTICAL) {
                    reward += 2;
                }
                if (this.previousIntendedPlayerAngles.get(1) == PlayerAngle.VERTICAL) {
                    reward += 2;
                }
                // Reward players for following the ball
                for (int player = 0; player < gameState.getNumbersOfPlayersForRow(3); player++) {
                    int yPosOfPlayer = (yPositionOfRowThree - player * gameState.getDistanceBetweenPlayersForRow(3)) / 3;
                    if (ballYPos == yPosOfPlayer) {
                        reward += 2;
                        break;
                    }
                }
                for (int player = 0; player < gameState.getNumbersOfPlayersForRow(1); player++) {
                    int yPosOfPlayer = (yPositionOfRowOne - player * gameState.getDistanceBetweenPlayersForRow(1)) / 3;
                    if (ballYPos == yPosOfPlayer) {
                        reward += 2;
                        break;
                    }
                }
            }
            return reward;

        } else if (playerThatScored == Player.SELF) {
            return 50;
        } else {
            return 0;
        }
    }

    private class QValueCalculator implements Callable<Float> {
        private PlayerAngle playerAngle;

        public QValueCalculator(PlayerAngle playerAngle) {
            super();
            this.playerAngle = playerAngle;
        }

        public Float call() {
            float maxQVal = -Float.MAX_VALUE;
            // Max over all possible actions (40,000)
            for (int rowZeroYPos = 0; rowZeroYPos < 40; rowZeroYPos++) {
                for (int rowOneYPos = 0; rowOneYPos < 40; rowOneYPos++) {
                    for (PlayerAngle rowZeroAngle : PlayerAngle.getLimitedValues()) {
                        float qVal = 0;
                        List<Float> featureVals = getFeatureValuesCurrentStateAndActions(rowZeroYPos, rowOneYPos, rowZeroAngle, playerAngle);
                        for (int i = 0; i < featureVals.size(); i++) {
                            qVal += featureWeights.get(i) * featureVals.get(i);
                        }
                        if (qVal > maxQVal) {
                            maxQVal = qVal;
                        }

                    }
                }
            }
            return maxQVal;
        }

    }

    private class ExtendedQValueCalculator implements Callable<QValueData> {
        private PlayerAngle playerAngle;

        public ExtendedQValueCalculator(PlayerAngle playerAngle) {
            super();
            this.playerAngle = playerAngle;
        }

        public QValueData call() {
            float maxQVal = -Float.MAX_VALUE;
            List<List<Float>> bestFeatureVals = new ArrayList<List<Float>>();
            List<Action> bestActions = new ArrayList<Action>();
            // Max over all possible actions (40,000)
            for (int rowZeroYPos = 0; rowZeroYPos < 40; rowZeroYPos += 2) {
                for (int rowOneYPos = 0; rowOneYPos < 40; rowOneYPos++) {
                    for (PlayerAngle rowOneAngle : PlayerAngle.getLimitedValues()) {
                        float qVal = 0;
                        List<Float> featureVals = getFeatureValuesCurrentStateAndActions(rowZeroYPos, rowOneYPos, playerAngle, rowOneAngle);
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
                            intendedPlayerAngles.add(playerAngle);
                            intendedPlayerAngles.add(rowOneAngle);
                            intendedYPositions.add(rowZeroYPos);
                            intendedYPositions.add(rowOneYPos);

                            // Add to lists
                            bestFeatureVals.add(featureVals);
                            bestActions.add(new Action(intendedYPositions, intendedPlayerAngles));
                        } else if (qVal == maxQVal) {
                            List<Integer> intendedYPositions = new ArrayList<Integer>(2);
                            List<PlayerAngle> intendedPlayerAngles = new ArrayList<PlayerAngle>(2);
                            intendedPlayerAngles.add(playerAngle);
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

            return new QValueData(maxQVal, bestFeatureVals, bestActions);
        }
    }

    private class QValueData {
        float maxQVal = -Float.MAX_VALUE;
        List<List<Float>> bestFeatureVals = new ArrayList<List<Float>>();
        List<Action> bestActions = new ArrayList<Action>();

        public QValueData(float maxQVal, List<List<Float>> bestFeatureVals, List<Action> bestActions) {
            super();
            this.maxQVal = maxQVal;
            this.bestFeatureVals = bestFeatureVals;
            this.bestActions = bestActions;
        }

        public float getMaxQVal() {
            return maxQVal;
        }

        public List<List<Float>> getBestFeatureVals() {
            return bestFeatureVals;
        }

        public List<Action> getBestActions() {
            return bestActions;
        }

    }

    private boolean isBallInFrontAndWithinReachOfRow(int row, int xPosOfBall) {
        if (row == 3) {
            return (xPosOfBall < (gameState.getRowXPosition(row) - 15) && xPosOfBall > gameState.getRowXPosition(row) - 53);
        }
        return (xPosOfBall < (gameState.getRowXPosition(row) - 8) && xPosOfBall > gameState.getRowXPosition(row) - 38);
    }

    private boolean isBallInBehindAndWithinReachOfRow(int row, int xPosOfBall) {
        return (xPosOfBall >= gameState.getRowXPosition(row) + 10 && xPosOfBall < gameState.getRowXPosition(row) + 35);
    }

    private boolean isBallWithinReachOfRow(int row, int xPosOfBall) {
        return this.isBallInFrontAndWithinReachOfRow(row, xPosOfBall) || this.isBallInBehindAndWithinReachOfRow(row, xPosOfBall);
    }

    private boolean isBallInFrontOfRow(int row, int xPosOfBall) {
        return (xPosOfBall < gameState.getRowXPosition(row));
    }

    private boolean isBallBehindRow(int row, int xPosOfBall) {
        return (xPosOfBall >= gameState.getRowXPosition(row));
    }
}
