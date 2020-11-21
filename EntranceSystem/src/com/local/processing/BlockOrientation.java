package processing;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import static processing.ImageProcessing.*;

public class BlockOrientation {

	/**
	 * static fields
	 * 
	 */
	public static List<MatOfPoint> allContour = null;
	public static List<MatOfPoint> roiContour = null;

	/**
	 * class variables
	 * 
	 */
	public List<Block> list;
	public Mat orientation;
	public Mat mask;

	/**
	 * constructor
	 * 
	 * @param input
	 * @param isRidge
	 * @throws IOException
	 */
	public BlockOrientation(Mat input, boolean isRidge) throws IOException {
		this.list = new ArrayList<Block>(height / BLOCKSIZE * width / BLOCKSIZE);
		computeBlocks(input, isRidge); // 2. list of 288 blocks' data: mean, var, angle, coh, sign of std
		List<Mat> matList = matToBlockList(input);
		calculateHistogram(matList); // 3. list of blocks histogram from matrix list
	}

	/**
	 * calculates the data members of the Blockorientation class
	 * 
	 * @param input
	 * @param isRidge
	 * @throws IOException
	 */
	public void computeBlocks(Mat input, boolean isRidge) throws IOException {
		int dark;
		int light;
		int widthGrid = width / BLOCKSIZE;
		int heightGrid = height / BLOCKSIZE;
		MatOfDouble mean = new MatOfDouble(0);
		MatOfDouble std = new MatOfDouble(0);
		Mat block;
		Rect roiRectangle;
		double localMean;
		double localVar;
		double theta;
		double coherence;
		boolean darkness;
		for (int i = 1; i <= heightGrid; i++) {
			for (int j = 1; j <= widthGrid; j++) {
				dark = 0;
				light = 0;
				roiRectangle = new Rect((BLOCKSIZE) * (j - 1), (BLOCKSIZE) * (i - 1), BLOCKSIZE, BLOCKSIZE);
				block = input.submat(roiRectangle);
				Core.meanStdDev(block, mean, std);
				localMean = mean.toArray()[0];
				localVar = std.toArray()[0];
				Mat sobel_x = new Mat(block.size(), block.type());
				Mat sobel_y = new Mat(block.size(), block.type());
				Imgproc.Sobel(block, sobel_x, CvType.CV_32F, 1, 0, 3, 1, 0, Core.BORDER_DEFAULT);
				Imgproc.Sobel(block, sobel_y, CvType.CV_32F, 0, 1, 3, 1, 0, Core.BORDER_DEFAULT);
				Mat x_square = new Mat(height, width, CvType.CV_32FC1);
				Mat y_square = new Mat(height, width, CvType.CV_32FC1);
				Mat vx = new Mat(height, width, CvType.CV_32FC1);
				Mat vy = new Mat(height, width, CvType.CV_32FC1);
				Mat denominatore = new Mat(height, width, CvType.CV_32FC1);
				Mat thetaMat = new Mat(height, width, CvType.CV_32FC1);
				Scalar duplex = Scalar.all(2);
				Core.multiply(sobel_x, sobel_x, x_square);
				Core.multiply(sobel_y, sobel_y, y_square);
				Core.multiply(sobel_x, sobel_y, vx);
				Core.multiply(vx, duplex, vx);
				Core.subtract(x_square, y_square, vy);
				Core.add(x_square, y_square, denominatore);
				Scalar sum_x = Core.sumElems(vx);
				Scalar sum_y = Core.sumElems(vy);
				Scalar sum_x_square = Core.sumElems(x_square);
				Scalar sum_y_square = Core.sumElems(y_square);
				theta = 0.5 * Math.atan2(sum_x.val[0], sum_y.val[0]);
				theta = Math.toDegrees(theta);
				double sum_denominatore = sum_x_square.val[0] + sum_y_square.val[0];
				if (sum_denominatore == 0) {
					coherence = 0;
				} else {
					coherence = Math.sqrt(Math.pow(sum_x.val[0], 2) + Math.pow(sum_y.val[0], 2)) / sum_denominatore;
				}
				for (int k = 0; k < block.height(); k++) {
					for (int l = 0; l < block.width(); l++) {
						double[] d = block.get(k, l);
						if (isRidge) {
							if (d[0] < localMean) {
								dark++;
							} else {
								light++;
							}
						} else {
							if (d[0] > localMean) {
								light++;
							} else {
								dark++;
							}
						}
					}
				}
				darkness = (light < dark) ? true : false;
				Block blockData = new Block(localMean, localVar, theta, coherence, darkness);
				this.list.add(blockData);
			}
		}
	}

