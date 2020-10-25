package verify;

import static controller.ViewController.message;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.Initializable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import static controller.ViewController.staticVerify;
import static controller.ViewController.personData;
import model.Person;


public class VerifyController implements Initializable {

  public static boolean buttonVerifyClicked = false;
  public static String userName;
  public static String password;
    
    @FXML
    private TextField addVerifyName;

    @FXML
    private TextField addVerifyPassword;

    @FXML
    private Button addVerify;

    /**logged user for the verification can verify himself
     * 
     * @param event
     */
    @FXML
    void addVerifyButton(ActionEvent event) {
        Person selected = null;
        userName = addVerifyName.getText();
        password = addVerifyPassword.getText();
        if (userName.equals("") || userName == null || password.equals("") || password == null)
              message("You should fill in all fields");
        else if (userName.equals("admin"))
        	message("Administrator can not make verification");
        else {
            for (Person p : personData) {
              if (p.getLastname().equals(userName) && p.getPassword().equals(password))
                       selected = p;     
            }
            if (selected != null){
                  message("You can start the verification");
                  buttonVerifyClicked = true;
                  staticVerify.close();
                                    
            }
            else
                  message("Incorrect name or password");
        }
     }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {        
    }

}
