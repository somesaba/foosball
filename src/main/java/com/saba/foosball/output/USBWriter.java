package com.saba.foosball.output;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import com.saba.foosball.model.PlayerAngle;

public class USBWriter {

    private SerialPort serialPort;
    /** The output stream to the port */
    private OutputStream output;
    /** Milliseconds to block while waiting for port open */
    private static final int TIME_OUT = 2000;
    /** Default bits per second for COM port. */
    private static final int DATA_RATE = 9600;
    // Experimentally Derived
    double yPositionToByteFactor = 1;
    List<Byte> rowYPosisitionOffset = new ArrayList<Byte>();

    public void initialize() {
        // Calculate gameState to byte factors
        yPositionToByteFactor = 45d / 70d;
        rowYPosisitionOffset.add((byte) 83);
        rowYPosisitionOffset.add((byte) 63);
        CommPortIdentifier portId = null;

        try {
            portId = CommPortIdentifier.getPortIdentifier("/dev/ttyACM0");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (portId == null) {
            System.out.println("Could not find COM port.");
            return;
        }

        try {
            // open serial port, and use class name for the appName.
            serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);

            // set port parameters
            serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

            // open the stream
            output = serialPort.getOutputStream();
            // (new Thread(new SerialReader(serialPort.getInputStream()))).start();
            output.write(new byte[] { (byte) 225 });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPlayerPositions(List<Integer> intendedYPositions, List<PlayerAngle> intendedPlayerAngles) {
        // Map integers to bytes
        try {
            ByteBuffer buf = ByteBuffer.allocate(intendedPlayerAngles.size() * 2);
            for (int controllablePlayerRow = 0; controllablePlayerRow < intendedPlayerAngles.size(); controllablePlayerRow++) {
                byte yPosition = 0;
                if (controllablePlayerRow == 0)
                    yPosition = (byte) (((intendedYPositions.get(controllablePlayerRow) - 152) * yPositionToByteFactor));
                else
                    yPosition = (byte) ((223 - intendedYPositions.get(controllablePlayerRow)) * yPositionToByteFactor);

                if (yPosition > 40)
                    yPosition = 40;
                if (yPosition < 0)
                    yPosition = 0;
                yPosition += rowYPosisitionOffset.get(controllablePlayerRow);
                PlayerAngle intendedAngle = intendedPlayerAngles.get(controllablePlayerRow);
                byte angleByte = (byte) 90;
                if (intendedAngle == PlayerAngle.BACKWARD_ANGLED) {
                    angleByte = (byte) 45;
                } else if (intendedAngle == PlayerAngle.BACKWARD_HORIZONTAL) {
                    angleByte = (byte) 0;
                } else if (intendedAngle == PlayerAngle.FORWARD_ANGLED) {
                    angleByte = (byte) 0x87;
                } else if (intendedAngle == PlayerAngle.FORWARD_HORIZONTAL) {
                    angleByte = (byte) 0xB4;
                }
                // if (controllablePlayerRow == 0)
                // System.out.println("Row=" + controllablePlayerRow + "; y=" + yPosition + "; unfactored="
                // + (intendedYPositions.get(controllablePlayerRow)));
                // else
                // System.out.println("Row=" + controllablePlayerRow + "; y=" + yPosition + "; unfactored="
                // + (intendedYPositions.get(controllablePlayerRow)));
                buf.put(yPosition);
                buf.put(angleByte);
            }
            output.write(buf.array());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** */
    public static class SerialReader implements Runnable {
        InputStream in;

        public SerialReader(InputStream in) {
            this.in = in;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int len = -1;
            try {
                while ((len = this.in.read(buffer)) > -1) {
                    System.out.print(new String(buffer, 0, len));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    public static void main(String[] args) {
        Enumeration ports = CommPortIdentifier.getPortIdentifiers();
        System.out.println("PRTS:" + ports.toString());
        while (ports.hasMoreElements()) {
            CommPortIdentifier currPortId = (CommPortIdentifier) ports.nextElement();
            System.out.println("PRT" + currPortId.getName());
        }
    }
}
