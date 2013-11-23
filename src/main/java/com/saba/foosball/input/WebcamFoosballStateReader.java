package com.saba.foosball.input;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;

import com.github.sarxos.webcam.Webcam;


public class WebcamFoosballStateReader implements FoosballStateReader {

	private Webcam camera;
	
	public WebcamFoosballStateReader() {
		List<Webcam> webcams = Webcam.getWebcams();
        this.camera = webcams.get(webcams.size() - 1);
        camera.setViewSize(new Dimension(320, 240));
        camera.open();
	}

	public BufferedImage readState() {
		return camera.getImage();
	}
	
}
