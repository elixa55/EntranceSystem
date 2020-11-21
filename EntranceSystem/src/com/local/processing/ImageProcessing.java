package processing;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;


import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import processing.BlockOrientation;

public class ImageProcessing {

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	public static double[][] pixels;
	public static int width = 256;
	public static int height = 288;
	public static Size sizeShow = new Size(800, 1000);
	public static Size size = new Size(width, height);
	public static int channel = 3;
	public static int matrixType = CvType.CV_8UC3; // 16
	public static int BLOCKSIZE = 16;
	public static double avgRidgeDistance = 12;

	// for calculate orientation
	private int gradientSigma = 1;
	private int blockSigma = 5;
	private int orientSmoothSigma = 5;
	private int frequencyBlockSize = 38;
	private int frequencyWindowSize = 5;
	private int minWaveLength = 3;
	private int maxWaveLength = 15;
	private double filterSize = 0.55;

	private Mat source = new Mat(size, matrixType);
	private Mat equalizedRidge;
	private Mat normalizedRidgeShow;
	private Mat ridgeOrientationShow;
	private Mat orientation;
	private Mat segmentedRidge;
	private Mat filteredRidgeShow;
	private Mat openedRidge;
	private Mat binarizedRidge;
	private Mat withAnglesRidge;
	private Mat minutiaeExtractedRidge;
	private List<Minutiae> finalMinutiaeSet;

