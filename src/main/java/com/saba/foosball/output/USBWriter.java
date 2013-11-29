package com.saba.foosball.output;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Map;

import com.saba.foosball.model.GameState;
import com.saba.foosball.model.PlayerAngle;

public class USBWriter {

    private SerialPort serialPort;
    /** The port we're normally going to use. */
    private static final String PORT_NAMES[] = { "/dev/ttyUSB0" };

    /** The output stream to the port */
    private OutputStream output;
    /** Milliseconds to block while waiting for port open */
    private static final int TIME_OUT = 2000;
    /** Default bits per second for COM port. */
    private static final int DATA_RATE = 9600;

    double yPositionToByteFactor = 0;

    public void initialize(GameState gameState) {
        // Calculate gameState to byte factors
        yPositionToByteFactor = 255 / (gameState.getMaxY() - 1);

        CommPortIdentifier portId = null;
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

        // First, Find an instance of serial port as set in PORT_NAMES.
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
            for (String portName : PORT_NAMES) {
                if (currPortId.getName().equals(portName)) {
                    portId = currPortId;
                    break;
                }
            }
        }
        if (portId == null) {
            System.out.println("Could not find COM port.");
            return;
        }

        try {
            // open serial port, and use class name for the appName.
            serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);

            // set port parameters
            serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            // open the stream
            output = serialPort.getOutputStream();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPlayerPositions(Map<Integer, Integer> rowToYPositionMap, Map<Integer, PlayerAngle> rowToAngleMap) {
        // Map integers to bytes
        ByteBuffer buf = ByteBuffer.allocate(rowToAngleMap.size() * 2);
        for (int row = 0; row < rowToAngleMap.size(); row++) {
            byte yPosition = (byte) (rowToYPositionMap.get(row) * yPositionToByteFactor);
            PlayerAngle intendedAngle = rowToAngleMap.get(row);
            byte angleByte = (byte) 127;
            if (intendedAngle == PlayerAngle.BACKWARD_ANGLED) {
                angleByte = (byte) 191;
            } else if (intendedAngle == PlayerAngle.BACKWARD_HORIZONTAL) {
                angleByte = (byte) 255;
            } else if (intendedAngle == PlayerAngle.FORWARD_ANGLED) {
                angleByte = (byte) 63;
            } else if (intendedAngle == PlayerAngle.FORWARD_HORIZONTAL) {
                angleByte = (byte) 0;
            }
            buf.put(yPosition);
            buf.put(angleByte);
        }
        try {
            output.write(buf.array());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * This should be called when you stop using the port. This will prevent port locking on platforms like Linux.
     */
    public synchronized void shutdown() {
        if (serialPort != null) {
            serialPort.removeEventListener();
            serialPort.close();
        }
    }

}
