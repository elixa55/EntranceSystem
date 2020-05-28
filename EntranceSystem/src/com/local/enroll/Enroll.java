package enroll;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.CharBuffer;

import jssc.SerialPort;
import jssc.SerialPortException;
public class Enroll {


	public static int width = 256;
	public static int height = 288;
	public static int depth = 8;
	public static byte[] array;
	public static String staticText;

	

	public static void main(String[] args) throws SerialPortException, InterruptedException{
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		String name = "ujabb.bmp";
            	System.out.println("Everything is created");
	}
}
