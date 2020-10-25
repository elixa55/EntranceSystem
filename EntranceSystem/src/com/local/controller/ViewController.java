package controller;

import java.io.File;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.awt.image.BufferedImage;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jssc.SerialPortException;
import login.Login;
import matchfinger.FingerprintMatcher;
import matchfinger.FingerprintTemplate;
import model.Options;
import model.Person;
import model.PersonDao;
import model.PersonDaoImpl;
import processing.ImageProcessing;
import processing.Minutiae;
import verify.Verify;
import entrancesystem.EntranceSystem;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TabPane;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import enroll.Enroll;

import static processing.ImageProcessing.width;
import static verify.VerifyController.buttonVerifyClicked;
import static verify.VerifyController.userName;
import static processing.ImageProcessing.height;
import static login.LoginController.buttonLoginClicked;
import static login.LoginController.logged;

public class ViewController implements Initializable {
	/**
	 * FXML components
	 */
	@FXML
	private TableView<Person> table;
	@FXML
	private TableColumn<Person, String> tId;
	@FXML
	private TableColumn<Person, String> tLastname;
	@FXML
	private TableColumn<Person, String> tFirstname;
	@FXML
	private TableColumn<Person, String> tOccupation;
	@FXML
	private ImageView probeImage;
	@FXML
	private Pane imagePane;
	@FXML
	private TextField addFirstname;
	@FXML
	private TextField addLastname;
	@FXML
	private TextField addOccupation;
	@FXML
	private TextField addPassword;
	@FXML
	private TextField editFirstname;
	@FXML
	private TextField editLastname;
	@FXML
	private TextField editOccupation;
	@FXML
	private TextArea editFinger;
	@FXML
	private TextField editPassword;
	@FXML
	private TextArea textBox;
	@FXML
	private Label loggedText;
	@FXML
	private ComboBox<String> addBaudRate;
	@FXML
	private ComboBox<String> addPort;
	@FXML
	private TextField addFileName;
	@FXML
	private TabPane tabPane;
	@FXML
	private Tab edit;
	@FXML
	private TextArea text;
	@FXML
	private MenuItem contextImportImage;
	@FXML
	private MenuItem contextDelete;
	@FXML
	private ContextMenu context;

	/**
	 * static global variables
	 */
	final private static PersonDao database = new PersonDaoImpl();
	public static ObservableList<Person> personData = database.get();
	private static String userDir = System.getProperty("user.dir") + "\\";
	
	private static String actualFinger = null;
	private static ImageProcessing ip = null;
	private static BufferedImage imageToSave;

	public static Verify staticVerify;
	public static Login staticLogin;
	public static boolean flagVerify = false;
	public static boolean flag = false;
	public static boolean dataAvailable = false;
	public static File selectedFile = null;
	private static String selectedId;

	/**
	 * Enroll button -> opens given port, get the fingerprint, displays enrolled
	 * image
	 * 
	 * @param event
	 * @throws SerialPortException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@FXML
	public void buttonEnroll(ActionEvent event) throws SerialPortException, InterruptedException, IOException {
		probeImage.setImage(null);
		text.clear();
		Enroll enroll = new Enroll();
		enroll.setUsedBaud(addBaudRate.getSelectionModel().getSelectedItem().toString());
		enroll.setUsedPort(addPort.getSelectionModel().getSelectedItem().toString());
		imageToSave = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		actualFinger = "actual.bmp";
		BufferedImage im = imageToSave;
		File fileTemp = new File(userDir + actualFinger);
		if (fileTemp.exists())
			fileTemp.delete();
		enroll.readDataFromSensor();
		enroll.createImageByteArray(im, actualFinger);
		text.setText(enroll.getText());
		FileInputStream inputstream;
		try {
			inputstream = new FileInputStream(userDir + actualFinger);
			Image image = new Image(inputstream);
			probeImage.setImage(image);
			selectedFile = null;
		} catch (FileNotFoundException ex) {
			Logger.getLogger(ViewController.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * save image as given file name
	 * 
	 * @param event
	 * @throws IOException
	 */
	@FXML
	public void buttonSave(ActionEvent event) throws IOException {
		String subFolder = "src\\fingerprints\\";
		Path fileTemp = Paths.get(userDir + actualFinger);
		if (confirmation()) {
			if ((addFileName.getText() == null || addFileName.getText().equals(""))) {
				message("You should name your file");
			} else {
				if (probeImage.getImage() != null) {
					String validFileName = fileNameCheck(addFileName.getText());
					if (validFileName != null) {
						Path fileToSave = Paths.get(userDir + subFolder + validFileName);
						Files.copy(fileTemp, fileToSave, StandardCopyOption.REPLACE_EXISTING);
						message("Image is saved");
						addFileName.clear();
					} else
						message("Incorrect filename");
				} else
					message("First enroll fingerprint");
			}
		}
	}

