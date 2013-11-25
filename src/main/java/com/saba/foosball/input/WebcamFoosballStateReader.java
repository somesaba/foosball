package com.saba.foosball.input;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;

import com.github.sarxos.webcam.Webcam;

public class WebcamFoosballStateReader implements FoosballStateReader, Runnable {

    private Webcam camera;
    private Thread webcamReaderThread;
    private BufferedImage img;
    private volatile boolean isShutdown = false;

    public WebcamFoosballStateReader() {
        List<Webcam> webcams = Webcam.getWebcams();
        this.camera = webcams.get(webcams.size() - 1);
        camera.setViewSize(new Dimension(320, 240));
        camera.open();
    }

    public void start() {
        while (img != null)
            img = camera.getImage();
        webcamReaderThread = new Thread(this, "WebcamReaderThread");
        webcamReaderThread.start();
    }

    public void stop() {
        isShutdown = true;
        try {
            webcamReaderThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public BufferedImage readState() {
        return img;
    }

    public void run() {
        isShutdown = false;
        while (!isShutdown) {
            img = camera.getImage();
        }

    }

}
