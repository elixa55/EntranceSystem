package login;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.Initializable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import static controller.ViewController.staticLog;


public class LoginController implements Initializable {

  public static boolean buttonLoginClicked = false;
  public static String userName;
  public static String password;
    
    @FXML
    private TextField addLoginName;

    @FXML
    private TextField addLoginPassword;

    @FXML
    private Button addLogin;

    @FXML
    void addLoginButton(ActionEvent event) {
        userName = addLoginName.getText();
        password = addLoginPassword.getText();
        buttonLoginClicked = true;
        staticLog.close();
     }
    
   
    
    private void messageInfo(String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Információ");
        alert.setHeaderText("Információ");
        alert.setContentText(text);
        alert.showAndWait();
    }
    @Override
    public void initialize(URL url, ResourceBundle rb) {        
 

    }

}
