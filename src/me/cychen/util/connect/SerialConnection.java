package me.cychen.util.connect;

import java.io.*;

// the following packages are from rxtx lib.
import gnu.io.UnsupportedCommOperationException;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

public class SerialConnection extends Connection {
    public static final int SERIAL_BAUD_RATE = 115200;
    private static final int SERIAL_TIMEOUT = 2000;
    private String portName = "";
    private SerialPort serial;

    public String getPortName() {
        return portName;
    }

    public SerialConnection(String inPortName) throws Exception {
        super();
        portName = inPortName;
        open(); // This function throws exception.
    }

    private void open() throws Exception {
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);

        if (portIdentifier.isCurrentlyOwned()) {
            //Log.errPutline("Error: Port is currently in use.");
            throw new IOException("Error: Port is currently in use.");
        } else if (portIdentifier.getPortType() != CommPortIdentifier.PORT_SERIAL) {
            //Log.errPutline("Error: It's not a serial port.");
            throw new IOException("Error: It's not a serial port.");
        }

        serial = (SerialPort) portIdentifier.open(this.getClass().getName(), SERIAL_TIMEOUT);
        serial.enableReceiveTimeout(SERIAL_TIMEOUT);
        serial.setSerialPortParams(SERIAL_BAUD_RATE,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);

        inputStream = new BufferedInputStream(serial.getInputStream());
        outputStream = new BufferedOutputStream(serial.getOutputStream());
        output = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(serial.getOutputStream())),
                true);

        //Log.sysPutLine("Attach to the serial port: " + portName);

    }

    public Boolean reopen() {
        if (portName.equals("")) {
            return false;
        }

        try {
            open();
            return true;
        } catch (Exception e) {
            //e.printStackTrace();
            return false;
        }
    }

    @Override
    public void close() {
        serial.close();
        //Log.sysPutLine("Detach from the serial port: " + portName);
    }

    public void flush() {
        Boolean isTimeoutEnabled = serial.isReceiveTimeoutEnabled();
        int tmpTimeout = serial.getReceiveTimeout();

        try {
            serial.enableReceiveTimeout(10);
            String flushedString = read();
            if (flushedString != null) {
                //Log.sysPutLine("Flushed data: " + flushedString);
            }
            if (isTimeoutEnabled) {
                serial.enableReceiveTimeout(tmpTimeout);
            } else {
                serial.disableReceiveTimeout();
            }

        } catch (UnsupportedCommOperationException e) {
            //serial.disableReceiveTimeout();
            e.printStackTrace();
        }
    }
}