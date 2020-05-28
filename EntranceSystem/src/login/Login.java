package login;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class Login extends Stage {
    
    public Login() {
        this.setTitle("Identification");
        this.setResizable(true);
        this.initModality(Modality.APPLICATION_MODAL);
        FXMLLoader Loader = new FXMLLoader();
        Loader.setLocation(getClass().getResource("/login/Login.fxml"));
        try {
            Loader.load();
        } catch (Exception e) {
            System.out.println("Login stage loading error");
            System.err.println(e);
        }
        Parent root = Loader.getRoot();
        Scene scene = new Scene(root, 350, 400);
        scene.getStylesheets().add(getClass().getResource("/entrancesys/style.css").toExternalForm());
        this.setScene(scene);
    }
}

