package com.saba.foosball.input;

import java.awt.Point;
import java.util.List;
import java.util.Map;

import com.saba.foosball.model.PlayerAngle;

public interface FoosballStateReader {    
    //Static State
    Point getMaxBoardSize();
    Map<Integer, Integer> getRowToPlayerCountMap();
    Map<Integer, Integer> getRowToPlayerDistanceMap();
    Map<Integer, Integer> getRowToYPositionMap();
    
    //Dynamic State
    Point readBallPosition();
    List<PlayerAngle> readPlayerAnglesByRow();
    List<Integer> readRowXPositions();
}
