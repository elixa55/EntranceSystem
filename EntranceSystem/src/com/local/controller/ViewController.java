package controller;

import java.io.File;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import entrancesys.EntranceSys;
import java.awt.image.BufferedImage;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import jssc.SerialPortException;
import login.Login;
import main.FingerprintMatcher;
import main.FingerprintTemplate;
import model.Person;
import model.PersonDao;
import model.PersonDaoImpl;
import static login.LoginController.userName;
import static login.LoginController.buttonLoginClicked;
import enroll.Options;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TabPane;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;

public class ViewController implements Initializable {
      // FXML elements
      @FXML
      private TableView<Person> table;
      @FXML
      private TableColumn<Person, String> tId;
      @FXML
      private TableColumn<Person, String> tName;
      @FXML
      private TableColumn<Person, String> tPassword;
      @FXML
      private TableColumn<Person, String> tFinger;
      @FXML
      private ImageView probeImage;
      @FXML
      private Pane imagePane;
      @FXML
      private TextField addName;
      @FXML
      private TextField addPassword;
      @FXML
      private TextField editName;
      @FXML
      private TextArea editFinger;
      @FXML
      private TextField editPassword;
      @FXML
      private TextArea textBox;
      @FXML
      private ComboBox addBaudRate;
      @FXML
      private ComboBox addPort;
      @FXML
      private TextField addFileName;
      @FXML
      private TabPane tabPane;
      @FXML
      private TextArea text;
      
      // global variables
      private String selectedId;
      final private static PersonDao database = new PersonDaoImpl();
      public static ObservableList<Person> personData = database.get();
      private static String actualFinger = null;
      final private static String folderPath = System.getProperty("user.home") + "\\Documents\\NetBeansProjects\\EntranceSys\\";
      private static String usedBaud;
      private static String usedPort;
      private static String usedFileName;
      private static BufferedImage imageToSave;

      final private static int width = 256;
      final private static int height = 288;
      final private static int depth = 8;
      private static byte[] array;
      public static Login staticLog;
      public static boolean flag = false;
      public static boolean dataAvailable = false;
      // actually enrolled image loading to ByteArray typed 'array' variable
      public void createImageByteArray(BufferedImage image, String name) {
            Image im;
            int verticalPixel = 1;
            int horizontalPixel = 1;
            int colorNum = 0;
            int usedColor = 0;
            File file = new File(name);
            byte[] arrayImage = new byte[width * height];
            try {
                  OutputStream streamOut = new FileOutputStream(file);
                  DataOutputStream fileOut = new DataOutputStream(streamOut);
                  fileOut.writeByte((byte) 0x42);
                  fileOut.writeByte((byte) 0x4d);

                  fileOut.writeByte((byte) 0x36);
                  fileOut.writeByte((byte) 0x24);
                  fileOut.writeByte((byte) 0x01);
                  fileOut.writeByte((byte) 0x00);

                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);

                  fileOut.writeByte((byte) 0x36);
                  fileOut.writeByte((byte) 0x04);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);