	/**
	 * visualizes the orientation of the blocks (16 * 18 = 288)	 1. version - not used
	 * @param input
	 * @param blocklist
	 * @return Mat object for visualization of the orientation of blocks
	 * @throws IOException
	 */
	public Mat drawDirection(Mat input, List<Block> blocklist) throws IOException {
		Mat out = input.clone();
		Imgproc.cvtColor(out, out, Imgproc.COLOR_GRAY2RGB);
		double sum = 0;
		double avg = 0;
		for (Block b : blocklist) {
			sum += b.getCoherence();
		}
		avg = sum / (double) blocklist.size();
		double r = BLOCKSIZE / 3;
		int countBlock = 0;
		double theta = 0;
		double coh = 0;
		for (int j = BLOCKSIZE / 2; j < input.height(); j += BLOCKSIZE) {
			for (int i = BLOCKSIZE / 2; i < input.width(); i += BLOCKSIZE) {
				theta = blocklist.get(countBlock).getOrientationAngle();
				coh = blocklist.get(countBlock).getCoherence();
				Point p1 = new Point(i, j);
				Point p2 = angle(theta + 90, p1, r);
				Point p3 = angle(theta - 90, p1, r);
				if (this != null) {
					if (coh >= avg) { 
						Imgproc.line(out, p1, p2, new Scalar(0, 0, 255), 1);
						Imgproc.line(out, p1, p3, new Scalar(0, 0, 255), 1);
					} else {
						Imgproc.line(out, p1, p2, new Scalar(222, 222, 222), 1);
						Imgproc.line(out, p1, p3, new Scalar(222, 222, 222), 1);
					}
				}
				countBlock++;
			}
		}
		return out;
	}
	
	/**draw the orientation of blocks in base of average orientation of pixels in block
	 * 2. version - is in use
	 * @param input
	 * @param orientation
	 * @return
	 * @throws IOException
	 */
	public Mat avgBlockForDraw(Mat input) throws IOException {
		Mat out = input.clone();
		List<Double> blocklist = new ArrayList<>();
		int widthGrid = width / BLOCKSIZE;
		int heightGrid = height / BLOCKSIZE;
		MatOfDouble mean = new MatOfDouble(0);
		MatOfDouble std = new MatOfDouble(0);
		Rect roi;
		Mat blockArea;
		for (int i = 1; i <= heightGrid; i++) {
			for (int j = 1; j <= widthGrid; j++) {
				roi = new Rect((BLOCKSIZE) * (j - 1), (BLOCKSIZE) * (i - 1), BLOCKSIZE, BLOCKSIZE);
				blockArea = this.orientation.submat(roi);
				Core.meanStdDev(blockArea, mean, std);
				double m = mean.toArray()[0];
				m = Math.toDegrees(m);
				blocklist.add(m);
			}
		}
		boolean isBackground;
		Imgproc.cvtColor(out, out, Imgproc.COLOR_GRAY2RGB);
		double r = BLOCKSIZE / 3;
		int countBlock = 0;
		double fi = 0;
		for (int j = BLOCKSIZE / 2; j < input.height(); j += BLOCKSIZE) {
			for (int i = BLOCKSIZE / 2; i < input.width(); i += BLOCKSIZE) {
				fi = blocklist.get(countBlock);
				isBackground = this.list.get(countBlock).isBackground();
				Point p1 = new Point(i, j);
				Point p2 = angle(fi + 90, p1, r);
				Point p3 = angle(fi - 90, p1, r);
				if (!isBackground) {
					Imgproc.line(out, p1, p2, new Scalar(255, 0, 255), 1);
					Imgproc.line(out, p1, p3, new Scalar(255, 0, 255), 1);
				}
				countBlock++;
			}
		}
		return out;
	}
	
	/**method auxiliary: 
	 * calculate the other Point coordinate of a line session in a given angle
	 * @param fi
	 * @param p1
	 * @param r
	 * @return
	 */
	public static Point angle(double fi, Point p1, double r) {
		// fi += 90;
		Point p2 = new Point();
		fi = Math.toRadians(fi);
		p2.x = Math.cos(fi) * r + p1.x;
		p2.y = Math.sin(fi) * r + p1.y;
		return p2;
	}

	/***********
	 * 4. step: ROI decl. -> segmentation: normalizedRidgeShow -> segmented + mask
	 * segmentation in base of histogram, if in local area normalized histogram
	 * related to one intensity value is more than 0.5, and it is a 'light area'
	 * (greater than global mean) we declare it background, otherwise not -> we
	 * declare the mask as well
	 ************/

