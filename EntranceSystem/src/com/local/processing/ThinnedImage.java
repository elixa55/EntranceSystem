package processing;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ThinnedImage {
	public Mat thinned;
	public double[][] pixels;
	public static int width = 256;
	public static int height = 288;
	public static int channel = 3;
	public static int matrixType = CvType.CV_8UC3; // 16


	/** constructor 
	 * from binarized matrix convert to skeleton matrix in member thinned
	 * deleting the contour pixels around the shapes
	 * @param input: a binarized image
	 * @throws IOException
	 */
	public ThinnedImage(Mat input) throws IOException {
		this.pixels = new double[height][width];
		Set<Point> set = new HashSet<>();
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				double[] data = input.get(i, j);
				this.pixels[i][j] = data[0];
			}
		}
		this.thinned = new Mat(input.size(), input.type());
		do {
			set.clear();
			for (int i = 1; i < height - 1; i++) {
				for (int j = 1; j < width - 1; j++) {
					if (this.pixels[i][j] == 0) {
						if (step1(j, i)) {
								set.add(new Point(j, i));
						}
					}
				}
			}
			if (set.size() != 0) {
				for (Point p : set) {
					this.pixels[(int) p.y][(int) p.x] = 255;
				}
			}
			set.clear();
			for (int i = 1; i < height - 1; i++) {
				for (int j = 1; j < width - 1; j++) {
					if (this.pixels[i][j] == 0) {
						if (step2(j, i)) {
								set.add(new Point(j, i));
						}
					}
				}
			}
			if (set.size() != 0) {
				for (Point p : set) {
					this.pixels[(int) p.y][(int) p.x] = 255;
				}
			}
		} while (set.size() != 0);
		this.thinned = arrayToMat(this.pixels);
	}

	/** returns the numbers of black neighbours around the given pixel (8-connected)
	 * @param j
	 * @param i
	 * @return
	 */
	public int neighboursCalculation(int j, int i) {
		int sum = 0;
		for (int u = i - 1; u < i + 2; u++) {
			for (int v = j - 1; v < j + 2; v++) {
				if (!(i == u && j == v)) {
					if (this.pixels[u][v] == 0) {
						sum++;
					}
				}
			}
		}
		return sum;
	}

	/** returns the numbers of 0 to 1 transitions around the given pixel (8-connected)
	 * @param j
	 * @param i
	 * @return
	 */
	public int transitionsCalculation(int j, int i) {
		int sum = 0;
		double P2 = this.pixels[i - 1][j];
		double P3 = this.pixels[i - 1][j + 1];
		double P4 = this.pixels[i][j + 1];
		double P5 = this.pixels[i + 1][j + 1];
		double P6 = this.pixels[i + 1][j];
		double P7 = this.pixels[i + 1][j - 1];
		double P8 = this.pixels[i][j - 1];
		double P9 = this.pixels[i - 1][j - 1];
		if (P2 == 255 && P3 == 0)
			sum++;
		if (P3 == 255 && P4 == 0)
			sum++;
		if (P4 == 255 && P5 == 0)
			sum++;
		if (P5 == 255 && P6 == 0)
			sum++;
		if (P6 == 255 && P7 == 0)
			sum++;
		if (P7 == 255 && P8 == 0)
			sum++;
		if (P8 == 255 && P9 == 0)
			sum++;
		if (P9 == 255 && P2 == 0)
			sum++;
		return sum;
	}

	/** double matrix of 2 dimensions converts to Mat object
	 * @param a
	 * @return
	 */
	public Mat arrayToMat(double[][] a) {
		Mat m = new Mat(height, width, matrixType);
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				double[] data = new double[channel];
				for (int k = 0; k < channel; k++) {
					data[k] = a[i][j];
				}
				m.put(i, j, data);
			}
		}
		return m;
	}
	
	

	/** The first sub iteration of the validation of pixels to delete (black-> white)
	 * @param j
	 * @param i
	 * @return
	 */
	public boolean step1(int j, int i) {
		boolean isChanged = false;
		double P2 = this.pixels[i - 1][j];
		double P4 = this.pixels[i][j + 1];
		double P6 = this.pixels[i + 1][j];
		double P8 = this.pixels[i][j - 1];
		int neighboursCount = neighboursCalculation(j, i);
		if ((2 <= neighboursCount && neighboursCount <= 6) && (transitionsCalculation(j, i) == 1)
				&& (P4 == 255 || P6 == 255 || (P2 == 255 && P8 == 255))) {
			isChanged = true;
		}
		return isChanged;
	}

	/** The second sub iteration of the validation of pixels to delete (black-> white)
	 * @param j
	 * @param i
	 * @return
	 */
	public boolean step2(int j, int i) {
		boolean isChanged = false;
		double P2 = this.pixels[i - 1][j];
		double P4 = this.pixels[i][j + 1];
		double P6 = this.pixels[i + 1][j];
		double P8 = this.pixels[i][j - 1];
		int neighboursCount = neighboursCalculation(j, i);
		if ((2 <= neighboursCount && neighboursCount <= 6) && (transitionsCalculation(j, i) == 1)
				&& (P2 == 255 || P8 == 255 || (P4 == 255 && P6 == 255))) {
			isChanged = true;
		}
		return isChanged;
	}

	public boolean ab(int j, int i) {
		boolean isChanged = false;
		int neighboursCount = neighboursCalculation(j, i);
		if ((2 <= neighboursCount && neighboursCount <= 6) && (transitionsCalculation(j, i) == 1)) {
			isChanged = true;
		}
		return isChanged;
	}

	public boolean bc1(int j, int i) {
		boolean isChanged = false;
		double P2 = this.pixels[i - 1][j];
		double P4 = this.pixels[i][j + 1];
		double P6 = this.pixels[i + 1][j];
		double P8 = this.pixels[i][j - 1];
		if (P4 == 255 || P6 == 255 || (P2 == 255 && P8 == 255)) {
			isChanged = true;
		}
		return isChanged;
	}

	public boolean bc2(int j, int i) {
		boolean isChanged = false;
		double P2 = this.pixels[i - 1][j];
		double P4 = this.pixels[i][j + 1];
		double P6 = this.pixels[i + 1][j];
		double P8 = this.pixels[i][j - 1];
		if (P2 == 255 || P8 == 255 || (P4 == 255 && P6 == 255)) {
			isChanged = true;
		}
		return isChanged;
	}
	
	public void circle(int i, int j) {
		Imgproc.circle(this.thinned, new Point(i, j), 3, new Scalar(0, 0, 255));
	}
}
