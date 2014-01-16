package com.saba.foosball.model;

import java.util.List;

public class Action {

    private List<Integer> intendedYPositions;
    private List<PlayerAngle> intendedPlayerAngles;

    public Action(List<Integer> intendedYPositions, List<PlayerAngle> intendedPlayerAngles) {
        super();
        this.intendedYPositions = intendedYPositions;
        this.intendedPlayerAngles = intendedPlayerAngles;
    }

    public List<Integer> getIntendedYPositions() {
        return intendedYPositions;
    }

    public List<PlayerAngle> getIntendedPlayerAngles() {
        return intendedPlayerAngles;
    }

}
