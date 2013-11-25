package com.saba.foosball.input;

import java.awt.image.BufferedImage;

public interface FoosballStateReader {

    public void start();

    public void stop();

    BufferedImage readState();
}