                  fileOut.writeByte((byte) 0x28);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);

                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x01);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  // height
                  fileOut.writeByte((byte) 0xe0);
                  fileOut.writeByte((byte) 0xfe);
                  fileOut.writeByte((byte) 0xff);
                  fileOut.writeByte((byte) 0xff);

                  fileOut.writeByte((byte) 0x01);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x08);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x01);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x01);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x01);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);
                  fileOut.writeByte((byte) 0x00);

                  fileOut.writeInt(verticalPixel); // 4 SOH + 4 STX
                  fileOut.writeInt(horizontalPixel); // 4 ETX + 4 EOT
                  fileOut.writeInt(colorNum);// 4 ACK + 4 ENQ
                  fileOut.writeInt(usedColor); // 4 BS + 4BEL
                  for (int i = 0; i < 256; i++) {
                        for (int j = 0; j < 4; j++) {
                              fileOut.writeByte(i);
                        }
                  }
                  // gray rectangle
                  if (array == null) {
			for (int i=0;i<width*height;i++) {
                           if (i%2==0)
                                 fileOut.writeByte((byte)0x00);
                           else
                                 fileOut.writeByte((byte)0xff);
			}
                  }
                  else {
                        // for 1 version
                        for (int i = 0, j = 0; i < width * height; i += 2, j++) {
                              fileOut.writeByte((byte) array[j] & (byte) 0xf0);
                              fileOut.writeByte((byte) (array[j] & (byte) 0x0f) << 4);
                        }
                        // for 2 version
//                      for (int i = 1, j = 0; i < width * height+1; i += 2, j++) {
//                            fileOut.writeByte((byte) array[j] & (byte) 0xf0);
//                            fileOut.writeByte((byte) (array[j] & (byte) 0x0f) << 4);
//                      }
                  }
                  fileOut.close();
            } catch (IOException e) {
                  e.printStackTrace();
            }
      }
      
      // read data from sensor to 'array' variables - 1 version
      public void readDataFromSensor1() throws SerialPortException, InterruptedException {
            String s = "";
            StringBuilder sb = new StringBuilder();
            SerialPort myport = new SerialPort(usedPort);
            myport.openPort();
            myport.setParams(SerialPort.BAUDRATE_57600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            if (myport.isOpened()) {
                  System.out.println("A portot megnyitottam.");
            }
            Thread.sleep(3000);
            message("Put your finger on sensor\nthen wait a while!");
            while (myport.isOpened()) {
                  String curr = myport.readString();
                  if (curr != null) {
                        sb.append(curr);
                        System.out.print(curr + "\n");
                        s += curr;
                        if (curr.contains("e.") || curr.contains("\n.")) {
                              break;
                        }
                  }
            }
            myport.closePort();
            if (!myport.isOpened()) {
                  System.out.println("A portot becsuktam.");
            }
            text.setText(s);
            array = sb.toString().getBytes();
            System.out.println("tomb hossz: " + array.length);
      }
      
     // Eventlistener - it is not used
      private static class PortReader implements SerialPortEventListener {
      SerialPort myport;
      PortReader(SerialPort myport) {
            this.myport = myport;
      }
      @Override
      public void serialEvent(SerialPortEvent event) {
            if(event.isRXCHAR() && event.getEventValue() > 0) {
                  dataAvailable = true;
                  int bytesCount = event.getEventValue();
                  try {
                        System.out.print(myport.readString(bytesCount));
                  } catch (SerialPortException ex) {
                        System.out.println("Serial communication error");
                  }
            }
            else 
                  dataAvailable = false;
            }
      }
      
      // read data from sensor to 'array' variables - 2 version
      public void readDataFromSensor2() throws SerialPortException, InterruptedException {
            String s = "";
            StringBuilder sb = new StringBuilder();
            SerialPort myport = new SerialPort(usedPort);
            myport.openPort();
            myport.setParams(SerialPort.BAUDRATE_57600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            //myport.addEventListener(new PortReader());
            if (myport.isOpened()) {
                  System.out.println("A portot megnyitottam.");
            }
            Thread.sleep(3000);
            System.out.println("Put your finger on the sensor fast!!!");
            while (myport.isOpened()) {
                  String curr = myport.readString();
                  do {
                        System.out.print(curr + "\n");
                        s += curr;
                  }  while (curr.equals(Character.toString('\t')));
                  System.out.println("Put your finger on sensor screen");
                  s += "\nPut your finger on sensor screen\nand wait a while...";
                  text.setText(s);
                  message(s);
                  array = myport.readBytes(36865); // 36864 Byte
//                  while (spazio)
//                        myport.readString();
                  myport.closePort();
            }
            if (!myport.isOpened()) {
                  System.out.println("A portot becsuktam.");
            }
            System.out.println("tomb hossz: " + array.length);
            text.clear();
            probeImage.setImage(null);
      }
         
      // Enroll button -> opens given port, get the fingerprint, displays enrolled image
      @FXML
      public void buttonEnroll(ActionEvent event) throws SerialPortException, InterruptedException, IOException {
            probeImage.setImage(null);
            text.clear();
            usedBaud = addBaudRate.getSelectionModel().getSelectedItem().toString();
            usedPort = addPort.getSelectionModel().getSelectedItem().toString();
            imageToSave = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            actualFinger = "actual.bmp";
            BufferedImage im = imageToSave;
            File fileTemp = new File(folderPath + actualFinger);
            if (fileTemp.exists())
                  fileTemp.delete();
            readDataFromSensor1();
            createImageByteArray(im, actualFinger);
            FileInputStream inputstream; 
            try {
                  inputstream = new FileInputStream(folderPath + actualFinger);
                  Image image = new Image(inputstream); 
                  probeImage.setImage(image);
            } catch (FileNotFoundException ex) {
                  Logger.getLogger(ViewController.class.getName()).log(Level.SEVERE, null, ex);
            }
      }
      // save image to given file name
      @FXML
      public void buttonSave(ActionEvent event) throws IOException {
            String subFolder = "src\\fingerprints\\";
            Path fileTemp = Paths.get(folderPath + actualFinger);
            if (confirmation()) {
                  if ((addFileName.getText() == null || addFileName.getText().equals(""))) {
                       message("You should name your file");
                  }
                  else {
                       if (probeImage.getImage() != null) {
                             String validFileName = fileNameCheck(addFileName.getText());
                             if (validFileName != null) {
                                    Path fileToSave = Paths.get(folderPath + subFolder + validFileName);
                                    Files.copy(fileTemp, fileToSave, StandardCopyOption.REPLACE_EXISTING);
                                    message("Image is saved");
                                    addFileName.clear();
                             }
                             else
                                   message("Incorrect filename");
                        }
                        else                               
                              message("First enroll fingerprint");
                  }
            } 
      }
      
      public static String fileNameCheck(String name) {
            String regex = "^[a-zA-Z_]+[a-zA-Z0-9_\\.]*";
            Pattern pattern = Pattern.compile(regex);
            if (pattern.matcher(name).matches()) {
                  int index = name.lastIndexOf(".");
                  String validFileName = name.substring(0, index);
                  validFileName += ".bmp";
                  return validFileName;
            }
           return null;
      }
      // editable fields get clicked row's values
      @FXML
      public void getValueViaClick() {
            table.setOnMouseClicked(e -> {
                  if (!table.getItems().isEmpty()) {
                        if (table.getSelectionModel().getSelectedItem() != null) {
                              Person p = table.getItems().get(table.getSelectionModel().getSelectedIndex());
                              editName.setText(p.getName());
                              editFinger.setText(p.getFinger());
                              editPassword.setText(p.getPassword());
                              selectedId = p.getId();
                        }
                  }
            });
      }
      // adding new record to database (name, password of new employee) - finger field is not editable
      @FXML
      public void buttonAdd(ActionEvent event) {
            Person p = new Person();
            p.setName(addName.getText());
            p.setFinger("");
            p.setPassword(addPassword.getText());
            database.add(p);
            refresh();
      }
      // modify the selected fields' values
      @FXML
      public void buttonEdit(ActionEvent event) {
            Person p = new Person();
            p.setId(selectedId);
            p.setName(editName.getText());
            p.setFinger(editFinger.getText());
            p.setPassword(editPassword.getText());
            database.update(p);
            refresh();
      }
     
      // MENUS
      // CONTEXT MENU - on tableview
      // 1, serialized fingerprint data from selected image (from filebrowser)
      @FXML
      public void contextTableImage(ActionEvent event) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Image read");
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home"), "\\Documents\\NetBeansProjects\\EntranceSys\\src\\fingerprints"));
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("*.bmp", "*.bmp"));
            File file = fileChooser.showOpenDialog(EntranceSys.getPrimaryStage());
            if (file != null) {
                  try {
                        byte[] probeImageToByte = Files.readAllBytes(Paths.get(file.toString()));
                        FingerprintTemplate probeImageToTemplate = new FingerprintTemplate().create(probeImageToByte);
                        String serializedProbe = probeImageToTemplate.serialize();
                        Person p = new Person();
                        p.setId(selectedId);
                        p.setName(editName.getText());
                        p.setFinger(serializedProbe);
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
      // 2, delete the selected record of tableview by clicking
      @FXML
      public void contextTableDelete(ActionEvent event) {
            Person t = table.getSelectionModel().getSelectedItem();
            database.remove(t);
            refresh();
      }
      // MENUBAR
      // 1, File menu -> 1th submenu -> open new identification window
      @FXML
      public void menuLogin(ActionEvent event) {
            staticLog = new Login();
            staticLog.showAndWait();
            if (buttonLoginClicked) {
                  staticLog.close();
                  tabPane.getSelectionModel().select(1);
            }
      }
      // 1, File menu -> 2th submenu -> exit from app
      @FXML
      public void menuExit(ActionEvent event) {
            Platform.exit();
      }
      // 2, Import menu -> 1th submenu -> import database file from file browser
      @FXML
      public void menuImportDB(ActionEvent event) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Database import");
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home"), "eclipse-workspace"));
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("*.csv", "*.csv"));
            File file = fileChooser.showOpenDialog(EntranceSys.getPrimaryStage());
            if (file != null) {
                  try {
                        List<String> reading = Files.readAllLines(Paths.get(file.getPath()));
                        for (String line : reading) {
                              String[] split = line.split(";");
                              Person p = new Person(split[0], split[1], split[2], split[3]);
                              database.add(p);
                        }
                  } catch (Exception e) {
                        System.err.println("Data reading error");
                  }
                  refresh();
            }
      }
      // setting the actual values of tableview
      public void setTableData() {
            tId.setCellValueFactory(cellData -> cellData.getValue().idProperty());
            tName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
            tPassword.setCellValueFactory(cellData -> cellData.getValue().passwordProperty());
            tFinger.setCellValueFactory(cellData -> cellData.getValue().fingerProperty());
            table.setItems(personData);
      }
      // actualize the content of tableview
      private void refresh() {
            personData = database.get();
            table.setItems(personData);
      }
      // matching the enrolled element to all elements of database
      @FXML
      public void buttonMatch(ActionEvent event) {
            String text = "";
            try {
                  List<String> names = new ArrayList<String>();
                  double threshold = 40; // corresponds to FMR (false match rate) 0.01%
                  if (actualFinger != null) {
                        byte[] candidateImageToByte = Files.readAllBytes(Paths.get(actualFinger));
                        FingerprintTemplate candidateImageToTemplate = new FingerprintTemplate().create(candidateImageToByte);
                        for (Person p : personData) {
                              FingerprintTemplate deserialized = new FingerprintTemplate().deserialize(p.getFinger());
                              double score = new FingerprintMatcher() // matching factor - higher -> better
                                      .index(deserialized).match(candidateImageToTemplate);
                              boolean matches = score >= threshold;
                              text += "score: " + score + "\t";
                              if (matches) {
                                    text += "ID: " + p.getId() + " Name: " + p.getName() + " matches\n";
                                    names.add(p.getName());
                              } else {
                                    text += "ID: " + p.getId() + " Name: " + p.getName() + " doesn't match\n";
                              }
                        }
                        text += "Person(s) with matching finger\n";
                        for (String s : names) {
                              text += s;
                              text += "\n";
                        }
                        System.out.println(text);
                        textBox.setText(text);
                  } else {
                        message("First enroll fingerprint");
                  }
            } catch (Exception e) {
                  System.out.println(e.getMessage() + e);
                  System.out.println("File reading error");
            }
      }
      // mathching the actual user to all elements of database
      @FXML
      public void buttonIdentify(ActionEvent event) {
            String text = "";
            Person person = null;
            try {
                  for (Person p : personData) {
                        if (p.getName().equals(userName)) {
                              person = p;
                        }
                  }
                  if (person == null) {
                        message("Name does not exist in database");
                  } else {
                        double threshold = 40; // corresponds to FMR (false match rate) 0.01%
                        if (actualFinger != null) {
                              byte[] candidateImageToByte = Files.readAllBytes(Paths.get(actualFinger));
                              FingerprintTemplate candidateImageToTemplate = new FingerprintTemplate().create(candidateImageToByte);
                              FingerprintTemplate deserialized = new FingerprintTemplate().deserialize(person.getFinger());
                              double score = new FingerprintMatcher() // matching factor
                                      .index(deserialized).match(candidateImageToTemplate);
                              boolean matches = score >= threshold;
                              text += "score: " + score + "\t";
                              if (matches) {
                                    text += "YOUR NAME IS: " + person.getName() + "\n" + "ACCESS PERMITTED\n";
                              } else {
                                    text += "YOUR NAME IS: " + person.getName() + "\n" + "ACCESS DENIED\n";
                              }
                              System.out.println(text);
                              textBox.setText(text);
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
      

      // auxiliary methods
      // confirmation
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
      // display a given text message with alert dialog
      public static void message(String text) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText("INFO");
            alert.setContentText(text);
            alert.showAndWait();
      }
      // write txt file
      public void fileWrite(String whereTo, String what) {
            BufferedWriter bw;
            try {
                  bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(whereTo)));
                  bw.write(what);
                  bw.flush();
                  bw.close();
                  System.out.println(what.toString() + " named file is created");
            } catch (IOException e) {
                  e.getMessage();
            }

      }
      // combobox values 
      private void options() {
            addBaudRate.getItems().addAll(Options.BAUDRATE);
            addPort.getItems().addAll(Options.PORT);
      }
      // overriding initialize method - in each init
      @Override
      public void initialize(URL url, ResourceBundle rb) {
            database.createTable();
            setTableData();
            getValueViaClick();
            options();
            addBaudRate.getSelectionModel().select(3);
            addPort.getSelectionModel().select(7);
      }
}
