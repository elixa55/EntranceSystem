/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * @author
 */
public class Options {
	
     /** parameters for setting communication port 
     * 
     */
    public final static ObservableList<String> BAUDRATE = FXCollections.observableArrayList(
            "9600", 
            "19200", 
            "38400",
            "57600", 
            "74880",
            "115200"
      );
      
       public final static ObservableList<String> PORT = FXCollections.observableArrayList(
            "COM1",
            "COM2",
            "COM3", 
            "COM4",
            "COM5",
            "COM6", 
            "COM7",
            "COM8",
            "COM9",
            "COM10"
      );
}