	/**decides whether a block in ROI (false) or in background (true)
	 * not absolutly in base of variance of given block
	 * but majority of pixels (histogram)
	 * and variance must be in white region -> background
	 * @param input
	 * @return
	 */
	public Mat correctedBackground(Mat input) {
		Mat out = input.clone();
		int countBlock = 0;
		Map<Double, Integer> map = new TreeMap<>();
		double globalMean = mean(input);
		for (int i = 0; i < height; i += BLOCKSIZE) {
			for (int j = 0; j < width; j += BLOCKSIZE) {
				map = this.list.get(countBlock).getHistogramMap();
				boolean blockContain = false;
				for (int u = i; u < i + BLOCKSIZE; u++) {
					for (int v = j; v < j + BLOCKSIZE; v++) {
						double[] data = out.get(u, v);
						double value = 0;
						boolean contain = false;
						for (Map.Entry<Double, Integer> m : map.entrySet()) {
							double majority = m.getValue() / (double) 256;
							if (majority >= 0.5 && m.getKey() > globalMean) {
								contain = true;
								blockContain = true;
								// value = m.getKey();
								value = 255;
							}
						}
						if (contain) {
							data[0] = value;
							blockContain = true;
						}
						out.put(u, v, data);
					}
				}
				if (blockContain) {
					this.list.get(countBlock).setBackground(true);
				} else {
					this.list.get(countBlock).setBackground(false);
				}
				countBlock++;
			}
		}
		return out;
	}

	/**create the segmentation mask of the image
	 * in base of member of Block class: isBackground
	 * if isBackground is true -> mask will be black -> background
	 * 					  false -> 			   white -> ROI
	 * @param input
	 * @return
	 */
	public Mat mask(Mat input) {
		Mat out = input.clone();
		int countBlock = 0;
		boolean isBackground = false;
		for (int i = 0; i < input.height(); i += BLOCKSIZE) {
			for (int j = 0; j < input.width(); j += BLOCKSIZE) {
				isBackground = this.list.get(countBlock).isBackground();
				for (int u = i; u < i + BLOCKSIZE; u++) {
					for (int v = j; v < j + BLOCKSIZE; v++) {
						double[] data = out.get(i, j);
						if (isBackground) {
							data[0] = 0;
							data[1] = 0;
							data[2] = 0;
						} else {
							data[0] = 255;
							data[1] = 255;
							data[2] = 255;
						}
						out.put(u, v, data);
					}
				}
				countBlock++;
			}
		}
		return out;
	}

	/**
	 * @param input
	 * @param mask
	 * @throws IOException
	 */
	public void contourMask(Mat input, Mat mask) throws IOException {
		Mat out = input.clone();
		Mat outMask = Mat.zeros(input.size(), input.type());
		Mat gray = new Mat(mask.rows(), mask.cols(), mask.type());
		Mat binary = new Mat(mask.rows(), mask.cols(), mask.type(), new Scalar(0));
		Mat hierarchy = new Mat();
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.cvtColor(mask, gray, Imgproc.COLOR_BGR2GRAY);
		Imgproc.threshold(gray, binary, 100, 255, Imgproc.THRESH_BINARY);
		Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		allContour = new ArrayList<>();
		double max = Double.MIN_VALUE;
		MatOfPoint largestPoly = null;
		for (MatOfPoint contour : contours) {
			double area = Imgproc.contourArea(contour);
			if (area > max) {
				max = area;
				largestPoly = contour;
			}
		}
		allContour.add(largestPoly);
		Imgproc.drawContours(outMask, allContour, 0, Scalar.all(255), -1, Core.BORDER_CONSTANT, hierarchy, 0,
				new Point());
		Imgproc.drawContours(out, allContour, 0, new Scalar(0, 0, 255), 2, Core.BORDER_CONSTANT, hierarchy, 0, new Point());
		// resizeAndShow(out, "4 - Valid ridge contour");
		this.mask = outMask;
	}

	/*********************************************
	 * calculate orientation and filter - 1. version
	 * @throws IOException 
	 * 
	 * ******************************************/
	
