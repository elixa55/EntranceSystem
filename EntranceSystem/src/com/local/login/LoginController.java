package login;

import static controller.ViewController.message;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.Initializable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import static controller.ViewController.staticLogin;
import static controller.ViewController.personData;
import model.Person;


public class LoginController implements Initializable {

  public static boolean buttonLoginClicked = false;
  public static Person logged;
  public static String loggedName;
  public static String password;
    
    @FXML
    private TextField addLoginName;

    @FXML
    private TextField addLoginPassword;

    @FXML
    private Button addLogin;

    /**log in editor application
     * @param event
     */
    @FXML
    void addLoginButton(ActionEvent event) {
        Person selected = null;
        loggedName = addLoginName.getText();
        password = addLoginPassword.getText();
        if (loggedName.equals("") || loggedName == null || password.equals("") || password == null)
              message("You should fill in all fields");
        else {
            for (Person p : personData) {
              if (p.getLastname().equals(loggedName) && p.getPassword().equals(password))
                       selected = p;     
            }
            if (selected != null){
                  message("Welcome to personal data editor");
                  buttonLoginClicked = true;
                  logged = selected;
                  staticLogin.close();
                                    
            }
            else {
                  message("Incorrect name or password");
                  staticLogin.close();
            }
        }
     }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {        
    }

}
