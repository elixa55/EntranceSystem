package processing;

import org.opencv.core.Mat;

import processing.Cell;
import static processing.ImageProcessing.width;
import static processing.ImageProcessing.height;
public class LogicMatrix {
	
	/**
	 * member field 
	 */
	private boolean[] array;

	/**constructor 
	 * @param input
	 */
	LogicMatrix(Mat input) {
		this.array = matToOneArray(input);
	}

	/**locate the pixel (2 dimensions - 1 dimension)
	 * @param x
	 * @param y
	 * @return
	 */
	public int offset(int x, int y) {
		return y * width + x;
	}

	/**create a boolean array of 1 dimension from the thinned image
	 * where there was ridge pixel would be true
	 * where there was valley pixel would be false
	 * @param input
	 * @return
	 */
	public boolean[] matToOneArray(Mat input) {
		boolean[] out = new boolean[width * height];
		int countArray = 0;
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				double[] data = input.get(i, j);
				if (data[0] == 255) {
					out[countArray] = false; // if pixel white -> 0 -> false
				} else {
					out[countArray] = true; // if pixel black -> 1 -> true
				}
				countArray++;
			}
		}
		return out;
	}

	/**decides whether the given pixel is a ridge pixel or not
	 * @param x
	 * @param y
	 * @param fallback
	 * @return
	 */
	boolean get(int x, int y, boolean fallback) {
		if (x < 0 || y < 0 || x >= width || y >= height)
			return fallback;
		return array[offset(x, y)];
	}

	/**decides whether the given pixel is a ridge pixel or not
	 * @param at
	 * @param fallback
	 * @return
	 */
	boolean get(Cell at, boolean fallback) {
		return get(at.x, at.y, fallback);
	}
}
