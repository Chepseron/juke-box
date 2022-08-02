/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package awesomex;

import static javafx.geometry.Pos.CENTER;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import jssc.SerialPort;
import jssc.SerialPortException;

/**
 *
 * @author asabul
 */
public class WaitForArduino {

    //a scene for a new song 
    Group _newSongGroup;
    Scene _newSongScene;
    Stage _newSongPrimaryStage;

    public void readCom() {
        //Serial port configuration variables
        String comPort = "COM3";
        int baudeRate = 9600;
        int dataBits = 8;
        int stopBits = 1;
        int parity = 0;
        SerialPort serialPort = new SerialPort(comPort);
        try {
            serialPort.openPort();//open serial port

            serialPort.setParams(baudeRate, dataBits, stopBits, parity);
            String _coins = "";

            while (!_coins.equals("exit")) {
                System.out.println("Connection successfully " + _coins);
                _coins = serialPort.readString();
                if (_coins != null) {
                    System.out.println(_coins);

                    TextField newSong = new TextField();

                    newSong.setAlignment(CENTER);
                    newSong.centerShapeProperty();
                    newSong.setOnKeyPressed(event -> {
                        if (event.getCode() == KeyCode.ENTER) {

                        }
                    });
                    _newSongGroup = new Group();
                    _newSongScene = new Scene(_newSongGroup, 200, 30);
                    _newSongPrimaryStage = new Stage();
                    _newSongGroup.getChildren().add(newSong);
                    _newSongPrimaryStage.setScene(_newSongScene);
                    _newSongPrimaryStage.setTitle("select song number");
                    _newSongPrimaryStage.show();
                } else {
                    _coins = "";
                }
            }
            serialPort.closePort();

        } catch (SerialPortException e) {
            System.out.println("Connection failure, follow the errors below: ");
            e.printStackTrace();
        }

    }
}
