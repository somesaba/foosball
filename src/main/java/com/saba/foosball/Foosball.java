package com.saba.foosball;

import java.util.HashMap;
import java.util.Map;

import com.saba.foosball.graphics.GameStateVisualization;
import com.saba.foosball.input.FoosballStateReader;
import com.saba.foosball.input.WebcamFoosballStateReader;
import com.saba.foosball.model.GameState;

public class Foosball {
    private FoosballStateReader stateReader;
    private GameStateUpdater stateUpdater;
    private boolean doDisplayVisualization = false;
    private Map<Integer, Integer> rowToPlayerCountMap = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> rowToPlayerDistanceMap = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> rowToXPositionMap = new HashMap<Integer, Integer>();

    public void start() {
        stateReader.start();
        GameState gameState = new GameState(320, 240, rowToPlayerCountMap, rowToPlayerDistanceMap, rowToXPositionMap);
        if (doDisplayVisualization) {
            GameStateVisualization visualization = new GameStateVisualization(gameState, stateReader);
            visualization.start();
            stateUpdater.register(visualization);
        }
        stateUpdater.setGameState(gameState);
        stateUpdater.start();
    }

    public void destroy() {

    }

    public static void main(String[] args) {
        Foosball foosball = new Foosball();
        FoosballStateReader stateReader = new WebcamFoosballStateReader();
        GameStateUpdater gameStateUpdater = new NaiveGameStateUpdater();
        gameStateUpdater.setStateReader(stateReader);
        Map<Integer, Integer> rowToPlayerCountMap = new HashMap<Integer, Integer>();
        Map<Integer, Integer> rowToPlayerDistanceMap = new HashMap<Integer, Integer>();
        Map<Integer, Integer> rowToXPositionMap = new HashMap<Integer, Integer>();
        for (int i = 0; i < 4; i++) {
            rowToPlayerCountMap.put(i, 3);
            rowToPlayerDistanceMap.put(i, 68);
            rowToXPositionMap.put(i, i * 78 + 40);
        }
        foosball.setStateReader(stateReader);
        foosball.setStateUpdater(gameStateUpdater);
        foosball.setDoDisplayVisualization(true);
        foosball.setRowToPlayerCountMap(rowToPlayerCountMap);
        foosball.setRowToPlayerDistanceMap(rowToPlayerDistanceMap);
        foosball.setRowToXPositionMap(rowToXPositionMap);
        foosball.start();
    }

    public boolean isDoDisplayVisualization() {
        return doDisplayVisualization;
    }

    public void setDoDisplayVisualization(boolean doDisplayVisualization) {
        this.doDisplayVisualization = doDisplayVisualization;
    }

    public GameStateUpdater getStateUpdater() {
        return stateUpdater;
    }

    public void setStateUpdater(GameStateUpdater stateUpdater) {
        this.stateUpdater = stateUpdater;
    }

    public Map<Integer, Integer> getRowToPlayerCountMap() {
        return rowToPlayerCountMap;
    }

    public void setRowToPlayerCountMap(Map<Integer, Integer> rowToPlayerCountMap) {
        this.rowToPlayerCountMap = rowToPlayerCountMap;
    }

    public Map<Integer, Integer> getRowToPlayerDistanceMap() {
        return rowToPlayerDistanceMap;
    }

    public void setRowToPlayerDistanceMap(Map<Integer, Integer> rowToPlayerDistanceMap) {
        this.rowToPlayerDistanceMap = rowToPlayerDistanceMap;
    }

    public Map<Integer, Integer> getRowToXPositionMap() {
        return rowToXPositionMap;
    }

    public void setRowToXPositionMap(Map<Integer, Integer> rowToXPositionMap) {
        this.rowToXPositionMap = rowToXPositionMap;
    }

    public FoosballStateReader getStateReader() {
        return stateReader;
    }

    public void setStateReader(FoosballStateReader stateReader) {
        this.stateReader = stateReader;
    }

}
