package com.saba.foosball.input;

import java.awt.image.BufferedImage;

import com.googlecode.javacv.FFmpegFrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class JavaCVFoosballStateReader implements FoosballStateReader, Runnable {

    private FFmpegFrameGrabber grabber;
    private Thread webcamReaderThread;
    private BufferedImage img;
    private volatile boolean isShutdown = false;

    public JavaCVFoosballStateReader() {
        grabber = new FFmpegFrameGrabber(new java.io.File("/dev/video1"));
        grabber.setFormat("video4linux2");
        grabber.setImageHeight(240);
        grabber.setImageWidth(320);
        // grabber.setSampleRate(125);
        grabber.setFrameRate(125);

    }

    public void start() {
        try {
            grabber.start();
            grabber.setFrameRate(125);
            System.out.println(grabber.getFrameRate());
            System.out.println(grabber.getFormat());
            System.out.println(grabber.getPixelFormat());
            System.out.println(grabber.getSampleRate());
        } catch (com.googlecode.javacv.FrameGrabber.Exception e1) {
            e1.printStackTrace();
            return;
        }

        try {
            IplImage image = grabber.grab();
            img = image.getBufferedImage();
        } catch (Exception e) {
            e.printStackTrace();
        }

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
            try {
                // long start = System.currentTimeMillis();
                IplImage image = grabber.grab();
                if (image != null) {
                    img = image.getBufferedImage();
                    // System.out.println(System.currentTimeMillis() - start + "ms");
                } else {
                    // System.out.println(System.currentTimeMillis() - start + "ms failed");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) throws com.googlecode.javacv.FrameGrabber.Exception {
        JavaCVFoosballStateReader reader = new JavaCVFoosballStateReader();
        reader.start();
    }

}