	/**
	 * correct file name checking
	 * 
	 * @param name
	 * @return
	 */
	public static String fileNameCheck(String name) {
		String regex = "^[a-zA-Z_]+[a-zA-Z0-9_\\.]*";
		String validFileName = null;
		Pattern pattern = Pattern.compile(regex);
		if (pattern.matcher(name).matches()) {
			if (name.contains(".")) {
				int index = name.lastIndexOf(".");
				validFileName = name.substring(0, index);
				validFileName += ".bmp";
			} else {
				validFileName = name;
				validFileName += ".bmp";
			}
		}
		return validFileName;
	}

	/**
	 * get clicked row's values (for marking the id of selected record)
	 * 
	 */
	@FXML
	public void getValueViaClick() {
		table.setOnMouseClicked(e -> {
			if (logged != null) {
				if (!table.getItems().isEmpty() && logged.getLastname().equals("admin")) {
					if (table.getSelectionModel().getSelectedItem() != null) {
						Person p = table.getItems().get(table.getSelectionModel().getSelectedIndex());
						editFirstname.setText(p.getFirstname());
						editLastname.setText(p.getLastname());
						editFinger.setText(p.getFinger());
						editOccupation.setText(p.getOccupation());
						editPassword.setText(p.getPassword());
						selectedId = p.getId();
					}
				}
			}
		});
	}

	/**
	 * adding new record to database (firstname, lastname, occupation, password of
	 * new employee) finger field is not editable
	 * 
	 * @param event
	 */
	@FXML
	public void buttonAdd(ActionEvent event) {
		Person p = new Person();
		p.setFirstname(addFirstname.getText());
		p.setLastname(addLastname.getText());
		p.setFinger("");
		p.setOccupation(addOccupation.getText());
		p.setPassword(addPassword.getText());
		database.add(p);
		refresh();
		addFirstname.clear();
		addLastname.clear();
		addOccupation.clear();
		addPassword.clear();
	}

	/**
	 * modify the selected fields' values
	 * 
	 * @param event
	 */
	@FXML
	public void buttonEdit(ActionEvent event) {
		Person p = new Person();
		System.out.println(logged.getId());
		p.setId(logged.getId());
		p.setFirstname(editFirstname.getText());
		p.setLastname(editLastname.getText());
		p.setFinger(editFinger.getText());
		p.setOccupation(editOccupation.getText());
		p.setPassword(editPassword.getText());
		database.update(p);
		refresh();
		message("You have successfully updated.");
	}

