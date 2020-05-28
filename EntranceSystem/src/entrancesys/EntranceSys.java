package entrancesys;

import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class EntranceSys extends Application {
	private static Stage primaryStage;

	private void setPrimaryStage(Stage stage) {
	    EntranceSys.primaryStage = stage;
	}

	static public Stage getPrimaryStage() {
	    return EntranceSys.primaryStage;
	}
	
    @Override
    public void start(Stage stage) throws Exception {
    	setPrimaryStage(stage);
        Parent root = FXMLLoader.load(getClass().getResource("/controller/View.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        stage.setTitle("Fingerprint identification");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
