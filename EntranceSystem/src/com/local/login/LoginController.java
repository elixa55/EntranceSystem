package login;

import static controller.ViewController.message;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.Initializable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import static controller.ViewController.staticLog;
import static controller.ViewController.personData;
import model.Person;


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
        Person selected = null;
        userName = addLoginName.getText();
        password = addLoginPassword.getText();
        if (userName.equals("") || userName == null || password.equals("") || password == null)
              message("You should fill in all fields");
        else {
            for (Person p : personData) {
              if (p.getName().equals(userName) && p.getPassword().equals(password))
                       selected = p;     
            }
            if (selected != null){
                  message("You can start the identification");
                  buttonLoginClicked = true;
                  staticLog.close();
                                    
            }
            else
                  message("Incorrect name or password");
        }
     }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {        
    }

}
