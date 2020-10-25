package entrancesystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


/**
 * @author edit
 *
 */
public class EntranceSystem extends Application {
	private static Stage primaryStage;

	private void setPrimaryStage(Stage stage) {
	    EntranceSystem.primaryStage = stage;
	}

	static public Stage getPrimaryStage() {
	    return EntranceSystem.primaryStage;
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
    	System.out.println("Start of the app");
        launch(args);
        System.out.println("End of the app");
    }
    
}
