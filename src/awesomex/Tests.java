/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package awesomex;

import java.util.Timer;
import java.util.TimerTask;
import jssc.SerialPort;
import jssc.SerialPortException;

/**
 *
 * @author asabul
 */
public class Tests {

    public static void main(String[] args) {

        Timer timer = new Timer();
        TimerTask task = new MyTask();
        timer.schedule(task, 1000, 3000);
    }

}

//tests the insert 10 bob bit
class MyTask extends TimerTask {

    public void run() {

        try {

            //Serial port configuration variables
            String comPort = "COM2";
            int baudeRate = 3000000;
            int dataBits = 8;
            int stopBits = 1;
            int parity = 0;
            SerialPort serialPort = new SerialPort(comPort);

            if (serialPort.isOpened()) {

                try {

                    serialPort.writeString("10");
                } catch (SerialPortException e) {
                    System.out.println("Connection failure, follow the errors below: ");
                    e.printStackTrace();
                }
            } else {

                try {
                    serialPort.openPort();
                    serialPort.writeString("10");
                } catch (SerialPortException e) {
                    System.out.println("Connection failure, follow the errors below: ");
                    e.printStackTrace();
                }

            }
        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}
