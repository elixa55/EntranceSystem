/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package enroll;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * @author
 */
public class Options {
      public final static ObservableList<String> BAUDRATE = FXCollections.observableArrayList(
            "57600", 
            "112600"
      );
      
       public final static ObservableList<String> PORT = FXCollections.observableArrayList(
            "COM3", 
            "COM6", 
            "COM8"
      );
}
