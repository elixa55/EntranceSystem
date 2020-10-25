package enroll;

import static processing.ImageProcessing.height;
import static processing.ImageProcessing.width;

import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javafx.scene.image.Image;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortException;

import static controller.ViewController.message;

public class Enroll {

	/**private members of the class
	 * 
	 */
	private String usedBaud;
	private String usedPort;
	private String usedFileName;
	private String text;
	private static byte[] array;
	boolean dataAvailable;

    public void serialEvent(SerialPortEvent event) {
    	System.out.println("action van");
          if(event.isRXCHAR() && event.getEventValue() > 0) {
        	  SerialPort myport = new SerialPort(usedPort);
                dataAvailable = true;
                int bytesCount = event.getEventValue();
                System.out.println("action: " + bytesCount);
                try {
                      System.out.print(myport.readString(bytesCount));
                } catch (SerialPortException ex) {
                      System.out.println("Serial communication error");
                }
          }
          else 
                dataAvailable = false;
          }

	/** getter - setter
	 * @return
	 */
	public String getUsedBaud() {
		return usedBaud;
	}

	public void setUsedBaud(String usedBaud) {
		this.usedBaud = usedBaud;
	}

	public String getUsedPort() {
		return usedPort;
	}

	public void setUsedPort(String usedPort) {
		this.usedPort = usedPort;
	}

	public String getUsedFileName() {
		return usedFileName;
	}

	public void setUsedFileName(String usedFileName) {
		this.usedFileName = usedFileName;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public static byte[] getArray() {
		return array;
	}

	public static void setArray(byte[] array) {
		Enroll.array = array;
	}
	
	/**actually enrolled image loading to ByteArray typed 'array' variable
	 * @param image
	 * @param name
	 */
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
				for (int i = 0; i < width * height; i++) {
					if (i % 2 == 0)
						fileOut.writeByte((byte) 0x00);
					else
						fileOut.writeByte((byte) 0xff);
				}
			} else {
				// for 1 version
				for (int i = 0, j = 0; i < width * height; i += 2, j++) {
					fileOut.writeByte((byte) array[j] & (byte) 0xf0);
					fileOut.writeByte((byte) (array[j] & (byte) 0x0f) << 4);
				}
				// for 2 version
//	                      for (int i = 1, j = 0; i < width * height+1; i += 2, j++) {
//	                            fileOut.writeByte((byte) array[j] & (byte) 0xf0);
//	                            fileOut.writeByte((byte) (array[j] & (byte) 0x0f) << 4);
//	                      }
			}
			fileOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**read data from sensor to 'array' variables - 1 version
	 * @throws SerialPortException
	 * @throws InterruptedException
	 */
	public void readDataFromSensor() throws SerialPortException, InterruptedException {
		int dataBytes = height * width / 2;
		text = "";
		StringBuilder sb = new StringBuilder();
		SerialPort myport = new SerialPort(usedPort);
		myport.openPort();
		myport.setParams(SerialPort.BAUDRATE_57600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
				SerialPort.PARITY_NONE);
		if (myport.isOpened()) {
			System.out.println("The port is opened.");
		}
		Thread.sleep(3000);
		message("Put your finger on sensor\nthen wait a while!");
		while (myport.isOpened()) {
			String curr = myport.readString();
			if (curr != null) {
				sb.append(curr);
				System.out.print(curr + "\n");
				text += curr;
				if (curr.contains("e.") || curr.contains("\n.")) {
					break;
				}
			}
		}
		myport.closePort();
		if (!myport.isOpened()) {
			System.out.println("The port is closed.");
		}
		array = sb.toString().getBytes();
		System.out.println("Nr of sent bytes: " + array.length);
	}

	public static byte[] toByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}
}
