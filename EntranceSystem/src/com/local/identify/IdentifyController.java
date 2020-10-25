package identify;

import static controller.ViewController.message;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.Initializable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import static controller.ViewController.staticIdentify;
import static controller.ViewController.personData;
import model.Person;


public class IdentifyController implements Initializable {

  public static boolean buttonIdentifyClicked = false;
  public static String userName;
  public static String password;
    
    @FXML
    private TextField addIdentifyName;

    @FXML
    private TextField addIdentifyPassword;

    @FXML
    private Button addIdentify;

    /**logged user for the identification can identify himself
     * 
     * @param event
     */
    @FXML
    void addIdentifyButton(ActionEvent event) {
        Person selected = null;
        userName = addIdentifyName.getText();
        password = addIdentifyPassword.getText();
        if (userName.equals("") || userName == null || password.equals("") || password == null)
              message("You should fill in all fields");
        else if (userName.equals("admin"))
        	message("Administrator can not make identification");
        else {
            for (Person p : personData) {
              if (p.getLastname().equals(userName) && p.getPassword().equals(password))
                       selected = p;     
            }
            if (selected != null){
                  message("You can start the identification");
                  buttonIdentifyClicked = true;
                  staticIdentify.close();
                                    
            }
            else
                  message("Incorrect name or password");
        }
     }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {        
    }

}
