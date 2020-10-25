package processing;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import processing.BlockOrientation;
import static processing.BlockOrientation.arrayToMat;

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
	private int blockSigma = 7; 
	private int orientSmoothSigma = 7;
	private int fBlockSize = 38;
	private int fWindowSize = 5;
	private int fMinWaveLength = 3;
	private int fMaxWaveLength = 15; 
	private double filterSize = 0.65;

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
	
	/** constructor
	 * executes the steps of image processing
	 * @param input
	 * @throws Exception
	 */
	public ImageProcessing(String input) throws Exception {
		this.source = Imgcodecs.imread(input, 1); // RGB 3 channels (default)

		/***********
		 * 1. step: histogram equalization: source -> equalized
		 ************/
		//resizeAndShow(source, "0 - Ridge Original");
		Imgproc.cvtColor(this.source, this.source, Imgproc.COLOR_RGB2GRAY);
		this.equalizedRidge = equalizeOpenCV(source);
		//resizeAndShow(equalizedRidge, "1 - Equalized ridge");

		/***********
		 * 2. step: normalize: equalized -> normalized normalized matrix values are
		 * float = value / total intensity values (256) global mean: 0, variance: 1
		 ************/
		Mat floatedRidge = new Mat(size, CvType.CV_32FC1);
		this.equalizedRidge.convertTo(floatedRidge, CvType.CV_32FC1);
		Imgcodecs.imwrite("equ.bmp", this.equalizedRidge);
		Mat normalizedRidge = new Mat(size, CvType.CV_32FC1);
		normalizeImage(floatedRidge, normalizedRidge);
//		this.normalizedRidgeShow = new Mat(size, CvType.CV_32SC1);
//		Core.normalize(normalizedRidge, this.normalizedRidgeShow, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
		//resizeAndShow(normalizedRidgeShow, "2 - Normalized Ridge");
		this.normalizedRidgeShow = normalize(this.equalizedRidge);
		//resizeAndShow(normalizedRidgeShow, "normalizált");
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
		//resizeAndShow(segmentedRidge, "4 - Segmented Ridge");
		Mat maskRidgeInitial = orientationRidge.mask(this.segmentedRidge);
		//resizeAndShow(maskRidgeInitial, "4 - Initial mask");
		// for visualization, and the points of contour put in a MatOfPoint variable
		orientationRidge.contourMask(this.segmentedRidge, maskRidgeInitial);
		Mat maskRidge = orientationRidge.mask;
		//resizeAndShow(maskRidge, "4 - Mask ridge");
		Mat normalizedMask = new Mat(size, CvType.CV_32FC1);
		Core.divide(maskRidge, Scalar.all(255), normalizedMask);	
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
//		orientationRidge.computeOrientation(this.segmentedRidge, gradientSigma, blockSigma,
//				orientSmoothSigma);
		ridgeOrientation = orientationRidge.orientation;
		Core.normalize(ridgeOrientation, this.ridgeOrientationShow, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
		//resizeAndShow(ridgeOrientationShow, "5 - Oriented ridge");
		this.orientation = new Mat(source.size(), source.type());
		orientation = orientationRidge.avgBlockForDraw(segmentedRidge);
		orientation = orientationRidge.drawGrid(orientation);
		//resizeAndShow(orientation, "5 - Oriented ridge");
		/***********
		 * 6. step: ridge frequency calculation
		 ************/
		Mat ridgeFrequencyRidge = new Mat(size, CvType.CV_32FC1);
		double medianFrequencyRidge = orientationRidge.ridgeFrequency(this.segmentedRidge, maskRidge, ridgeOrientation,
				ridgeFrequencyRidge, fBlockSize, fWindowSize, fMinWaveLength, fMaxWaveLength);
//		Mat filteredRidge1 = orientationRidge.gaborFilter1(segmentedRidge, ridgeOrientation);
//		Core.normalize(filteredRidge1, filteredRidge1, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
//		resizeAndShow(filteredRidge1, "Gabor");
		/***********
		 * 7. step: Gabor filtering
		 ************/
		Mat filteredRidge = new Mat(size, CvType.CV_32FC1);
		this.segmentedRidge.convertTo(this.segmentedRidge, CvType.CV_32FC1);
		orientationRidge.ridgeFilter(this.segmentedRidge, ridgeOrientation, ridgeFrequencyRidge, filteredRidge, filterSize,
				filterSize, medianFrequencyRidge);
		this.filteredRidgeShow = new Mat(size, CvType.CV_32FC1);
		Core.normalize(filteredRidge, this.filteredRidgeShow, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
		//resizeAndShow(filteredRidgeShow, "7 - Filtered ridge (Gabor)");
		
		/***********
		 * 8. step: adaptive binarization
		 ************/
		filteredRidge.convertTo(filteredRidge, CvType.CV_8UC3);
		this.binarizedRidge = binarizeAdaptive(filteredRidge);
		Mat binarizedValley = new Mat(source.size(), source.type());
		Core.bitwise_not(this.binarizedRidge, binarizedValley);
		//resizeAndShow(binarizedValley, "8 - Binarized valley");

		// nem adaptív
		//Imgproc.threshold(filteredRidge, binarizedRidge, 0, 255, Imgproc.THRESH_BINARY);
		//resizeAndShow(binarizedRidge, "8 - Binarized ridge");

		/***********
		 * 9. step: thinning
		 * 1, thin the binarized ridge image
		 * 2, thin the binarized valley image (= inverse of the binarized ridge image)
		 ************/
		Mat thinnedRidge = thinning(this.binarizedRidge);
		// resizeAndShow(thinnedRidge, "6 - Ridge Thinned");
		Mat invertMask = Extraction.contourNonZeroPoints(thinnedRidge);
		Mat finalBinarizedValley = Extraction.maskInverted(binarizedValley, invertMask);
		//resizeAndShow(finalBinarizedValley, "Segmented bin invert");

		Mat thinnedValley = thinning(finalBinarizedValley);
		// resizeAndShow(thinnedValley, "6 - Valley Thinned");

		/***********
		 * 10. step: small lakes, dots elimination from thinned image
		 ************/
		this.openedRidge = opening(thinnedRidge);
		//resizeAndShow(openedRidge, "7 - Ridge w/o small lakes");
		// avgRidgeDistance = stepsRidge.avgRidgeDistance(openedRidge);
		Mat openedValley = opening(thinnedValley);
		//resizeAndShow(openedValley, "7 - Valley w/o small lakes");

		/***********
		 * 11, step: instanciate the RidgesMap class for mark all possible minutiae
		 * - keresztszám módszerrel kiszámolja a lehetséges minucia pontokat
		 * - betölti a set adattagba 
		 * - kiszámítja minden minuciához a hozzá tartozó redõvonalak adatait:
		 * (a létrehozott vékonyított kép LogicMatrix (0 - white, 1 - black) példányából) 
		 * 		a, ha végzõdés (terminations Set-be a pixelpontokat, angleTerm - a szög
		 * 		b, ha elágazás (bifurcations Set-be a pixelpontoakt, 
		 * 			minden ág kezdõ és végpontját, azok szögeit
		 * 	 
		 1, terminations (ha minucia egy végzõdés, ebbe a Set-be menti a redõ
		 pixelpontokat)
		 2, bifurcations (ha minucia egy elágazás, ebbe a Set-be menti a redõ
		 pixelpontokat)
		 3, neighbours (minucia közvetlen szomszédok - általában 3 darab)
		 4, farpoints (ha a minucia elágazás, akkor a minuciaponttól legtávolabbra esõ
		 pont a bifurcations Set-be elmentett pixelpontok közül)
		 5, pointpairs (ha a minucia elágazás, képzi az összetartozó
		 neighbours-farpoints párokat)
		 6, angles (ha a minucia elágazás, elmenti egy 3 darabos Double Set-be a
		 pointpairs által megadott szakaszok irányszögét)
		 kiszámolja a minuciától legtávolabbi elágazási pontokat,
		 és beletölti a Ridges fairpoints adattagjának Set-jébe
		 nem a farPoints és a minuciapont, hanem farpoints és közeli szomszédok között
		 számolja az irányt - és betölti a Ridges pointpairs adattagjába
		 minden végzõdés minuciához (Cell) kiszámolja annak irányszögét
		 minden elágazáshoz (Cell mutatja a helyet) tartozik a 3 darab redõvonal szöge
		 * - a végsõ eredményt Minutiae -> Ridges map - be töltjük
		 ************/
		// params: vékonyított kép
		RidgesMap ridgesMapRidge = new RidgesMap(this.openedRidge);
		RidgesMap ridgesMapValley = new RidgesMap(openedValley);
		// beállítjuk a végzõdések szögeit ridge és valley képen egyaránt
		ridgesMapRidge.setOrientationForTermination();
		ridgesMapValley.setOrientationForTermination();
		// ridge képen az elágazások szögeit is kiszámítja
		// (a valley kép végzõdés szögei alapján)
		ridgesMapRidge.calculateBifurcation(ridgesMapValley.getMinutiaeMapFinal());
		
		// a felderített végzõdések és elágazásokhoz tartozó redõágak
		// a redõképen
		Mat terminationsRidge = ridgesMapRidge.terminationMatrix();
		Mat bifurcationsRidge = ridgesMapRidge.bifurcationMatrix();
//		resizeAndShow(terminationsRidge, "9 - Ridge Terminations");
//		resizeAndShow(bifurcationsRidge, "9 - Ridge Bifurcations");
		// a barázdaképen
		Mat terminationsValley = ridgesMapValley.terminationMatrix();
		Mat bifurcationsValley = ridgesMapValley.bifurcationMatrix();
//		resizeAndShow(terminationsValley, "9 - Valley Terminations");
//		resizeAndShow(bifurcationsValley, "9 - Valley Bifurcations");

		/*********** 12. lépés: a redõkép és duálja (barázdakép) megadása
		 * az álminucia eltávolító osztály számára -> példányosítás
		 * params: 1. redõkép, 2. a hozzá tartozó barázdakép ************/
		Extraction extractionRidge = new Extraction(ridgesMapRidge, ridgesMapValley);
		// összes lehetséges minucia irányszögének megjelenítése
		this.withAnglesRidge = extractionRidge.drawMinutiaeDirection();
		//resizeAndShow(withAnglesRidge, "10 - Ridge w/t angles");
		// minucia pontok megadása a redõképen megjelenítve
		Mat allInRidge = extractionRidge.allMinutia();
		//resizeAndShow(allInRidge, "All minutiae in ridge");
			
		/*********** 16. lépés: perem meghatározás ************/
		// a maszkot most a vékonyított képbõl képezzük
		Mat nonBlockMask = Extraction.contourNonZeroPoints(this.openedRidge);
		//resizeAndShow(nonBlockMask, "12 - Ridge Mask from Thinned");
		Mat borderRidgeMask = Extraction.scaleContour(Extraction.roiContour, 0.85, nonBlockMask);
		//resizeAndShow(borderRidgeMask, "12 - Ridge border mask");
		
		/*********** 13. step: false minutiae elimination ************/
		// 1. lépés: elszigetelt pontok eltávolítása
		extractionRidge.eliminationDot();
		// 2. lépés: apró tavak, szigetek eltávolítása
		extractionRidge.eliminationPore();
		// 3. lépés: elágazásból végzõdés lehet, ha a duál barázdaképen ezt az elágazást
		//           éppen két olyan végzõdés fogja közre, mely szakadás miatt nem valós
		extractionRidge.changeType();
		extractionRidge.changeType2();
		// 4. lépés: túl közeli végzõdések törlése
		extractionRidge.eliminationTooNearTerm();
		// 5. lépés: redõszakadások eltávolítása 
		extractionRidge.eliminationGap();
		// 6. lépés: nyúlványok eltávolítása
		extractionRidge.eliminationSpur();
		// 7. lépés: H-pontok eltávolítása
		extractionRidge.eliminationHpoint();

		/*********** 15. lépés: perem minutiae eltávolítás ************/
		// 8. lépés: peremen lévõ végzõdések eltávolítása
		extractionRidge.borderMask(borderRidgeMask);
		this.minutiaeExtractedRidge = extractionRidge.drawMinutiaeDirection();
		//resizeAndShow(minutiaeExtractedRidge, "13 - Ridge w/o border minutiae");

		// a Controller osztály számára megadjuk a minucialistát a minucia set azonosítóhoz
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


	/**getters
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

	public static void resizeAndShow(Mat input, String title) throws IOException {
		Mat resized = new Mat();
		Imgproc.resize(input, resized, sizeShow);
		imshow(resized, title);
	}

	// általános segédfüggvény: frame-ben megjeleníti a megadott
	// Mat-objectumot
	public static void imshow(Mat matrix, String name) throws IOException {
		MatOfByte matOfByte = new MatOfByte(); 
		Imgcodecs.imencode(".bmp", matrix, matOfByte);
		byte[] byteArray = matOfByte.toArray();
		InputStream in = new ByteArrayInputStream(byteArray);
		BufferedImage bufImage = ImageIO.read(in); 
		JFrame frame = new JFrame(); 
		frame.getContentPane().add(new JLabel(new ImageIcon(bufImage)));
		frame.pack();
		frame.setTitle(name);
		frame.setSize(new Dimension(800, 1000));
		frame.setLocation(500, 50);
		frame.setVisible(true);
	}

	/**equalizes a Mat object
	 * @param input
	 * @return
	 */
	public Mat equalizeOpenCV(Mat input) {
		Mat equalized = new Mat(size, CvType.CV_32FC1);
		Imgproc.equalizeHist(input, equalized);
		return equalized;
	}

	/***********
	 * 2. step: normalization: equalized -> 
	 * normalized values between 0 and 1 
	 * mean zero, unit std dev.
	 ************/

	/**normalizes a Mat object
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

	/**thinning operation
	 * @param input
	 * @return
	 * @throws IOException
	 */
	public Mat thinning(Mat input) throws IOException {
		Mat output = new Mat(input.size(), input.type());
		double [][] pixels = new double[height][width];
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				double[] data = input.get(i, j);
				pixels[i][j] = data[0];
			}
		}
		ThinnedImage thinnedImage = new ThinnedImage(pixels);
		thinnedImage.thinImage();
		output = arrayToMat(pixels);
		return output;
	}
	
	/**morphology operation for elimination of small islands, 
	 * ridge lines (like noise)
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
		resizeAndShow(input, "Original");
		resizeAndShow(dx, "dx");
		resizeAndShow(dy, "dy");
	}

	/***********
	 * Functions auxiliary
	 ************/

	/** unifies the two type of thinned image:
	 * ridge image - valley image in one thinned image
	 * for visualization
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
				if (data[0] == 0 && ridgeData[0] == 254) { // ahol metszi a redÄ‚â€¦Ã¢â‚¬ï¿½ a barÄ‚ï¿½Ã‚Ë‡zdÄ‚ï¿½Ã‚Ë‡t
					outData[0] = 254; // legyen kÄ‚ï¿½Ã‚Â©k
					outData[1] = 0;
					outData[2] = 0;
				} else if (data[0] == 0 && ridgeData[0] == 255) { // ahol csak barÄ‚ï¿½Ã‚Ë‡zda van
					outData[0] = 255; // legyen lila
					outData[1] = 0;
					outData[2] = 255;
				} else if (data[0] == 255 && ridgeData[0] == 254) { // ahol csak redÄ‚â€¦Ã¢â‚¬ï¿½ van
					outData[0] = 254;
					outData[1] = 0;
					outData[2] = 0;
				} else if (data[0] == 255 && ridgeData[0] == 255) { // hÄ‚ï¿½Ã‚Ë‡ttÄ‚ï¿½Ã‚Â©r marad is
					outData[0] = 255;
					outData[1] = 255;
					outData[2] = 255;
				} else { // mit hagytam ki???
					outData[0] = 0;
					outData[1] = 0;
					outData[2] = 0;
				}
				out.put(i, j, outData);
			}
		}
		return out;
	}

	/** calculates the distance between two given points
	 * @param a 
	 * @param b
	 * @return
	 */
	public static double distanceEuclidean(Cell a, Cell b) {
		double x_quad = Math.pow((b.x - a.x), 2);
		double y_quad = Math.pow((b.y - a.y), 2);
		return Math.sqrt(x_quad + y_quad);
	}

	/** calculates the distance between two given points
	 * @param a
	 * @param b
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

	/**sort minutiae by
	 * first: type of minutiae (ending, then bifurcation)
	 * second: x coordinate 
	 * third: y coordinate
	 * @param input
	 * @return
	 */
	public static List<Minutiae> sortByMinutiae(Set<Minutiae> input) {
		List<Minutiae> arrayList = new ArrayList<>();
		arrayList.addAll(input);
		Collections.sort(arrayList, new CompareMinutiae());
		return arrayList;
	}

	/** the angle of line session given by two input coordinates points
	 * if the angle < 0 -> [0,-PI] -> I. and II. quadrant
	 * if the angle > 0 -> [0,PI] -> III. and IV. quadrant
	 * @param cell1
	 * @param cell2
	 * @return
	 */
	public static double orientation(Cell cell1, Cell cell2) {
		double x = cell2.x - cell1.x;
		double y = cell2.y - cell1.y;
		return Math.atan2(y, x);
	}
	
	/**convert angle in [-PI,PI] to [0,2*PI]
	 * the angle increments in clockwise (- direction)
	 * @param angle
	 * @return always in Radians
	 */
	public static double orientationForFullAngle(double angle) {
		if (angle >= 0)
			return angle;
		else 
			return angle + (2*Math.PI);
	}

	public static boolean angleConcordant(double angle1, double angle2) {
		if (Math.abs(angle1 - angle2) == Math.PI)
			return true;
		else
			return false;
	}


	/**the angle of line session given by two points
	 * @param cell1
	 * @param cell2
	 * @return
	 */
	public double orientationVector(Cell cell1, Cell cell2) {
		double x = cell2.x - cell1.x;
		double y = cell2.y - cell1.y;
		return Math.atan2(y, x);
	}

	/**exam whether one point is in the given distance from the other point, or not
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

	/**exam whether one point is in the given distance from the other point, or not
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
		int count = 0;
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				double[] data = input.get(i, j);
				sb.append(data[0] + "\t");
				count++;
			}
			sb.append("\n" + i + ". row:\t");
		}
		return sb.toString();
	}

	/**calculate the other Point coordinate of a line session in a given angle
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
	
	/**binarizes the image
	 * @param input
	 * @return
	 */
	public static Mat binarize(Mat input) {
		Mat binarized = new Mat(input.size(), input.type());
		Imgproc.threshold(input, binarized, 100, 255, Imgproc.THRESH_BINARY);
		return binarized;
	}

	/**adaptive binarization of image
	 * @param input
	 * @return
	 */
	public static Mat binarizeAdaptive(Mat input) {
		//Imgproc.cvtColor(input, input, Imgproc.COLOR_RGB2GRAY);
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
		System.out.println("mean: " + meanImage + " var: " + varImage);
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