	/**
	 * get image from file browser to make fingerprint signature
	 * 
	 * @param event
	 */
	@FXML
	public void buttonImportImage(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Image read");
		fileChooser.setInitialDirectory(new File(System.getProperty("user.dir"),
				"\\src\\fingerprints"));
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("*.bmp", "*.bmp"));
		File file = fileChooser.showOpenDialog(EntranceSystem.getPrimaryStage());
		if (file != null) {
			try {
				String fileName = file.toString().replace(userDir, "");
				ip = new ImageProcessing(fileName);
				List<Minutiae> sortedList = ip.getFinalMinutiaeSet();
				String candidateSignature = minutiaeListToString(sortedList);
				System.out.println("Candidate image signature: " + candidateSignature);
				Person p = new Person();
				p.setId(logged.getId());
				p.setFirstname(editFirstname.getText());
				p.setLastname(editLastname.getText());
				p.setFinger(candidateSignature);
				p.setOccupation(editOccupation.getText());
				p.setPassword(editPassword.getText());
				database.update(p);
				refresh();
				editFinger.setText(p.getFinger());
				message("Your fingerprint signature is generated");
			} catch (Exception e) {
				System.out.println(e.getMessage() + e);
				System.out.println("Image file reading error");
			}
			refresh();
		}
	}

	/**
	 * logout from editor
	 * 
	 * @param event
	 */
	@FXML
	public void buttonLogout(ActionEvent event) {
		if (logged.getLastname().equals("admin")) {
			contextImportImage.setVisible(false);
			contextDelete.setVisible(false);
		}
		tabPane.getTabs().get(3).setDisable(true);
		loggedText.setText("");
		message("You have logged out successfully.");
		logged = null;
	}

	/**
	 * MENUS CONTEXT MENU - on tableview 1, serialized fingerprint data from
	 * selected image (from filebrowser)
	 * 
	 * @param event
	 */
	@FXML
	public void contextTableImage(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Image read");
		fileChooser.setInitialDirectory(new File(System.getProperty("user.dir"),
				"\\src\\fingerprints"));
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("*.bmp", "*.bmp"));
		File file = fileChooser.showOpenDialog(EntranceSystem.getPrimaryStage());
		if (file != null) {
			try {
				String fileName = file.toString().replace(userDir, "");
				ip = new ImageProcessing(fileName);
				List<Minutiae> sortedList = ip.getFinalMinutiaeSet();
				String candidateSignature = minutiaeListToString(sortedList);
				System.out.println("Candidate image signature: " + candidateSignature);
				Person p = new Person();
				p.setId(selectedId);
				p.setFirstname(editFirstname.getText());
				p.setLastname(editLastname.getText());
				p.setFinger(candidateSignature);
				p.setOccupation(editOccupation.getText());
				p.setPassword(editPassword.getText());
				database.update(p);
				refresh();
			} catch (Exception e) {
				System.out.println(e.getMessage() + e);
				System.out.println("Image file reading error");
			}
			refresh();
		}
	}

	/**
	 * 2, delete the selected record of tableview by clicking
	 * 
	 * @param event
	 */
	@FXML
	public void contextTableDelete(ActionEvent event) {
		Person t = table.getSelectionModel().getSelectedItem();
		database.remove(t);
		refresh();
	}

	/**
	 * MENUBAR 1, File menu -> 1th submenu -> open new verification window
	 * 
	 * @param event
	 */
	@FXML
	public void menuVerify(ActionEvent event) {
		staticVerify = new Verify();
		staticVerify.showAndWait();
		if (buttonVerifyClicked) {
			staticVerify.close();
			tabPane.getSelectionModel().select(1);
			flagVerify = true;
		}
	}

	/**
	 * 1, File menu -> 2th submenu -> open login window
	 * 
	 * @param event
	 */
	@FXML
	public void menuLogin(ActionEvent event) {
		staticLogin = new Login();
		staticLogin.showAndWait();
		if (buttonLoginClicked) {
			tabPane.getTabs().get(3).setDisable(false);
			tabPane.getSelectionModel().select(3);
			editFirstname.setText(logged.getFirstname());
			editLastname.setText(logged.getLastname());
			editOccupation.setText(logged.getOccupation());
			editPassword.setText(logged.getPassword());
			editFinger.setText(logged.getFinger());
			String text = "Logged user: " + logged.getLastname();
			loggedText.setText(text);
			if (logged.getLastname().equals("admin")) {
				contextImportImage.setVisible(true);
				contextDelete.setVisible(true);
			}
		}
		staticLogin.close();
	}

	/**
	 * 1, File menu -> 3th submenu -> exit from app
	 * 
	 * @param event
	 */
	@FXML
	public void menuExit(ActionEvent event) {
		Platform.exit();
	}

	/*Export to *.csv, custom settings: column enclosed / escaped by ' char,
	 * remove the first row (column name) manually */
	/**
	 * 2, Import menu -> 1th submenu -> import database file from file browser
	 * 
	 * @param event
	 */
	@FXML
	public void menuImportDB(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Database import");
		fileChooser.setInitialDirectory(new File(System.getProperty("user.home"), "eclipse-workspace"));
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("*.csv", "*.csv"));
		File file = fileChooser.showOpenDialog(EntranceSystem.getPrimaryStage());
		if (file != null) {
			try {
				List<String> reading = Files.readAllLines(Paths.get(file.getPath()));
				for (String line : reading) {
					String lineCsv = line.replace("'", "");
					String[] split = lineCsv.split(",");
					Person p = new Person(split[0], split[1], split[2], split[3], split[4], split[5]);
					database.add(p);
				}
			} catch (Exception e) {
				System.err.println("Data reading error");
			}
			refresh();
		}
	}

	/**
	 * setting the actual values of tableview
	 * 
	 */
	public void setTableData() {
		tId.setCellValueFactory(cellData -> cellData.getValue().idProperty());
		tFirstname.setCellValueFactory(cellData -> cellData.getValue().firstnameProperty());
		tLastname.setCellValueFactory(cellData -> cellData.getValue().lastnameProperty());
		tOccupation.setCellValueFactory(cellData -> cellData.getValue().occupationProperty());
		table.setItems(personData);
	}

	/**
	 * actualize the content of tableview
	 * 
	 */
	private void refresh() {
		personData = database.get();
		table.setItems(personData);
	}

	/**IDENTIFY (1:N) matching
	 * matching the enrolled element to all elements of database
	 * @param event
	 */
	@FXML
	public void buttonIdentify(ActionEvent event) {
		if (!flagVerify) {
		String text = "";
		try {
			if (selectedFile != null || actualFinger != null) {
				List<String> names = new ArrayList<String>();
				double threshold = 30; // corresponds to FMR (false match rate) 0.01%
				if (selectedFile != null) {
					String fileName = selectedFile.toString().replace(userDir, "");
					ip = new ImageProcessing(fileName);
					text += "File selected: " + fileName + "\n";
				}
				else if (actualFinger != null) {
					ip = new ImageProcessing(actualFinger);
				}
				List<Minutiae> sortedList = ip.getFinalMinutiaeSet();
				String candidateSignature = minutiaeListToString(sortedList);
				System.out.println(candidateSignature);
				FingerprintTemplate candidate = new FingerprintTemplate().deserialize(candidateSignature);
				for (Person p : personData) {
					double score = 0;
					if (p.getFinger() != null && !p.getFinger().equals("")) {
						FingerprintTemplate template = new FingerprintTemplate().deserialize(p.getFinger());
						score = new FingerprintMatcher().index(template).match(candidate);
					}
					boolean matches = score >= threshold;
					text += "score: " + String.valueOf(score) + "\t";
					if (matches) {
						text += "ID: " + p.getId() + " Name: " + p.getLastname() + " matches\n";
						names.add(p.getLastname());
					} else {
						text += "ID: " + p.getId() + " Name: " + p.getLastname() + " doesn't match\n";
					}
				}
				text += "Person(s) with matching finger\n";
				for (String str : names) {
					text += str;
					text += "\n";
				}
				System.out.println(text);
				textBox.setText(text);
			}else {
				message("First enroll fingerprint");
			}
		}catch(Exception e) {
			System.out.println(e.getMessage() + e);
			System.out.println("Attention! File reading error");
		}
		} else {
			message("You can not match during identification.");
		}
	}

	/**
	 * VERIFY (1:1 match)
	 * mathching the logged user to one specified element of database
	 * 
	 * @param event
	 */
	@FXML
	public void buttonVerify(ActionEvent event) {
		String text = "";
		Person person = null;
		try {
			for (Person p : personData) {
				if (p.getLastname().equals(userName)) {
					person = p;
				}
			}
			if (person == null) {
				message("Name does not exist in database");
			} else {
				double threshold = 30; // corresponds to FMR (false match rate) 0.01%
				if (actualFinger != null) {
					ip = new ImageProcessing(actualFinger);
					List<Minutiae> sortedList = ip.getFinalMinutiaeSet();
					String candidateSignature = minutiaeListToString(sortedList);
					FingerprintTemplate candidate = new FingerprintTemplate().deserialize(candidateSignature);
					FingerprintTemplate template = new FingerprintTemplate().deserialize(person.getFinger());
					double score = new FingerprintMatcher() // matching factor
							.index(template).match(candidate);
					boolean matches = score >= threshold;
					text += "score: " + score + "\t";
					if (matches) {
						text += "YOUR NAME IS: " + person.getLastname() + "\n" + "ACCESS PERMITTED\n";
					} else {
						text += "YOUR NAME IS: " + person.getLastname() + "\n" + "ACCESS DENIED\n";
					}
					System.out.println(text);
					textBox.setText(text);
					flagVerify = false;
					actualFinger = null;
					probeImage.setImage(null);
				} else {
					message("First enroll fingerprint");
					tabPane.getSelectionModel().select(0);
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage() + e);
			System.out.println("File reading error");
		}
	}

	/**
	 * choose the selected file from filebrowser
	 * 
	 * @return
	 */
	public File chooseFile() {
		File file = null;
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Image read");
		fileChooser.setInitialDirectory(
				new File(System.getProperty("user.dir"), "\\"));
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("*.bmp", "*.bmp"));
		file = fileChooser.showOpenDialog(EntranceSystem.getPrimaryStage());
		return file;
	}

	/**
	 * load the saved fingerprint image from filebrowser to match to other records
	 * from database
	 * 
	 * @param event
	 */
	@FXML
	public void buttonLoad(ActionEvent event) {
		if (!flagVerify) {
		probeImage.setImage(null);
		selectedFile = chooseFile();
		FileInputStream inputstream;
		String fileName = null;
		if (selectedFile != null) {
			try {
				fileName = selectedFile.toString().replace(userDir, "");
				inputstream = new FileInputStream(fileName);
				Image image = new Image(inputstream);
				probeImage.setImage(image);
				probeImage.setVisible(true);
				actualFinger = null;
			} catch (Exception e) {
				System.out.println(e.getMessage() + "Image show problems");
			}
		}
		} else {
			message("You can not load image during identification.");
		}
	}

	/****************** auxiliary methods *******************/

	/**
	 * confirmation
	 * 
	 * @return true if the confirmation is approved
	 */
	public boolean confirmation() {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("CONFIRMATION");
		alert.setHeaderText("Do you really want to preserve image?");
		Optional<ButtonType> reply = alert.showAndWait();
		if (reply.get() == ButtonType.OK) {
			return true;
		} else if (reply.get() == ButtonType.CANCEL) {
			return false;
		} else {
			return false;
		}
	}

	/**
	 * display a given text message with alert dialog
	 * 
	 * @param text
	 */
	public static void message(String text) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Information");
		alert.setHeaderText("INFO");
		alert.setContentText(text);
		alert.showAndWait();
	}

	/**
	 * set the parameters of the port
	 * 
	 */
	private void options() {
		addBaudRate.getItems().addAll(Options.BAUDRATE);
		addPort.getItems().addAll(Options.PORT);
	}

	/**
	 * visualize as image a Mat object
	 * 
	 * @param input
	 * @param title
	 * @throws Exception
	 */
	public static void imshow(Mat input, String title) throws Exception {
		Mat resized = new Mat();
		Imgproc.resize(input, resized, new Size(450, 500));
		WritableImage writableImage = loadImage(resized);
		ImageView imageView = new ImageView(writableImage);
		imageView.setFitHeight(500);
		imageView.setFitWidth(450);
		imageView.setPreserveRatio(true);
		Group group = new Group(imageView);
		Scene scene = new Scene(group, 460, 510);
		Stage stage = new Stage();
		stage.setTitle(title);
		stage.setScene(scene);
		stage.show();
	}

	/**
	 * 1. Image processing menu: All images
	 * 
	 * @param event
	 * @throws Exception
	 */
	@FXML
	void menuAll(ActionEvent event) throws Exception {
		if (ip == null) {
			message("First enroll your finger!!");
		} else {
			try {
				imshow(ip.getSource(), "0 - Original");
				imshow(ip.getEqualizedRidge(), "1 - Equalized");
				imshow(ip.getNormalizedRidgeShow(), "2 - Normalized");
				imshow(ip.getOrientation(), "3 - With orient field");
				imshow(ip.getRidgeOrientationShow(), "4 - Orientation");
				imshow(ip.getSegmentedRidge(), "5 - Segmented");
				imshow(ip.getFilteredRidgeShow(), "6 - Filtered");
				imshow(ip.getOpenedRidge(), "7 - Thinned");
				imshow(ip.getBinarizedRidge(), "8 - Binarized");
				imshow(ip.getWithAnglesRidge(), "9 - All candidate minutiae");
				imshow(ip.getMinutiaeExtractedRidge(), "9 - Minutiae Extracted");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 2. Image processing menu: original image
	 * 
	 * @param event
	 * @throws Exception
	 */
	@FXML
	void menuOriginal(ActionEvent event) throws Exception {
		if (ip == null) {
			message("First enroll your finger!!");
		} else {
			try {
				imshow(ip.getSource(), "0 - Original");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 3. Image processing menu: equalized image
	 * 
	 * @param event
	 * @throws Exception
	 */
	@FXML
	void menuEqualize(ActionEvent event) throws Exception {
		if (ip == null) {
			message("First enroll your finger!!");
		} else {
			try {
				imshow(ip.getEqualizedRidge(), "1 - Equalized");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 4. Image processing menu: normalized image
	 * 
	 * @param event
	 * @throws Exception
	 */
	@FXML
	void menuNormalize(ActionEvent event) throws Exception {
		if (ip == null) {
			message("First enroll your finger!!");
		} else {
			try {
				imshow(ip.getNormalizedRidgeShow(), "2 - Normalized");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 5. Image processing menu: image with orientation field
	 * 
	 * @param event
	 * @throws Exception
	 */
	@FXML
	void menuWithOrientField(ActionEvent event) throws Exception {
		if (ip == null) {
			message("First enroll your finger!!");
		} else {
			try {
				imshow(ip.getOrientation(), "3 - With orient field");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * 6. Image processing menu: pixel wise orientation of image
	 * 
	 * @param event
	 * @throws Exception
	 */
	@FXML
	void menuOrientation(ActionEvent event) throws Exception {
		if (ip == null) {
			message("First enroll your finger!!");
		} else {
			try {
				imshow(ip.getRidgeOrientationShow(), "4 - Orientation");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 7. Image processing menu: segmented image
	 * 
	 * @param event
	 * @throws Exception
	 */
	@FXML
	void menuSegment(ActionEvent event) throws Exception {
		if (ip == null) {
			message("First enroll your finger!!");
		} else {
			try {
				imshow(ip.getSegmentedRidge(), "5 - Segmented");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 8. Image processing menu: filtered image
	 * 
	 * @param event
	 * @throws Exception
	 */
	@FXML
	void menuFilter(ActionEvent event) throws Exception {
		if (ip == null) {
			message("First enroll your finger!!");
		} else {
			try {
				imshow(ip.getFilteredRidgeShow(), "6 - Filtered");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 9. Image processing menu: thinned image
	 * 
	 * @param event
	 * @throws Exception
	 */
	@FXML
	void menuThin(ActionEvent event) throws Exception {
		if (ip == null) {
			message("First enroll your finger!!");
		} else {
			try {
				imshow(ip.getOpenedRidge(), "7 - Thinned");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 10. Image processing menu: binarized image
	 * 
	 * @param event
	 * @throws Exception
	 */
	@FXML
	void menuBinarize(ActionEvent event) throws Exception {
		if (ip == null) {
			message("First enroll your finger!!");
		} else {
			try {
				imshow(ip.getBinarizedRidge(), "8 - Binarized");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 11. Image processing menu: all candidate minutiae
	 * 
	 * @param event
	 * @throws Exception
	 */
	@FXML
	void menuMinutiaeAll(ActionEvent event) throws Exception {
		if (ip == null) {
			message("First enroll your finger!!");
		} else {
			try {
				imshow(ip.getWithAnglesRidge(), "9 - All candidate minutiae");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 12. Image processing menu: minutiae extracted
	 * 
	 * @param event
	 * @throws Exception
	 */
	@FXML
	void menuMinutiaeExtracted(ActionEvent event) throws Exception {
		if (ip == null) {
			message("First enroll your finger!!");
		} else {
			try {
				imshow(ip.getMinutiaeExtractedRidge(), "10 - Minutiae Extracted");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * serializes each record of the minutiae array converts to only one String
	 * value for database
	 * 
	 * @param list
	 * @return
	 */
	public String minutiaeListToString(List<Minutiae> list) {
		String str = "";
		str += "width:256 height:288 ";
		for (Minutiae m : list) {
			str += (int) m.getLocation().x;
			str += "#";
			str += (int) m.getLocation().y;
			str += "#";
			str += m.getOrientation();
			str += "#";
			str += m.getType();
			str += ";";
		}
		return str;
	}

	/**
	 * converts to writable image suitable for visualizating from Mat object
	 * 
	 * @param input
	 * @return
	 * @throws Exception
	 */
	public static WritableImage loadImage(Mat input) throws Exception {
		MatOfByte matOfByte = new MatOfByte();
		Imgcodecs.imencode(".bmp", input, matOfByte);
		byte[] byteArray = matOfByte.toArray();
		InputStream in = new ByteArrayInputStream(byteArray);
		BufferedImage bufImage = ImageIO.read(in);
		WritableImage writableImage = SwingFXUtils.toFXImage(bufImage, null);
		return writableImage;
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		database.createTable();
		contextImportImage.setVisible(false);
		contextDelete.setVisible(false);
		getValueViaClick();
		setTableData();
		options();
		addBaudRate.getSelectionModel().select(3);
		addPort.getSelectionModel().select(7);
	}
}