	/**
	 * constructor executes the steps of image processing
	 * 
	 * @param input
	 * @throws Exception
	 */
	public ImageProcessing(String input) throws Exception {
		this.source = Imgcodecs.imread(input, 1); // RGB 3 channels (default)

		/***********
		 * 1. step: histogram equalization: source -> equalized
		 ************/
		// resizeAndShow(source, "0 - Ridge Original");
		Imgproc.cvtColor(this.source, this.source, Imgproc.COLOR_RGB2GRAY);
		
		//this.equalizedRidge = equalizeOpenCV(this.source);
		this.equalizedRidge = equalizeAdaptiveOpenCV(this.source);
		// resizeAndShow(equalizedRidge, "1 - Equalized ridge");

		/***********
		 * 2. step: normalize: equalized -> normalized normalized matrix values are
		 * float = value / total intensity values (256) global mean: 0, variance: 1
		 ************/
		Mat floatedRidge = new Mat(size, CvType.CV_32FC1);
		this.equalizedRidge.convertTo(floatedRidge, CvType.CV_32FC1);

		Mat normalizedRidge = new Mat(size, CvType.CV_32FC1);
		normalizeImage(floatedRidge, normalizedRidge);
//		this.normalizedRidgeShow = new Mat(size, CvType.CV_32SC1);
//		Core.normalize(normalizedRidge, this.normalizedRidgeShow, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
		// resizeAndShow(normalizedRidgeShow, "2 - Normalized Ridge");
		this.normalizedRidgeShow = normalize(this.equalizedRidge);
		// resizeAndShow(normalizedRidgeShow, "normalized");
		/***********
		 * 3. step: create instance of BlockOrientation for calculate of local data of
		 * each block: variance, mean, coherence, orientation angle
		 ************/
		BlockOrientation orientationRidge = new BlockOrientation(this.normalizedRidgeShow, true);

		/***********
		 * 4. step: ROI declaration -> segmentation: normalizedRidgeShow -> segmented +
		 * mask segmentation in base of histogram, if in local area normalized histogram
		 * related to one intensity value is more than 0.5, and it is a 'light area'
		 * (greater than global mean) we declare it background, otherwise not -> we
		 * declare the mask as well
		 ************/
		this.segmentedRidge = orientationRidge.correctedBackground(normalizedRidgeShow);
		Core.normalize(this.segmentedRidge, this.segmentedRidge, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
		Imgproc.cvtColor(this.segmentedRidge, this.segmentedRidge, Imgproc.COLOR_GRAY2RGB);
		// segmentedRidge = orientationRidge.drawGrid(segmentedRidge);
		// resizeAndShow(segmentedRidge, "4 - Segmented Ridge");
		Mat maskRidgeInitial = orientationRidge.mask(this.segmentedRidge);
		// resizeAndShow(maskRidgeInitial, "4 - Initial mask");
		// for visualization, and the points of contour put in a MatOfPoint variable
		orientationRidge.contourMask(this.segmentedRidge, maskRidgeInitial);
		Mat maskRidge = orientationRidge.mask;
		// resizeAndShow(maskRidge, "4 - Mask ridge");

//		Mat normalizedMask = new Mat(size, CvType.CV_32FC1);
//		Core.divide(maskRidge, Scalar.all(255), normalizedMask);
		boolean recoverableImage = orientationRidge.roiCheck();
		// to do
//		if (!recoverableImage)
//			return;

		/***********
		 * 5. step: orienation field calculation
		 ************/
		Mat ridgeOrientation = new Mat(size, CvType.CV_32FC1);
		this.ridgeOrientationShow = new Mat(size, CvType.CV_32FC1);
		Imgproc.cvtColor(this.segmentedRidge, this.segmentedRidge, Imgproc.COLOR_RGB2GRAY);
		Imgproc.cvtColor(maskRidge, maskRidge, Imgproc.COLOR_RGB2GRAY);
		orientationRidge.ridgeOrientation(this.segmentedRidge, ridgeOrientation, gradientSigma, blockSigma,
				orientSmoothSigma);
		ridgeOrientation = orientationRidge.orientation;
		Core.normalize(ridgeOrientation, this.ridgeOrientationShow, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
		// resizeAndShow(ridgeOrientationShow, "5 - Oriented ridge");
		this.orientation = new Mat(source.size(), source.type());
		orientation = orientationRidge.avgBlockForDraw(segmentedRidge);
		orientation = orientationRidge.drawGrid(orientation);
		// resizeAndShow(orientation, "5 - Oriented ridge");
		/***********
		 * 6. step: ridge frequency calculation
		 ************/
		Mat ridgeFrequencyRidge = new Mat(size, CvType.CV_32FC1);
		double medianFrequencyRidge = orientationRidge.ridgeFrequency(this.segmentedRidge, maskRidge, ridgeOrientation,
				ridgeFrequencyRidge, frequencyBlockSize, frequencyWindowSize, minWaveLength, maxWaveLength);
//		Mat filteredRidge1 = orientationRidge.gaborFilter1(segmentedRidge, ridgeOrientation);
//		Core.normalize(filteredRidge1, filteredRidge1, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
//		resizeAndShow(filteredRidge1, "Gabor");
		/***********
		 * 7. step: Gabor filtering
		 ************/
		Mat filteredRidge = new Mat(size, CvType.CV_32FC1);
		this.segmentedRidge.convertTo(this.segmentedRidge, CvType.CV_32FC1);
		orientationRidge.ridgeFilter(this.segmentedRidge, ridgeOrientation, ridgeFrequencyRidge, filteredRidge,
				filterSize, filterSize, medianFrequencyRidge);
		this.filteredRidgeShow = new Mat(size, CvType.CV_32FC1);
		Core.normalize(filteredRidge, this.filteredRidgeShow, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
		// resizeAndShow(filteredRidgeShow, "7 - Filtered ridge (Gabor)");

		/***********
		 * 8. step: adaptive binarization
		 ************/
		filteredRidge.convertTo(filteredRidge, CvType.CV_8UC3);
		this.binarizedRidge = binarizeAdaptive(filteredRidge);

		Mat binarizedValley = new Mat(source.size(), source.type());
		Core.bitwise_not(this.binarizedRidge, binarizedValley);
		// resizeAndShow(binarizedValley, "8 - Binarized valley");

		// not adaptive binarize
		// Imgproc.threshold(filteredRidge, binarizedRidge, 0, 255,
		// Imgproc.THRESH_BINARY);
		// resizeAndShow(binarizedRidge, "8 - Binarized ridge");

		/***********
		 * 9. step: thinning 1, thin the binarized ridge image 2, thin the binarized
		 * valley image (= inverse of the binarized ridge image)
		 ************/
		Mat thinnedRidge = thinning(this.binarizedRidge);
		// resizeAndShow(thinnedRidge, "6 - Ridge Thinned");
		Mat invertMask = Extraction.contourNonZeroPoints(thinnedRidge);
		Mat finalBinarizedValley = Extraction.maskInverted(binarizedValley, invertMask);
		// resizeAndShow(finalBinarizedValley, "Segmented bin invert");
		Mat thinnedValley = thinning(finalBinarizedValley);
		// resizeAndShow(thinnedValley, "6 - Valley Thinned");
		/***********
		 * 10. step: preprocessing: small lakes, dots, short ridges elimination from
		 * thinned image
		 ************/
		this.openedRidge = opening(thinnedRidge);
		// resizeAndShow(openedRidge, "7 - Ridge w/o small lakes");
		// avgRidgeDistance = stepsRidge.avgRidgeDistance(openedRidge);
		Mat openedValley = opening(thinnedValley);
		// resizeAndShow(openedValley, "7 - Valley w/o small lakes");

		/***********
		 * 11, step: instanciate the RidgesMap class for mark all possible minutiae -
		 * then calculation of ridges data to Ridges instance for all minutiae 
		 * (from LogicMatrix instance 0-white, 1-black
		 * if minutia is a bifurcation -> members:
		 * terminations (Set<Cell>)
		 * angleTerm (double)
		 * if minutia is a termination -> members:
		 * bifurcations, neighbours, farpoints (Set<Cell>)
		 * angleBif (double)
		 * anglesBif (Set<Double>)
		 * pointpairs (Map<Cell,Cell>)
		 * final result to RidgesMap instance	
		 ************/
		// params: thinnged image
		RidgesMap ridgesMapRidge = new RidgesMap(this.openedRidge);
		RidgesMap ridgesMapValley = new RidgesMap(openedValley);
		ridgesMapRidge.setOrientationForTermination();
		ridgesMapValley.setOrientationForTermination();
		// calculate bifurcation angles in base of termination angles in dual image
		ridgesMapRidge.calculateBifurcation(ridgesMapValley.getMinutiaeMapFinal());
		// visualise ridges line belonging to minutia
		Mat terminationsRidge = ridgesMapRidge.terminationMatrix();
		Mat bifurcationsRidge = ridgesMapRidge.bifurcationMatrix();
//		resizeAndShow(terminationsRidge, "9 - Ridge Terminations");
//		resizeAndShow(bifurcationsRidge, "9 - Ridge Bifurcations");
		Mat terminationsValley = ridgesMapValley.terminationMatrix();
		Mat bifurcationsValley = ridgesMapValley.bifurcationMatrix();
//		resizeAndShow(terminationsValley, "9 - Valley Terminations");
//		resizeAndShow(bifurcationsValley, "9 - Valley Bifurcations");

		/***********
		 * 12. step: false minutiae elimination in Extraction
		 * params: ridge RidgesMap instance and its dual
		 ************/
		Extraction extractionRidge = new Extraction(ridgesMapRidge, ridgesMapValley);
		// visualize the direction of minutiae
		// angle closed to x axis incrementing clockwise
		this.withAnglesRidge = extractionRidge.drawMinutiaeDirection();
		// resizeAndShow(withAnglesRidge, "10 - Ridge w/t angles");
//		Mat allInRidge = extractionRidge.allMinutia();
//		resizeAndShow(allInRidge, "All minutiae in ridge");

		/*********** 16. step: border minutiae exploration ************/
		// mask for ROI area
		Mat nonBlockMask = Extraction.contourNonZeroPoints(openedRidge);
		// resizeAndShow(nonBlockMask, "12 - Ridge Mask from Thinned");
		// reduction of mask
		Mat borderRidgeMask = Extraction.scaleContour(Extraction.roiContour, 0.8, nonBlockMask);
		// resizeAndShow(borderRidgeMask, "12 - Ridge border mask");

		/*********** 13. step: false minutiae elimination ************/
		// 0/1. step: isolated dot
		extractionRidge.eliminationDot();
		// 0/2. step: enclosure
		extractionRidge.eliminationLake();
		// 0/3. step: type changed minutiae
		extractionRidge.changeType();
		extractionRidge.changeTypeInverse();
		extractionRidge.changeType2();
		// 0/3. step: too near terminations
		extractionRidge.eliminationTooNearTerm();
		// 1. step: breaking ridges, gaps
		extractionRidge.eliminationGap();
		// 2. step: spurs
		extractionRidge.eliminationSpur();
		// 3. step: H-points structures
		extractionRidge.eliminationHpoint();
		// 4/1. step: too near minutiae
		extractionRidge.eliminationTooNearMinutiae();
		// 4/2. step: not real minutiae elimination
		// extractionRidge.notRealMinutiae();
		/*********** 15. step: border minutiae elimination ************/
		extractionRidge.borderMask(borderRidgeMask);
		this.minutiaeExtractedRidge = extractionRidge.drawMinutiaeDirection();
		// resizeAndShow(minutiaeExtractedRidge, "13 - Ridge w/o border minutiae");

		// real minutiae set for Controller 
		this.finalMinutiaeSet = sortByMinutiae(ridgesMapRidge.getMinutiaeMapFinal().keySet());
		System.out.println("Final state");
		System.out.println("Terminations Ridges: ");
		int c = 0;
		for (Minutiae m : finalMinutiaeSet) {
			if (m.getType().equals("ending")) {
				c++;
				System.out.println(c + ". elem: " + m.getLocation() + " " + Math.toDegrees(m.getOrientation()));
			}
		}
		c = 0;
		System.out.println("Bifurcations Ridges: ");
		for (Minutiae m : finalMinutiaeSet) {
			if (m.getType().equals("bifurcation")) {
				c++;
				System.out.println(c + ". elem: " + m.getLocation() + " " + Math.toDegrees(m.getOrientation()));
			}
		}

//		Mat combinated = combinatedRidgeValleyFromThinned(openedRidge, openedValley);
//		resizeAndShow(combinated, "Combinated sceleton");
		finalMinutiaeSet = sortByMinutiae(ridgesMapRidge.getMinutiaeMapFinal().keySet());
	}

	/**
	 * getters
	 * 
	 * @return
	 */
	public Mat getSource() {
		return source;
	}

	public Mat getEqualizedRidge() {
		return equalizedRidge;
	}

	public Mat getNormalizedRidgeShow() {
		return normalizedRidgeShow;
	}

	public Mat getRidgeOrientationShow() {
		return ridgeOrientationShow;
	}

	public Mat getOrientation() {
		return orientation;
	}

	public Mat getSegmentedRidge() {
		return segmentedRidge;
	}

	public Mat getFilteredRidgeShow() {
		return filteredRidgeShow;
	}

	public Mat getOpenedRidge() {
		return openedRidge;
	}

	public Mat getBinarizedRidge() {
		return binarizedRidge;
	}

	public Mat getWithAnglesRidge() {
		return withAnglesRidge;
	}

	public Mat getMinutiaeExtractedRidge() {
		return minutiaeExtractedRidge;
	}

	public List<Minutiae> getFinalMinutiaeSet() {
		return finalMinutiaeSet;
	}

	/**
	 * histogram equalizes in Mat object
	 * 
	 * @param input
	 * @return
	 */
	public Mat equalizeOpenCV(Mat input) {
		Mat equalized = new Mat(size, CvType.CV_32FC1);
		Imgproc.equalizeHist(input, equalized);
		return equalized;
	}
	
	/** adaptive histogram equalizes in Mat object
	 * @param input
	 * @return
	 * @throws IOException 
	 */
	public Mat equalizeAdaptiveOpenCV(Mat input) throws IOException {
		Mat equalized = new Mat(size, CvType.CV_32FC1);
		CLAHE clahe = Imgproc.createCLAHE(10, new Size(13, 13));
		clahe.apply(input, equalized);
		Imgproc.equalizeHist(input, equalized);
		return equalized;
	}

	/***********
	 * 2. step: normalization: equalized -> normalized 
	 ************/

	/**
	 * normalizes a Mat object
	 * 
	 * @param src
	 * @param dst
	 */
	public void normalizeImage(Mat src, Mat dst) {
		MatOfDouble mean = new MatOfDouble(0.0);
		MatOfDouble std = new MatOfDouble(0.0);
		Core.meanStdDev(src, mean, std);
		Core.subtract(src, Scalar.all(mean.toArray()[0]), dst);
		Core.meanStdDev(dst, mean, std);
		Core.divide(dst, Scalar.all(std.toArray()[0]), dst);
	}

	/**
	 * thinning operation
	 * 
	 * @param input
	 * @return
	 * @throws IOException
	 */
	public Mat thinning(Mat input) throws IOException {
		ThinnedImage thinnedImage = new ThinnedImage(input);
		Mat out = thinnedImage.thinned;
		return out;
	}

	/**
	 * morphology operation for elimination of small islands, ridge lines (like
	 * noise)
	 * 
	 * @param input
	 * @return
	 * @throws IOException
	 */
	public Mat opening(Mat input) throws IOException {
		Mat out = new Mat(input.size(), input.type());
		Mat kernel = Mat.ones(3, 3, CvType.CV_8U);
		// other way
//			Imgproc.erode(input, out, kernel);
//			Imgproc.dilate(out, out, kernel); 
//			out = thinning(out);
		Imgproc.morphologyEx(input, out, Imgproc.MORPH_OPEN, kernel);
		out = thinning(out);
		return out;
	}

	/********************* for Histogram calculations **************/
	public static double mean(Mat input) {
		MatOfDouble mean = new MatOfDouble();
		MatOfDouble dev = new MatOfDouble();
		Core.meanStdDev(input, mean, dev);
		double M = mean.get(0, 0)[0];
		double D = dev.get(0, 0)[0];
		System.out.println("mean = " + M + ", var = " + D);
		return M;
	}

	public static void sobel(Mat input) throws IOException {
		Mat dx = new Mat(input.size(), input.type());
		Mat dy = new Mat(input.size(), input.type());
		Imgproc.Sobel(input, dx, CvType.CV_32F, 1, 0);
		Imgproc.Sobel(input, dy, CvType.CV_32F, 0, 1);
	}

	/***********
	 * Functions auxiliary
	 ************/

	/**
	 * unifies the two type of thinned image: ridge image - valley image in one
	 * thinned image for visualization
	 * 
	 * @param ridge
	 * @param valley
	 * @return
	 */
	public Mat combinatedRidgeValleyFromThinned(Mat ridge, Mat valley) {
		Mat out = new Mat(ridge.size(), ridge.type());
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				double[] data = ridge.get(i, j);
				double[] outData = new double[ridge.channels()];
				if (data[0] == 0) {
					outData[0] = 254;
					outData[1] = 0;
					outData[2] = 0;
				} else {
					outData[0] = 255;
					outData[1] = 255;
					outData[2] = 255;
				}
				out.put(i, j, outData);
			}
		}
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				double[] data = valley.get(i, j);
				double[] ridgeData = out.get(i, j);
				double[] outData = new double[ridge.channels()];
				if (data[0] == 0 && ridgeData[0] == 254) { 
					outData[0] = 254;
					outData[1] = 0;
					outData[2] = 0;
				} else if (data[0] == 0 && ridgeData[0] == 255) { 
					outData[0] = 255;
					outData[1] = 0;
					outData[2] = 255;
				} else if (data[0] == 255 && ridgeData[0] == 254) { 
					outData[0] = 254;
					outData[1] = 0;
					outData[2] = 0;
				} else if (data[0] == 255 && ridgeData[0] == 255) { 
					outData[0] = 255;
					outData[1] = 255;
					outData[2] = 255;
				} else { 
					outData[0] = 0;
					outData[1] = 0;
					outData[2] = 0;
				}
				out.put(i, j, outData);
			}
		}
		return out;
	}

	/**
	 * calculates the distance between two given points
	 * 
	 * @param a Cell
	 * @param b Cell
	 * @return
	 */
	public static double distanceEuclidean(Cell a, Cell b) {
		double x_quad = Math.pow((b.x - a.x), 2);
		double y_quad = Math.pow((b.y - a.y), 2);
		return Math.sqrt(x_quad + y_quad);
	}

	/**
	 * calculates the distance between two given points
	 * 
	 * @param a Point
	 * @param b Point
	 * @return
	 */
	public static double distanceEuclidean(Point a, Point b) {
		double x_quad = Math.pow((b.x - a.x), 2);
		double y_quad = Math.pow((b.y - a.y), 2);
		return Math.sqrt(x_quad + y_quad);
	}

	public static Cell pointToCell(Point p) {
		return new Cell((int) p.x, (int) p.y);
	}

	/**
	 * sort minutiae by first: type of minutiae (termination, then bifurcation) second: x
	 * coordinate third: y coordinate
	 * 
	 * @param input
	 * @return
	 */
	public static List<Minutiae> sortByMinutiae(Set<Minutiae> input) {
		List<Minutiae> arrayList = new ArrayList<>();
		arrayList.addAll(input);
		Collections.sort(arrayList, new CompareMinutiae());
		return arrayList;
	}

	/**
	 * the angle of line session given by two input coordinates points if the angle
	 * < 0 -> [0,-PI] -> I. and II. quadrant if the angle > 0 -> [0,PI] -> III. and
	 * IV. quadrant
	 * 
	 * @param cell1
	 * @param cell2
	 * @return
	 */
	public static double orientation(Cell cell1, Cell cell2) {
		double x = cell2.x - cell1.x;
		double y = cell2.y - cell1.y;
		return Math.atan2(y, x);
	}

	/**
	 * convert angle in [-PI,PI] to [0,2*PI] the angle increments in clockwise (-
	 * direction)
	 * 
	 * @param angle
	 * @return always in Radians
	 */
	public static double orientationForFullAngle(double angle) {
		if (angle >= 0)
			return angle;
		else
			return angle + (2 * Math.PI);
	}

	public static boolean angleConcordant(double angle1, double angle2) {
		if (Math.abs(angle1 - angle2) == Math.PI)
			return true;
		else
			return false;
	}

	/**
	 * the angle of line session given by two points
	 * 
	 * @param cell1
	 * @param cell2
	 * @return
	 */
	public double orientationVector(Cell cell1, Cell cell2) {
		double x = cell2.x - cell1.x;
		double y = cell2.y - cell1.y;
		return Math.atan2(y, x);
	}

	/**
	 * exam whether one point is in the given distance from the other point, or not
	 * 
	 * @param p
	 * @param q
	 * @param r
	 * @return
	 */
	public static boolean near(Point p, Point q, double r) {
		if (Math.abs(p.x - q.x) <= r && Math.abs(p.y - q.y) <= r)
			return true;
		else
			return false;
	}

	/**
	 * exam whether one point is in the given distance from the other point, or not
	 * 
	 * @param p
	 * @param q
	 * @param r
	 * @return
	 */
	public static boolean near(Cell p, Cell q, double r) {
		if (Math.abs(p.x - q.x) <= r && Math.abs(p.y - q.y) <= r)
			return true;
		else
			return false;
	}

	public static void writeFile(String whereTo, String what) {
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(whereTo)));
			bw.write(what);
			bw.flush();
			bw.close();
			System.out.println("Txt file with bitmap is done.");
		} catch (IOException e) {
			e.getMessage();
		}
	}

	public static String dump(Mat input) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				double[] data = input.get(i, j);
				sb.append(data[0] + "\t");
			}
			sb.append("\n" + i + ". row:\t");
		}
		return sb.toString();
	}

	/**
	 * calculate the other Point coordinate of a line session in a given angle
	 * 
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

	/**
	 * binarization of image
	 * 
	 * @param input
	 * @return
	 */
	public static Mat binarize(Mat input) {
		Mat binarized = new Mat(input.size(), input.type());
		Imgproc.threshold(input, binarized, 100, 255, Imgproc.THRESH_BINARY);
		return binarized;
	}

	/**
	 * adaptive binarization of image
	 * 
	 * @param input
	 * @return
	 */
	public static Mat binarizeAdaptive(Mat input) {
		// Imgproc.cvtColor(input, input, Imgproc.COLOR_RGB2GRAY);
		Mat binarized = new Mat(input.size(), input.type());
		Imgproc.adaptiveThreshold(input, binarized, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 12);
		return binarized;
	}

	/***********
	 * Static class for comparing Minutiae
	 ************/
	public static class CompareMinutiae implements Comparator<Minutiae> {
		@Override
		public int compare(Minutiae o1, Minutiae o2) {
			int typeCompare = o2.getType().compareTo(o1.getType());
			int min1x = (int) o1.getLocation().x;
			int min1y = (int) o1.getLocation().y;
			int min2x = (int) o2.getLocation().x;
			int min2y = (int) o2.getLocation().y;
			int xCompare = min1x - min2x;
			int yCompare = min1y - min2y;
			if (typeCompare == 0) {
				if (xCompare == 0) {
					return yCompare;
				}
				return xCompare;
			}
			return typeCompare;
		}

	}

	/**normalize image a la Hong 
	 * @param input
	 * @return
	 */
	public static Mat normalize(Mat input) {
		Mat out = input.clone();
		MatOfDouble mean = new MatOfDouble();
		MatOfDouble dev = new MatOfDouble();
		Core.meanStdDev(input, mean, dev);
		double meanImage = mean.get(0, 0)[0];
		double varImage = dev.get(0, 0)[0];
		double pixelValue = 0;
		double expectedMean = meanImage - 15;
		double expectedVar = varImage - 15;
		//System.out.println("mean: " + meanImage + " var: " + varImage);
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				double[] dataIn = input.get(i, j);
				double[] dataOut = new double[input.channels()];
				double temp = Math.sqrt(Math.pow(dataIn[0] - meanImage, 2) * expectedVar / varImage);
				if (dataIn[0] > meanImage)
					pixelValue = expectedMean + temp;
				else
					pixelValue = expectedMean - temp;
				dataOut[0] = pixelValue;
				out.put(i, j, dataOut);
			}
		}
		return out;
	}
}