	public Mat gaborFilter1(Mat input, Mat orientation) throws IOException {
		Mat frequency = Mat.zeros(input.size(),  input.type());
		Mat blockMatrix = new Mat(input.size(), input.type());
		Mat blockOrientation = new Mat(orientation.size(), orientation.type());
		Mat blockFrequency = new Mat(frequency.size(), frequency.type());
		for (int i = 0; i < height; i += BLOCKSIZE) {
			for (int j = 0; j < width; j += BLOCKSIZE) {
				blockMatrix = input.submat(i, i + BLOCKSIZE, j, j +BLOCKSIZE);
				blockOrientation = orientation.submat(i, i + BLOCKSIZE, j, j + BLOCKSIZE);
				double mean = mean(blockOrientation);
				System.out.println(mean);
				blockFrequency = gaborFilter(blockMatrix, mean);
				blockFrequency.copyTo(frequency.rowRange(i, i + BLOCKSIZE).colRange(j, j + BLOCKSIZE));
			}
		}
		return frequency;
	}
		public Mat gaborFilter(Mat input, double theta) throws IOException {
			// theta - orientation of the normal to the parallel stripes
			int kernel_s = 11; // size of gabor filter
			double sigma = 2; // standard deviation of the gaussian function 
			double lambda = 6; // wavelength of the sinusoidal factor 
			double gamma = 3.5; // spatial aspect ratio 
			double psi = 0; // phase offset 
			Mat g_kernel = Imgproc.getGaborKernel(new Size(kernel_s, kernel_s), sigma, theta, lambda, gamma, psi,
					CvType.CV_32F);
			Mat destination = new Mat();
			Mat filtered = new Mat();
			// Imgproc.cvtColor(input, destination, Imgproc.COLOR_BGR2GRAY);
			Imgproc.cvtColor(input, destination, Imgproc.COLOR_BGR2RGB, 3);
			Imgproc.filter2D(destination, filtered, CvType.CV_8UC3, g_kernel);
			Imgproc.cvtColor(filtered, filtered, Imgproc.COLOR_BGR2GRAY);
			return filtered;
		}
	
	 /***************************
	 * 5/a. step: calculation orientation
	 * least squares method (Hong)
	 * blockSigma = 7, orientSmoothSigma = 7 
	 *  *************************/
	public void ridgeOrientation(Mat ridgeSegment, Mat result, int gradientSigma, int blockSigma,
			int orientSmoothSigma) {
		int rows = ridgeSegment.rows();
		int cols = ridgeSegment.cols();
		int kSize = Math.round(6 * gradientSigma);
		if (kSize % 2 == 0) {
			kSize++;
		}
		Mat kernel = getGaussianKernel(kSize, gradientSigma);
		Mat sobel_x = new Mat(1, 3, CvType.CV_32FC1);
		Mat sobel_y = new Mat(3, 1, CvType.CV_32FC1);
		Imgproc.Sobel(kernel, sobel_x, CvType.CV_32F, 1, 0);
		Imgproc.Sobel(kernel, sobel_y, CvType.CV_32F, 0, 1);

		// vx = 2 * g_x * g_y, vy = g_x^2 - g_y^2
		// theta = 0.5 * arctan(vy / vx) - adott i,j pixelközéppontú blokk 
		Mat f_x = new Mat(kSize, kSize, CvType.CV_32FC1);
		Mat f_y = new Mat(kSize, kSize, CvType.CV_32FC1);
		Imgproc.filter2D(kernel, f_x, CvType.CV_32FC1, sobel_x);
		Imgproc.filter2D(kernel, f_y, CvType.CV_32FC1, sobel_y);
		Mat g_x = new Mat(rows, cols, CvType.CV_32FC1);
		Mat g_y = new Mat(rows, cols, CvType.CV_32FC1);
		Imgproc.filter2D(ridgeSegment, g_x, CvType.CV_32FC1, f_x);
		Imgproc.filter2D(ridgeSegment, g_y, CvType.CV_32FC1, f_y);
		// square of the gradients
		Mat g_x_square = new Mat(rows, cols, CvType.CV_32FC1);
		Mat g_xy = new Mat(rows, cols, CvType.CV_32FC1);
		Mat g_y_square = new Mat(rows, cols, CvType.CV_32FC1);
		Mat vx = new Mat(rows, cols, CvType.CV_32FC1);
		Mat vy = new Mat(rows, cols, CvType.CV_32FC1);
		Core.multiply(g_x, g_x, g_x_square);
		Core.multiply(g_y, g_y, g_y_square);
		Core.multiply(g_x, g_y, g_xy);
		kSize = Math.round(6 * blockSigma);
		if (kSize % 2 == 0) {
			kSize++;
		}
		kernel = getGaussianKernel(kSize, blockSigma);
		Imgproc.filter2D(g_x_square, g_x_square, CvType.CV_32FC1, kernel);
		Imgproc.filter2D(g_y_square, g_y_square, CvType.CV_32FC1, kernel);
		Imgproc.filter2D(g_xy, g_xy, CvType.CV_32FC1, kernel);
		Core.multiply(g_xy, Scalar.all(2), vx);
		Mat denom = new Mat(rows, cols, CvType.CV_32FC1);
		Mat vx_square = new Mat(rows, cols, CvType.CV_32FC1);
		Mat vy_square = new Mat(rows, cols, CvType.CV_32FC1);
		Core.subtract(g_x_square, g_y_square, vy);
		
		// phi_x = cos(2 * theta), phi_y = sin(2*theta)
		Core.multiply(vx, vx, vx_square);
		Core.multiply(vy, vy, vy_square);
		Core.add(vx_square, vy_square, denom);
		Core.sqrt(denom, denom);
		Mat sin2Theta = new Mat(rows, cols, CvType.CV_32FC1);
		Mat cos2Theta = new Mat(rows, cols, CvType.CV_32FC1);
		Core.divide(vx, denom, sin2Theta);
		Core.divide(vy, denom, cos2Theta);
		// low-pass filter
		kSize = Math.round(6 * orientSmoothSigma);
		if (kSize % 2 == 0) {
			kSize++;
		}
		kernel = getGaussianKernel(kSize, orientSmoothSigma);
		Imgproc.filter2D(sin2Theta, sin2Theta, CvType.CV_32FC1, kernel);
		Imgproc.filter2D(cos2Theta, cos2Theta, CvType.CV_32FC1, kernel);
		atan2(sin2Theta, cos2Theta, result);
		Core.multiply(result, Scalar.all(Math.PI / 360.0), result);
		this.orientation = result;
	}
	

	/**Gauss kernel: by x and y components matrix executes real matrix multiply
	 * @param kernel_size
	 * @param sigma
	 * @return
	 */
	public Mat getGaussianKernel(int kernel_size, int sigma) {
		Mat kernelX = Imgproc.getGaussianKernel(kernel_size, sigma, CvType.CV_32FC1);
		Mat kernelY = Imgproc.getGaussianKernel(kernel_size, sigma, CvType.CV_32FC1);
		Mat kernel = new Mat(kernel_size, kernel_size, CvType.CV_32FC1);
		Core.gemm(kernelX, kernelY.t(), 1, Mat.zeros(kernel_size, kernel_size, CvType.CV_32FC1), 0, kernel, 0);
		return kernel;
	}

	/**calculates for each pixel of output matrix the orientation of 
	 * each element of input matrixes 
	 * @param src1
	 * @param src2
	 * @param dst
	 */
	public void atan2(Mat input1, Mat input2, Mat out) {
		for (int i = 0; i < input1.height(); i++) {
			for (int j = 0; j < input2.width(); j++) {
				out.put(i, j, Core.fastAtan2((float) input1.get(i, j)[0], (float) input2.get(i, j)[0]));
			}
		}
	}

	/**********************************
	 * 6. step: calculation frequency
	 * blockSize = 38
	 * windowSize = 5
	 *************************/
	public double ridgeFrequency(Mat input, Mat mask, Mat orientation, Mat frequency,
			int blockSize, int windowSize, int minWaveLength, int maxWaveLength) {
		Mat blockMatrix = new Mat(input.size(), input.type());
		Mat blockOrientation = new Mat(orientation.size(), orientation.type());
		Mat blockFrequency = new Mat(frequency.size(), frequency.type());
		for (int i = 0; i < height - blockSize; i += blockSize) {
			for (int j = 0; j < width - blockSize; j += blockSize) {
				blockMatrix = input.submat(i, i + blockSize, j, j + blockSize);
				blockOrientation = orientation.submat(i, i + blockSize, j, j + blockSize);
				blockFrequency = calculateFrequency(blockMatrix, blockOrientation, windowSize, minWaveLength,
						maxWaveLength);
				blockFrequency.copyTo(frequency.rowRange(i, i + blockSize).colRange(j, j + blockSize));
			}
		}
		Core.divide(mask, Scalar.all(255), mask);
	    Core.multiply(frequency, mask, frequency, 1.0, CvType.CV_32FC1);
        double medianFrequency = medianFrequency(frequency);
        Core.multiply(mask, Scalar.all(medianFrequency), frequency, 1.0, CvType.CV_32FC1);
        return medianFrequency;
	}

	/**
	 * @param block
	 * @param blockOrientation
	 * @param windowSize
	 * @param minWaveLength
	 * @param maxWaveLength
	 * @return
	 */
	private static Mat calculateFrequency(Mat block, Mat blockOrientation, int windowSize, int minWaveLength,
			int maxWaveLength) {
		int rows = block.rows();
		int cols = block.cols();
		Mat orientation = blockOrientation.clone();
		Core.multiply(orientation, Scalar.all(2.0), orientation);
		int orientLength = (int) (orientation.total());
		float[] orientations = new float[orientLength];
		orientation.get(0, 0, orientations);
		double[] sinOrient = new double[orientLength];
		double[] cosOrient = new double[orientLength];
		for (int i = 1; i < orientLength; i++) {
			sinOrient[i] = Math.sin((double) orientations[i]);
			cosOrient[i] = Math.cos((double) orientations[i]);
		}
		float orient = Core.fastAtan2((float) calculateMean(sinOrient), (float) calculateMean(cosOrient)) / (float) 2.0;
		Mat rotated = new Mat(rows, cols, CvType.CV_32FC1);
		Point center = new Point(cols / 2, rows / 2);
		double rotateAngle = ((orient / Math.PI) * (180.0)) + 90.0;
		double rotateScale = 1.0;
		Size rotatedSize = new Size(cols, rows);
		Mat rotateMatrix = Imgproc.getRotationMatrix2D(center, rotateAngle, rotateScale);
		Imgproc.warpAffine(block, rotated, rotateMatrix, rotatedSize, Imgproc.INTER_NEAREST);
		int cropSize = (int) Math.round(rows / Math.sqrt(2));
		int offset = (int) Math.round((rows - cropSize) / 2.0) - 1;
		Mat cropped = rotated.submat(offset, offset + cropSize, offset, offset + cropSize);
		float sum = 0;
		Mat proj = new Mat(1, cropped.cols(), CvType.CV_32FC1);
		for (int c = 1; c < cropped.cols(); c++) {
			sum = 0;
			for (int r = 1; r < cropped.cols(); r++) {
				sum += cropped.get(r, c)[0];
			}
			proj.put(0, c, sum);
		}
		Mat dilateKernel = new Mat(windowSize, windowSize, CvType.CV_32FC1, Scalar.all(1.0));
		Mat dilate = new Mat(1, cropped.cols(), CvType.CV_32FC1);
		Imgproc.dilate(proj, dilate, dilateKernel, new Point(-1, -1), 1);
		double projMean = Core.mean(proj).val[0];
		double projValue;
		double dilateValue;
		final double ROUND_POINTS = 1000;
		ArrayList<Integer> maxind = new ArrayList<Integer>();
		for (int i = 0; i < cropped.cols(); i++) {
			projValue = proj.get(0, i)[0];
			dilateValue = dilate.get(0, i)[0];
			projValue = (double) Math.round(projValue * ROUND_POINTS) / ROUND_POINTS;
			dilateValue = (double) Math.round(dilateValue * ROUND_POINTS) / ROUND_POINTS;
			if (dilateValue == projValue && projValue > projMean) {
				maxind.add(i);
			}
		}
		Mat result = new Mat(rows, cols, CvType.CV_32FC1, Scalar.all(0.0));
		int peaks = maxind.size();
		if (peaks >= 2) {
			double waveLength = (maxind.get(peaks - 1) - maxind.get(0)) / (peaks - 1);
			if (waveLength >= minWaveLength && waveLength <= maxWaveLength) {
				result = new Mat(rows, cols, CvType.CV_32FC1, Scalar.all((1.0 / waveLength)));
			}
		}
		return result;
	}

	/**
	 * @param input
	 * @return
	 */
	private static double calculateMean(double[] input) {
		double sum = 0;
		double avg = 0;
		for (int i = 0; i < input.length; i++) {
			sum += input[i];
		}
		avg = sum / input.length;
		return avg;
	}

	/**
	 * @param image
	 * @return
	 */
	private static double medianFrequency(Mat image) {
		ArrayList<Double> values = new ArrayList<Double>();
		double value = 0;
		for (int r = 0; r < image.rows(); r++) {
			for (int c = 0; c < image.cols(); c++) {
				value = image.get(r, c)[0];
				if (value > 0) {
					values.add(value);
				}
			}
		}
		Collections.sort(values);
		int size = values.size();
		double median = 0;
		if (size > 0) {
			int halfSize = size / 2;
			if ((size % 2) == 0) {
				median = (values.get(halfSize - 1) + values.get(halfSize)) / 2.0;
			} else {
				median = values.get(halfSize);
			}
		}
		return median;
	}
	
	/**
	 * @param image
	 * @return
	 */
	private static double meanFrequency(Mat image) {
		ArrayList<Double> values = new ArrayList<Double>();
		double value = 0;
		for (int i = 0; i < image.height(); i++) {
			for (int j = 0; j < image.width(); j++) {
				value = image.get(i, j)[0];
				if (value > 0) {
					values.add(value);
				}
			}
		}
		double mean = 0;
		double sum = 0;
		for (double d : values)
			sum += d;
		mean = sum / values.size();
		return mean;
	}

	/*************************************
	 * 7. step: ridge Gabor filtering
	 *********************/
	/**
	 * @param input
	 * @param orientation
	 * @param frequency
	 * @param result
	 * @param kx
	 * @param ky
	 * @param median
	 */
	public void ridgeFilter(Mat ridgeSegment, Mat orientation, Mat frequency, Mat result, double kx, double ky,
			double medianFreq) {
		int angleInc = 1; // 3 volt
		int rows = ridgeSegment.rows();
		int cols = ridgeSegment.cols();
		int filterCount = 180 / angleInc;
		Mat[] filters = new Mat[filterCount];
		double sigmaX = kx / medianFreq;
		double sigmaY = ky / medianFreq;
		System.out.println(medianFreq);
		// mat refFilter = exp(-(x. ^ 2 / sigmaX ^ 2 + y. ^ 2 / sigmaY ^ 2) / 2). *
		// cos(2 * pi * medianFreq * x);
		int size = (int) Math.round(3 * Math.max(sigmaX, sigmaY));
		size = (size % 2 == 0) ? size : size + 1;
		int length = (size * 2) + 1;
		Mat x = meshGrid(size);
		Mat y = x.t();
		Mat xSquared = new Mat(length, length, CvType.CV_32FC1);
		Mat ySquared = new Mat(length, length, CvType.CV_32FC1);
		Core.multiply(x, x, xSquared);
		Core.multiply(y, y, ySquared);
		Core.divide(xSquared, Scalar.all(sigmaX * sigmaX), xSquared);
		Core.divide(ySquared, Scalar.all(sigmaY * sigmaY), ySquared);
		Mat refFilterPart1 = new Mat(length, length, CvType.CV_32FC1);
		Core.add(xSquared, ySquared, refFilterPart1);
		Core.divide(refFilterPart1, Scalar.all(-2), refFilterPart1);
		Core.exp(refFilterPart1, refFilterPart1);
		Mat refFilterPart2 = new Mat(length, length, CvType.CV_32FC1);
		Core.multiply(x, Scalar.all(2 * Math.PI * medianFreq), refFilterPart2);
		refFilterPart2 = cos(refFilterPart2);
		Mat refFilter = new Mat(length, length, CvType.CV_32FC1);
		Core.multiply(refFilterPart1, refFilterPart2, refFilter);
		Mat rotated;
		Mat rotateMatrix;
		double rotateAngle;
		Point center = new Point(length / 2, length / 2);
		Size rotatedSize = new Size(length, length);
		double rotateScale = 1.0;
		for (int i = 0; i < filterCount; i++) {
			rotateAngle = -(i * angleInc);
			rotated = new Mat(length, length, CvType.CV_32FC1);
			rotateMatrix = Imgproc.getRotationMatrix2D(center, rotateAngle, rotateScale);
			Imgproc.warpAffine(refFilter, rotated, rotateMatrix, rotatedSize, Imgproc.INTER_LINEAR);
			filters[i] = rotated;
		}
		Mat orientIndexes = new Mat(orientation.rows(), orientation.cols(), CvType.CV_8UC1);
		Core.multiply(orientation, Scalar.all((double) filterCount / Math.PI), orientIndexes, 1.0, CvType.CV_8UC1);
		Mat orientMask;
		Mat orientThreshold;
		orientMask = new Mat(orientation.rows(), orientation.cols(), CvType.CV_8UC1, Scalar.all(0));
		orientThreshold = new Mat(orientation.rows(), orientation.cols(), CvType.CV_8UC1, Scalar.all(0.0));
		Core.compare(orientIndexes, orientThreshold, orientMask, Core.CMP_LT);
		Core.add(orientIndexes, Scalar.all(filterCount), orientIndexes, orientMask);
		orientMask = new Mat(orientation.rows(), orientation.cols(), CvType.CV_8UC1, Scalar.all(0));
		orientThreshold = new Mat(orientation.rows(), orientation.cols(), CvType.CV_8UC1, Scalar.all(filterCount));
		Core.compare(orientIndexes, orientThreshold, orientMask, Core.CMP_GE);
		Core.subtract(orientIndexes, Scalar.all(filterCount), orientIndexes, orientMask);
		Mat value = new Mat(length, length, CvType.CV_32FC1);
		Mat subSegment;
		int orientIndex;
		double sum;
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				if (frequency.get(r, c)[0] > 0 && r > (size + 1) && r < (rows - size - 1) && c > (size + 1)
						&& c < (cols - size - 1)) {
					orientIndex = (int) orientIndexes.get(r, c)[0];
					subSegment = ridgeSegment.submat(r - size - 1, r + size, c - size - 1, c + size);
					Core.multiply(subSegment, filters[orientIndex], value);
					sum = Core.sumElems(value).val[0];
					result.put(r, c, sum);
				} else {
					result.put(r, c, 255);
				}
			}
		}
	}

	/**
	 * @param input
	 * @return
	 */
	private static Mat meshGrid(int input) {
		int size = (input * 2) + 1;
		Mat out = new Mat(size, size, CvType.CV_32FC1);
		int pixel = -input;
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				out.put(j, i, pixel);
			}
			pixel++;
		}
		return out;
	}

	/**
	 * @param input
	 * @return
	 */
	private Mat cos(Mat input) {
        Mat out = new Mat(input.width(), input.height(), CvType.CV_32FC1);
        for (int i = 0; i < input.height(); i++) {
            for (int j = 0; j < input.width(); j++) {
                out.put(j, i, Math.cos(input.get(j, i)[0]));
            }
        }
        return out;
    }


	/****************************
	 * Histogram calculation
	 *******************************/
	/**********
	 * - intensity * frequency
	 */
	public List<Mat> matToBlockList(Mat input) throws IOException {
		List<Mat> out = new ArrayList<Mat>();
		for (int i = 0; i < height; i += BLOCKSIZE) {
			for (int j = 0; j < width; j += BLOCKSIZE) {
				Rect roi = new Rect(j, i, BLOCKSIZE, BLOCKSIZE);
				Mat tempBlock = new Mat(input, roi);
				out.add(tempBlock);
			}
		}
		return out;
	}

	/**
	 * @param matList
	 * @throws IOException
	 */
	public void calculateHistogram(List<Mat> matList) throws IOException {
		for (int i = 0; i < this.list.size(); i++) {
			Histogram histogram = new Histogram(matList.get(i));
			this.list.get(i).setHistogramMap(histogram.histogramMap);
		}
	}

	/*****************************
	 * End of histogram operations
	 *************************/

	/**draws blocks grid (16 * 18 = 288)
	 * @param input
	 * @return
	 */
	public Mat drawGrid(Mat input) {
		Mat out = input.clone();
		for (int j = BLOCKSIZE / 2; j < input.height(); j += BLOCKSIZE) {
			for (int i = BLOCKSIZE / 2; i < input.width(); i += BLOCKSIZE) {
				int a = i - (BLOCKSIZE / 2);
				int b = j - (BLOCKSIZE / 2);
				int c = i + (BLOCKSIZE / 2);
				int d = j + (BLOCKSIZE / 2);
				Point rect1 = new Point(a, b);
				Point rect2 = new Point(c, d);
				Imgproc.rectangle(out, rect1, rect2, new Scalar(150, 240, 150));
			}
		}
		return out;
	}

	/** calculate mean of pixels in matrix
	 * @param input
	 * @return
	 */
	public double mean(Mat input) {
		MatOfDouble mean = new MatOfDouble();
		MatOfDouble deviation = new MatOfDouble();
		Core.meanStdDev(input, mean, deviation);
		double meanValue = mean.get(0, 0)[0];
		return meanValue;
	}

	/** calculate variance in image matrix
	 * @param input
	 * @return 
	 */
	public double variance(Mat input) {
		MatOfDouble mean = new MatOfDouble();
		MatOfDouble deviation = new MatOfDouble();
		Core.meanStdDev(input, mean, deviation);
		double variance = deviation.get(0, 0)[0];
		return variance;
	}

	/** double [] [] 2 dims array to Mat object
	 * @param a
	 * @return
	 */
	public static Mat arrayToMat(double[][] a) {
		Mat m = new Mat(height, width, matrixType);
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				double[] data = new double[3];
				for (int k = 0; k < 3; k++) {
					data[k] = a[i][j];
				}
				m.put(i, j, data);
			}
		}
		return m;
	}

	
	/**
	 * @param m
	 * @return
	 */
	public static double[][] matToArray(Mat m) {
		double[][] out = new double[height][width];
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				double[] data = m.get(i, j);
				if (data[0] == 255)
					out[i][j] = 0; // if pixel white -> 0
				else
					out[i][j] = 1; // if pixel black -> 1
			}
		}
		return out;
	}	
	
	/**
	 * @return
	 */
	public boolean roiCheck() {
		boolean result = false;
		int roiCount = 0;
		for (Block b : this.list) {
			if (!b.isBackground()) {
				roiCount++;
			}
		}
		double percent = (double) roiCount / this.list.size();
		if (percent > 0.4)
			result = true;
		System.out.println(result ? "Image is recoverable" : "Image is unrecoverable");
		return result;
	}
	

}
