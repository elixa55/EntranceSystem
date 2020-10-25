package processing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import static processing.Extraction.roiContour;
import static processing.ImageProcessing.*;
import static processing.BlockOrientation.angle;

public class Extraction {

	public static Scalar red = new Scalar(0, 0, 255);
	public static Scalar purple = new Scalar(255, 0, 255);
	public static Scalar blue = new Scalar(255, 0, 0);
	public static List<MatOfPoint> roiContour = null;

	public RidgesMap ridge;
	public RidgesMap valley;

	// contstructor
	public Extraction(RidgesMap ridge, RidgesMap valley) {
		this.ridge = ridge;
		this.valley = valley;
	}

	/********************
	 * False minutiae elimination steps******* 1.redõszakadások kiiktatása
	 * 2.nyúlványok eltávolítása 3. H-pontok eltávolítása 4. túl közeli minuciák
	 * eltávolítása (végzõdés, elágazás is) 5. perem minuciák eltávolítása
	 ******************************************************************************/

	/************************* I. FALSE TERMINATION ELIMINATION *******************/

	// 1. lépés: egyedülálló pontok eltávolítása
	public void eliminationDot() throws IOException {
		Mat out = this.ridge.getMatrix().clone();
		Set<Minutiae> section = new HashSet<>();
		for (Map.Entry<Minutiae, Ridges> term : ridge.getMinutiaeMapFinal().entrySet()) {
			if (term.getKey().getType().equals("ending") && term.getValue().terminations.size() <= 2) {
				section.add(term.getKey());
				Imgproc.circle(out, term.getKey().getLocation(), 3, red);
			}
		}
		for (Minutiae minutia : section) {
			ridge.removeRecord(minutia);
		}
		//resizeAndShow(out, "túl kicsi term");
	}

	// 2. lépés: apró tavak, szigetek eltávolítása
	public void eliminationPore() throws IOException {
		Mat out = this.ridge.getMatrix().clone();
		Set<Minutiae> section = new HashSet<>();
		for (Map.Entry<Minutiae, Ridges> termExt : valley.getMinutiaeMapFinal().entrySet()) {
			for (Map.Entry<Minutiae, Ridges> termInt : valley.getMinutiaeMapFinal().entrySet()) {
				if (termExt.getKey().getType().equals("ending") && termInt.getKey().getType().equals("ending")
						&& !termExt.getKey().getLocation().equals(termInt.getKey().getLocation())
						&& distanceEuclidean(termExt.getKey().getLocation(), termInt.getKey().getLocation()) < 25) {
					for (Map.Entry<Minutiae, Ridges> bifExt : ridge.getMinutiaeMapFinal().entrySet()) {
						for (Map.Entry<Minutiae, Ridges> bifInt : ridge.getMinutiaeMapFinal().entrySet()) {
							if (bifExt.getKey().getType().equals("bifurcation")
									&& bifInt.getKey().getType().equals("bifurcation")
									&& !bifExt.getKey().getLocation().equals(bifInt.getKey().getLocation())
									&& distanceEuclidean(bifExt.getKey().getLocation(),
											bifInt.getKey().getLocation()) < 35) {
								if ((distanceEuclidean(bifExt.getKey().getLocation(),
										termExt.getKey().getLocation()) < 5
										|| distanceEuclidean(bifExt.getKey().getLocation(),
												termInt.getKey().getLocation()) < 5)
										&& (distanceEuclidean(bifInt.getKey().getLocation(),
												termExt.getKey().getLocation()) < 5
												|| distanceEuclidean(bifInt.getKey().getLocation(),
														termInt.getKey().getLocation()) < 5)) {
									boolean containTerm = false;
									boolean containBif = false;
									double distanceBif = distanceEuclidean(bifExt.getKey().getLocation(),
											bifInt.getKey().getLocation());
									double distanceTerm = distanceEuclidean(termExt.getKey().getLocation(),
											termInt.getKey().getLocation());
									for (Cell c : termExt.getValue().terminations) {
										if (termInt.getValue().terminations.contains(c))
											containTerm = true;
									}
									for (Cell c : bifExt.getValue().bifurcations) {
										if (bifInt.getValue().bifurcations.contains(c))
											containBif = true;
									}
									if (containTerm && containBif && distanceBif > distanceTerm) {
										section.add(bifExt.getKey());
										section.add(bifInt.getKey());
										Imgproc.circle(out, bifExt.getKey().getLocation(), 3, red);
										Imgproc.circle(out, bifInt.getKey().getLocation(), 3, red);
									}
								}
							}
						}
					}
				}
			}
		}
		for (Minutiae minutia : section) {
			ridge.removeRecord(minutia);
		}
		//resizeAndShow(out, "redõsziget");
	}

	public double convertHalfAngle(double angle) {
		if (angle >= Math.PI) {
			angle -= Math.PI;
		}
		return angle;
	}

	public boolean setToContour(Set<Cell> set1, Set<Cell> set2, Point p1, Point p2) {
		ArrayList<Point> list = new ArrayList<>();
		boolean result = false;
		for (Cell c : set1)
			list.add(c.toPoint());
		for (Cell c : set2)
			list.add(c.toPoint());
		MatOfPoint matofpoint = new MatOfPoint();
		matofpoint.fromList(list);
		List<MatOfPoint> finalPoints = new ArrayList<MatOfPoint>();
		finalPoints.add(matofpoint);
		List<MatOfPoint2f> newContours = new ArrayList<>();
		for (MatOfPoint point : finalPoints) {
			MatOfPoint2f newPoint = new MatOfPoint2f(point.toArray());
			newContours.add(newPoint);
		}
		double result1 = Imgproc.pointPolygonTest(newContours.get(0), p1, true);
		double result2 = Imgproc.pointPolygonTest(newContours.get(0), p2, true);
		if (result1 < 0 && result2 < 0)
			result = true;
		return result;
	}

	// 3. lépés: redõképen lévõ elágazásból -> végzõdés lehet, ha
	// a barázdaképen az iménti elágazást közrefogja két végzõdés, melyhez elágazás
	// van közel,
	// így a két végzõdést csak az elágazás egyik ágában lévõ redõszakadás okozza
	public void changeType() throws IOException {
		Mat out = this.ridge.getMatrix().clone();
		for (Map.Entry<Minutiae, Ridges> bifValley : valley.getMinutiaeMapFinal().entrySet()) {
			for (Map.Entry<Minutiae, Ridges> termValley : valley.getMinutiaeMapFinal().entrySet()) {
				if (bifValley.getKey().getType().equals("bifurcation") && termValley.getKey().getType().equals("ending")
						&& distanceEuclidean(bifValley.getKey().getLocation(), termValley.getKey().getLocation()) < 6) {
					for (Map.Entry<Minutiae, Ridges> termIntValley : valley.getMinutiaeMapFinal().entrySet()) {
						double sessionAngle = orientation(pointToCell(termValley.getKey().getLocation()),
								pointToCell(termIntValley.getKey().getLocation()));
						sessionAngle = orientationForFullAngle(sessionAngle);
						double diffAngle = Math.abs(termIntValley.getValue().angleTerm - sessionAngle);
						if (!termValley.getKey().equals(termIntValley.getKey())
								&& termIntValley.getKey().getType().equals("ending")
								&& distanceEuclidean(termValley.getKey().getLocation(),
										termIntValley.getKey().getLocation()) < 12
								&& diffAngle <= Math.toRadians(15)) {
							for (Map.Entry<Minutiae, Ridges> bifRidge : ridge.getMinutiaeMapFinal().entrySet()) {
								if (bifRidge.getKey().getType().equals("bifurcation")) {
									double dist = minDistDotLine(termValley.getKey().getLocation(),
											termIntValley.getKey().getLocation(), bifRidge.getKey().getLocation());
									if (dist <= 0.7) {
										// típust vált: bif -> term, átállítjuk az adatait is, ne legyen nullpointer ex.
										bifRidge.getKey().setType("ending");
										bifRidge.getValue().setAngleTerm(bifRidge.getKey().getOrientation());
										bifRidge.getValue().setTerminations(bifRidge.getValue().getBifurcations());
										Imgproc.circle(out, bifRidge.getKey().getLocation(), 4, purple);
									}
								}
							}
						}
					}
				}
			}
		}
		//resizeAndShow(out, "Típust váltó 1. (elágazás->végzõdés)");
	}

	public void changeType2() throws IOException {
		Mat out = this.ridge.getMatrix().clone();
		for (Map.Entry<Minutiae, Ridges> bifRidge : ridge.getMinutiaeMapFinal().entrySet()) {
			for (Map.Entry<Minutiae, Ridges> bifValley : valley.getMinutiaeMapFinal().entrySet()) {
				if (bifRidge.getKey().getType().equals("bifurcation") && bifValley.getKey().getType().equals("bifurcation")
						&& distanceEuclidean(bifRidge.getKey().getLocation(), bifValley.getKey().getLocation()) <= 4) {
					double angle = calculateBifurcationAngle(bifValley.getValue());
					bifRidge.getKey().setType("ending");
					bifRidge.getKey().setOrientation(angle);
					bifRidge.getValue().setAngleTerm(angle);
					bifRidge.getValue().setTerminations(bifRidge.getValue().getBifurcations());
					Imgproc.circle(out, bifRidge.getKey().getLocation(), 4, purple);
				}
			}
		}
		//resizeAndShow(out, "Típust váltó 2. (elágazás->végzõdés)");
	}

	public double calculateBifurcationAngle(Ridges ridge) {
				Object[] array = ridge.anglesBif.toArray();
				double alpha = (double) array[0];
				double beta = (double) array[1];
				double gamma = (double) array[2];
				double distGammaAlpha = (2 * Math.PI - gamma) + alpha;
				double distBetaAlpha = beta - alpha;
				double distGammaBeta = gamma - beta;
				double minDistance = minElement(distGammaAlpha, distBetaAlpha, distGammaBeta);
				double half = minDistance / 2;
				double angle = 0;
				if (minDistance == distGammaAlpha) {
					angle = alpha - half;
					if (angle < 0) {
						angle = 2 * Math.PI + angle;
					}
				} else if (minDistance == distBetaAlpha)
					angle = alpha + half;
				else if (minDistance == distGammaBeta)
					angle = beta + half;
				return angle;
	}
	
	public double minElement(double a, double b, double c) {
		return Math.min(a, Math.min(b, c));
	}

	// 4. lépés: túl közel lévõ termek
	public void eliminationTooNearTerm() throws IOException {
		Mat out = this.ridge.getMatrix().clone();
		Set<Minutiae> section = new HashSet<>();
		for (Minutiae termExt : ridge.getMinutiaeMapFinal().keySet()) {
			for (Minutiae termInt : ridge.getMinutiaeMapFinal().keySet()) {
				if (termExt.getType().equals("ending") && termInt.getType().equals("ending")
						&& !termExt.getLocation().equals(termInt.getLocation())
						&& distanceEuclidean(termExt.getLocation(), termInt.getLocation()) <= 5) {
					section.add(termExt);
					section.add(termInt);
					Imgproc.circle(out, termExt.getLocation(), 3, red);
					Imgproc.circle(out, termInt.getLocation(), 3, red);
				}

			}
		}
		for (Minutiae m : section) {
			ridge.removeRecord(m);
		}
		//resizeAndShow(out, "Túl közeli termek");
	}

	// 5. lépés: redõszakadások eltávolítása
	public void eliminationGap() throws IOException {
		Mat out = ridge.getMatrix().clone();
		Set<Minutiae> section = new HashSet<>();
		// minutiaeListRidge.removeAll(hpointsValley);????
		double low = Math.toRadians(0);
		double high = Math.toRadians(15);
		for (Map.Entry<Minutiae, Ridges> minExt : this.ridge.getMinutiaeMapFinal().entrySet()) {
			for (Map.Entry<Minutiae, Ridges> minInt : this.ridge.getMinutiaeMapFinal().entrySet()) {
				if (minExt.getKey().getType().equals("ending") && minInt.getKey().getType().equals("ending")
						&& distanceEuclidean(minExt.getKey().getLocation(), minInt.getKey().getLocation()) < 12
						&& distanceEuclidean(minExt.getKey().getLocation(), minInt.getKey().getLocation()) != 0) {
					double sessionAngle = orientation(pointToCell(minExt.getKey().getLocation()),
							pointToCell(minInt.getKey().getLocation()));
					sessionAngle = angleTransform(sessionAngle);
					double minExtAngle = angleTransform(minExt.getKey().getOrientation());
					double minIntAngle = angleTransform(minInt.getKey().getOrientation());
					boolean contain = false;
					for (Cell c : minExt.getValue().terminations) {
						if (minInt.getValue().terminations.contains(c))
							contain = true;
					}
					if (((Math.abs(minExtAngle - sessionAngle) < high) || (Math.abs(minIntAngle - sessionAngle) < high))
							&& Math.abs(minExtAngle - minIntAngle) < high || contain) {
						section.add(minExt.getKey());
						section.add(minInt.getKey());
						Imgproc.circle(out, minExt.getKey().getLocation(), 4, new Scalar(0, 0, 255));
						Imgproc.circle(out, minInt.getKey().getLocation(), 4, new Scalar(0, 0, 255));
					}
				}
			}
		}
		for (Minutiae m : section) {
			ridge.removeRecord(m);
		}
		//resizeAndShow(out, "Redõ szakadások");
	}

	public double angleTransform(double angle) {
		if (angle >= Math.PI) {
			angle -= Math.PI;
		}
		return angle;
	}

	// 6. lépés: valódi nyúlványok eltávolítása (elágazáshoz túl közeli végzõdés)
	public void eliminationSpur() throws IOException {
		Mat out = ridge.getMatrix().clone();
		Set<Minutiae> section = new HashSet<>();
		for (Map.Entry<Minutiae, Ridges> bif : ridge.getMinutiaeMapFinal().entrySet()) {
			for (Map.Entry<Minutiae, Ridges> term : ridge.getMinutiaeMapFinal().entrySet()) {
				if (bif.getKey().getType().equals("bifurcation") && term.getKey().getType().equals("ending")
						&& distanceEuclidean(bif.getKey().getLocation(), term.getKey().getLocation()) < 20) {
					// term minden redõvonalpixelét tartalmazza bif pixelei
					Set<Cell> cellsOfBif = bif.getValue().bifurcations;
					int termCount = 0;
					for (Cell c : term.getValue().terminations) {
						if (cellsOfBif.contains(c)) {
							termCount++;
						}
					}
					if (termCount > 3) {
						section.add(bif.getKey());
						section.add(term.getKey());
						Imgproc.circle(out, bif.getKey().getLocation(), 3, red);
						Imgproc.circle(out, term.getKey().getLocation(), 3, red);
					}
				}
			}
		}
		for (Minutiae m : section) {
			ridge.removeRecord(m);
		}
		//resizeAndShow(out, "spur elimination");
	}

	// 7. lépés: H-pontok felderítése és eltávolítása
	public void eliminationHpoint() throws IOException {
		Mat out = ridge.getMatrix().clone();
		Set<Minutiae> section = new HashSet<>();
		for (Map.Entry<Minutiae, Ridges> bifMinExt : ridge.getMinutiaeMapFinal().entrySet()) {
			for (Map.Entry<Minutiae, Ridges> bifMinInt : ridge.getMinutiaeMapFinal().entrySet()) {
				if (bifMinExt.getKey().getType().equals("bifurcation")
						&& bifMinInt.getKey().getType().equals("bifurcation")
						&& distanceEuclidean(bifMinExt.getKey().getLocation(), bifMinInt.getKey().getLocation()) < 20
						&& distanceEuclidean(bifMinExt.getKey().getLocation(), bifMinInt.getKey().getLocation()) != 0) {
					for (Map.Entry<Minutiae, Ridges> termMin1 : valley.getMinutiaeMapFinal().entrySet()) {
						for (Map.Entry<Minutiae, Ridges> termMin2 : valley.getMinutiaeMapFinal().entrySet()) {
							if (termMin1.getKey().getType().equals("ending")
									&& termMin2.getKey().getType().equals("ending")
									&& distanceEuclidean(termMin1.getKey().getLocation(),
											bifMinExt.getKey().getLocation()) <= 8
									&& distanceEuclidean(termMin2.getKey().getLocation(),
											bifMinExt.getKey().getLocation()) <= 8
									&& !termMin1.getKey().getLocation().equals(termMin2.getKey().getLocation())) {
								if (sessionsIntersect(bifMinExt.getKey().getLocation(),
										bifMinInt.getKey().getLocation(), termMin1.getKey().getLocation(),
										termMin2.getKey().getLocation())) {
									Point midPointRidge = midPoint(bifMinExt.getKey().getLocation(),
											bifMinInt.getKey().getLocation());
									Point midPointValley = midPoint(termMin1.getKey().getLocation(),
											termMin2.getKey().getLocation());
									double distanceMidPoint = distanceEuclidean(midPointRidge, midPointValley);
									if (distanceMidPoint < 6) {
										double angleIntersect = Math.toDegrees(angleOfIntersectPoint(
												bifMinExt.getKey().getLocation(), bifMinInt.getKey().getLocation(),
												termMin1.getKey().getLocation(), termMin2.getKey().getLocation())); // radiánba
										// 0-180-ig
										if (angleIntersect >= 25 && angleIntersect <= 155) {
											section.add(bifMinExt.getKey());
											section.add(bifMinInt.getKey());
											section.add(termMin1.getKey());
											section.add(termMin2.getKey());
											Imgproc.circle(out, bifMinExt.getKey().getLocation(), 3, red);
											Imgproc.circle(out, bifMinInt.getKey().getLocation(), 3, red);
											Imgproc.circle(out, termMin1.getKey().getLocation(), 3, blue);
											Imgproc.circle(out, termMin2.getKey().getLocation(), 3, blue);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		for (Minutiae m : section) {
			ridge.removeRecord(m);
		}
		//resizeAndShow(out, "H-pontok eltávolítása");
	}

	// 8. lépés: perem minuciák eltávolítása (term és bif is)
	public void borderMask(Mat mask) throws IOException {
		Mat out = ridge.getMatrix().clone();
		Set<Minutiae> section = new HashSet<>();
		for (Minutiae minutia : this.ridge.getMinutiaeMapFinal().keySet()) {
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					double[] datamask = mask.get(i, j);
					if (datamask[0] == 255 && minutia.getLocation().x == j && minutia.getLocation().y == i) {
						section.add(minutia);
						Imgproc.circle(out, minutia.getLocation(), 3, red);
					}
				}
			}
		}
		for (Minutiae m : section) {
			ridge.removeRecord(m);
		}
		//resizeAndShow(out, "Perem menti minuciák eltávolítása");
	}

	/**to static variable allContour calculates new contour that 
	 * the convex border of non zero points of image 
	 * @param input: thinned ridge image
	 * @return: Mat object that would be the mask for segmentation of valley image
	 */
	public static Mat contourNonZeroPoints(Mat input) {
		int threshold = 200;
		Mat cannyOutput = new Mat(input.size(), input.type());
		Mat srcGray = new Mat(input.size(), input.type());
		Mat binary = new Mat(input.size(), input.type());
		Mat drawing = Mat.zeros(input.size(), CvType.CV_8UC3);
		Mat out = Mat.zeros(binary.size(), binary.type());
		Imgproc.cvtColor(input, srcGray, Imgproc.COLOR_BGR2GRAY);
		Imgproc.blur(srcGray, srcGray, new Size(3, 3));
		Imgproc.Canny(srcGray, cannyOutput, threshold, threshold * 2);
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		binary = ImageProcessing.binarize(cannyOutput);
		Core.findNonZero(binary, out);
		MatOfPoint matofpoint = new MatOfPoint(out);
		roiContour = new ArrayList<>();
		contours.add(matofpoint);
		for (MatOfPoint contour : contours) {
			MatOfInt hull = new MatOfInt();
			Imgproc.convexHull(contour, hull);
			Point[] contourArray = contour.toArray();
			Point[] hullPoints = new Point[hull.rows()];
			List<Integer> hullContourIdxList = hull.toList();
			for (int i = 0; i < hullContourIdxList.size(); i++) {
				hullPoints[i] = contourArray[hullContourIdxList.get(i)];
			}
			roiContour.add(new MatOfPoint(hullPoints));
		}
		Imgproc.fillConvexPoly(drawing, roiContour.get(0), new Scalar(255, 255, 255));
		return drawing;
	}

	/**scales the mask size to mark the border area of ROI
	 * @param mask
	 * @param scale
	 * @param input
	 * @return
	 */
	public static Mat scaleContour(List<MatOfPoint> mask, double scale, Mat input) {
		Mat out = input.clone();
		MatOfPoint cnt = mask.get(0);
		Moments M = Imgproc.moments(cnt);
		int cx = (int) (M.get_m10() / M.get_m00());
		int cy = (int) (M.get_m01() / M.get_m00());
		Point[] array = cnt.toArray();
		Point[] scaled_array = new Point[array.length];
		Point center = new Point(cx, cy);
		for (int i = 0; i < array.length; i++) {
			Point norm = new Point(array[i].x - center.x, array[i].y - center.y);
			Point scaled = new Point(norm.x * scale, norm.y * scale);
			scaled.x += cx;
			scaled.y += cy;
			scaled_array[i] = scaled;
		}
		MatOfPoint outList = new MatOfPoint(scaled_array);
		Imgproc.fillConvexPoly(out, outList, Scalar.all(0));
		return out;
	}
	
	/**make inverse of mask
	 * black -> background, white -> ROI
	 * @param input
	 * @param mask
	 * @return
	 */
	public static Mat maskInverted(Mat input, Mat mask) {
		Mat out = input.clone();
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				double[] data = out.get(i, j);
				double[] dataMask = mask.get(i, j);
				if (dataMask[0] == 0) {
					data[0] = 255;
				}
				out.put(i, j, data);
			}
		}
		return out;
	}

	

	public double minDistDotLine(Point a, Point b, Point d) {
		Point ab = new Point();
		ab.x = b.x - a.x;
		ab.y = b.y - a.y;
		Point bd = new Point();
		bd.x = d.x - b.x;
		bd.y = d.y - b.y;
		Point ad = new Point();
		ad.x = d.x - a.x;
		ad.y = d.y - a.y;
		double ab_bd, ab_ad;
		ab_bd = (ab.x * bd.x + ab.y * bd.y);
		ab_ad = (ab.x * ad.x + ab.y * ad.y);
		double reqAns = 0;
		if (ab_bd > 0) {
			double y = d.y - b.y;
			double x = d.x - b.x;
			reqAns = Math.sqrt(x * x + y * y);
		} else if (ab_ad < 0) {
			double y = d.y - a.y;
			double x = d.x - a.x;
			reqAns = Math.sqrt(x * x + y * y);
		} else {
			double x1 = ab.x;
			double y1 = ab.y;
			double x2 = ad.x;
			double y2 = ad.y;
			double mod = Math.sqrt(x1 * x1 + y1 * y1);
			reqAns = Math.abs(x1 * y2 - y1 * x2) / mod;
		}
		return reqAns;
	}

	/******************
	 * functions auxiliar of H points elmination
	 * 
	 **********************/
	// ha pr egy szakasz, megadja, hogy q pont a szakaszon van e
	public boolean onSegment(Point p, Point q, Point r) {
		if (q.x <= Math.max(p.x, r.x) && q.x >= Math.min(p.x, r.x) && q.y <= Math.max(p.y, r.y)
				&& q.y >= Math.min(p.y, r.y))
			return true;
		return false;
	}

	public int orientationSegment(Point p, Point q, Point r) {
		double val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
		if (val == 0)
			return 0; // colinear
		return (val > 0) ? 1 : 2; // clock or counterclock wise
	}

	public boolean sessionsIntersect(Point p1, Point p2, Point q1, Point q2) {
		int o1 = orientationSegment(p1, p2, q1);
		int o2 = orientationSegment(p1, p2, q2);
		int o3 = orientationSegment(q1, q2, p1);
		int o4 = orientationSegment(q1, q2, p2);
		if (o1 != o2 && o3 != o4)
			return true;
		// Special Cases
		// p1, q1 and p2 are colinear and p2 lies on segment p1q1
		if (o1 == 0 && onSegment(p1, p2, q1))
			return true;
		// p1, q1 and q2 are colinear and q2 lies on segment p1q1
		if (o2 == 0 && onSegment(p1, q2, q1))
			return true;
		// p2, q2 and p1 are colinear and p1 lies on segment p2q2
		if (o3 == 0 && onSegment(p2, p1, q2))
			return true;
		// p2, q2 and q1 are colinear and q1 lies on segment p2q2
		if (o4 == 0 && onSegment(p2, q1, q2))
			return true;
		return false;
	}

	public Point midPoint(Point a, Point b) {
		double halfX = (a.x + b.x) / 2;
		double halfY = (a.y + b.y) / 2;
		return new Point(halfX, halfY);
	}

	public double angleOfIntersectPoint(Point a1, Point a2, Point b1, Point b2) {
		Point a = new Point(a2.x - a1.x, a2.y - a1.y);
		Point b = new Point(b2.x - b1.x, b2.y - b1.y);
		double aLength = Math.sqrt(Math.pow(a.x, 2) + Math.pow(a.y, 2));
		a.x /= aLength;
		a.y /= aLength;
		double bLength = Math.sqrt(Math.pow(b.x, 2) + Math.pow(b.y, 2));
		b.x /= bLength;
		b.y /= bLength;
		double dot = (a.x * b.x) + (a.y * b.y);
		return Math.acos(dot);
	}

	public Mat allMinutia() {
		Mat out = this.ridge.getMatrix().clone();
		Scalar red = new Scalar(0, 0, 255);
		Scalar blue = new Scalar(255, 0, 0);
		Scalar purple = new Scalar(255, 0, 255);
		Scalar green = new Scalar(50, 180, 0);
		for (Map.Entry<Minutiae, Ridges> m : ridge.getMinutiaeMapFinal().entrySet()) {
			if (m.getKey().getType().equals("ending")) {
				Imgproc.circle(out, m.getKey().getLocation(), 2, red);
			} else {
				Imgproc.circle(out, m.getKey().getLocation(), 2, blue);
			}
		}
		for (Map.Entry<Minutiae, Ridges> m : valley.getMinutiaeMapFinal().entrySet()) {
			if (m.getKey().getType().equals("ending")) {
				Imgproc.circle(out, m.getKey().getLocation(), 2, purple);
			} else {
				Imgproc.circle(out, m.getKey().getLocation(), 2, green);
			}
		}
		return out;
	}

	// a minucia pont egy cella, ezért onnan kiindulva, megadom a
	// bifurcation 3 legtávolabbi pontjait
	public double orientationVector(Cell cell1, Cell cell2) {
		double x = cell2.x - cell1.x;
		double y = cell2.y - cell1.y;
		return Math.atan2(y, x);
	}

	/************
	 * 7. all potential minutiaes detection
	 * 
	 * keresztszámos módszerrel, ha CN==1 -> végzõdés, ha CN==3 -> elágazás
	 ***********/
	// Minutiae List for 2 types of minutiae (bifurcation, termination)
	public Set<Minutiae> minutiaeList(double[][] array) {
		Set<Minutiae> allMinutiaeList = new HashSet<Minutiae>();
		for (int i = 1; i < height - 1; i++) {
			for (int j = 1; j < width - 1; j++) {
				double cn = 0;
				double R1 = array[i][j + 1];
				double R2 = array[i - 1][j + 1];
				double R3 = array[i - 1][j];
				double R4 = array[i - 1][j - 1];
				double R5 = array[i][j - 1];
				double R6 = array[i + 1][j - 1];
				double R7 = array[i + 1][j];
				double R8 = array[i + 1][j + 1];
				cn = ((Math.abs(R1 - R2) + Math.abs(R2 - R3) + Math.abs(R3 - R4) + Math.abs(R4 - R5) + Math.abs(R5 - R6)
						+ Math.abs(R6 - R7) + Math.abs(R7 - R8) + Math.abs(R8 - R1)) / 2);
				if (cn > 2 && array[i][j] == 1) {
					Minutiae temp = new Minutiae(new Point(j, i), "bifurcation");
					allMinutiaeList.add(temp);
				} else if (cn == 1 && array[i][j] == 1) {
					Minutiae temp = new Minutiae(new Point(j, i), "ending");
					allMinutiaeList.add(temp);
				}
			}
		}
		return allMinutiaeList;
	}

	// segédfüggvény: Mat obj-ot (3 csatornás) 2 D mátrix (256*288)
	public double[][] matToArray(Mat m) {
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

	// segédfüggvény: kiszámítja a redõk közötti átlagos távolságot
	public double avgRidgeDistance(Mat input) {
		double sum = 0;
		double distance = 0;
		int countRidge = 0;
		double[][] pixels = matToArray(input);
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				if (pixels[i][j] == 1) {
					countRidge++;
				}
			}
			double temp = 0;
			if (countRidge != 0)
				temp = width / countRidge;
			countRidge = 0;
			sum += temp;
		}
		distance = sum / height;
		return distance;
	}

	// show termination - bifurcation orientation in Mat obj.
	// szögek tárolása PI radiánban 0-6.28 a Minutiae osztály egyik adattagjaként
	// kirajzoláshoz ezt át kell váltani elõbb fokba, majd orientationToDraw
	// fgv-nyel
	public Mat drawMinutiaeDirectionWithLabel() {
		Mat out = this.ridge.getMatrix().clone();
		Scalar termColor = new Scalar(190, 30, 190); // lila
		Scalar bifColor = new Scalar(50, 180, 30); // zöld
		for (Map.Entry<Minutiae, Ridges> minutiae : this.ridge.getMinutiaeMapFinal().entrySet()) {
			if (minutiae.getKey().getType().equals("bifurcation")) {
				Point p1 = minutiae.getKey().getLocation();
				double angle = orientationForFullAngle(minutiae.getKey().getOrientation());
				Point p2 = angle(Math.toDegrees(angle), p1, avgRidgeDistance);
				int x = (int) minutiae.getKey().getLocation().x;
				int y = (int) minutiae.getKey().getLocation().y;
				String str = x + " " + y;
				int ang = (int) Math.toDegrees(minutiae.getKey().getOrientation());
				String an = String.valueOf(ang);
				Imgproc.putText(out, str, minutiae.getKey().getLocation(), Imgproc.FONT_HERSHEY_SIMPLEX, 0.28, red);
				Imgproc.putText(out, an,
						new Point(minutiae.getKey().getLocation().x + 10, minutiae.getKey().getLocation().y - 8),
						Imgproc.FONT_HERSHEY_SIMPLEX, 0.3, red);
				Imgproc.circle(out, minutiae.getKey().getLocation(), 2, bifColor, 1);// BGR - green
				Imgproc.line(out, p1, p2, bifColor, 1);
			} else {
				Point p1 = minutiae.getKey().getLocation();
				double angle = orientationForFullAngle(minutiae.getKey().getOrientation());
				Point p2 = angle(Math.toDegrees(angle), p1, avgRidgeDistance);
				int x = (int) minutiae.getKey().getLocation().x;
				int y = (int) minutiae.getKey().getLocation().y;
				String str = x + " " + y;
				int ang = (int) Math.toDegrees(minutiae.getKey().getOrientation());
				String an = String.valueOf(ang);
				Imgproc.putText(out, str, minutiae.getKey().getLocation(), Imgproc.FONT_HERSHEY_SIMPLEX, 0.28, blue);
				Imgproc.putText(out, an,
						new Point(minutiae.getKey().getLocation().x + 10, minutiae.getKey().getLocation().y - 8),
						Imgproc.FONT_HERSHEY_SIMPLEX, 0.3, blue);
				Imgproc.circle(out, minutiae.getKey().getLocation(), 2, termColor, 1);// BGR - purple
				Imgproc.line(out, p1, p2, termColor, 1);
			}
		}
		return out;
	}

	public Mat drawMinutiaeDirection() {
		Mat out = this.ridge.getMatrix().clone();
		Scalar termColor = new Scalar(190, 30, 190); // lila
		Scalar bifColor = new Scalar(50, 180, 30); // zöld
		for (Map.Entry<Minutiae, Ridges> minutiae : this.ridge.getMinutiaeMapFinal().entrySet()) {
			if (minutiae.getKey().getType().equals("bifurcation")) {
				Point p1 = minutiae.getKey().getLocation();
				double angle = orientationForFullAngle(minutiae.getKey().getOrientation());
				Point p2 = angle(Math.toDegrees(angle), p1, avgRidgeDistance);
				Imgproc.circle(out, minutiae.getKey().getLocation(), 2, bifColor, 1);// BGR - green
				Imgproc.line(out, p1, p2, bifColor, 1);
			} else {
				Point p1 = minutiae.getKey().getLocation();
				double angle = orientationForFullAngle(minutiae.getKey().getOrientation());
				Point p2 = angle(Math.toDegrees(angle), p1, avgRidgeDistance);
				Imgproc.circle(out, minutiae.getKey().getLocation(), 2, termColor, 1);// BGR - purple
				Imgproc.line(out, p1, p2, termColor, 1);
			}
		}
		return out;
	}

	// vékonyított redõvonal és barázdavonal kép összemergelése - szemléltetéshez
	public Mat combinatedRidgeValleyFromThinned(Mat ridge, Mat valley) {
		Mat out = new Mat(ridge.size(), ridge.type());
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				double[] data = ridge.get(i, j);
				double[] outData = new double[ridge.channels()];
				if (data[0] == 0) { // redõvonal zöld
					outData[0] = 254;
					outData[1] = 0;
					outData[2] = 0;
				} else { // háttér fehér
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
				if (data[0] == 0 && ridgeData[0] == 254) { // ahol metszi a redõ a barázdát
					outData[0] = 254; // legyen kék
					outData[1] = 0;
					outData[2] = 0;
				} else if (data[0] == 0 && ridgeData[0] == 255) { // ahol csak barázda van
					outData[0] = 255; // legyen lila
					outData[1] = 0;
					outData[2] = 255;
				} else if (data[0] == 255 && ridgeData[0] == 254) { // ahol csak redõ van
					outData[0] = 254;
					outData[1] = 0;
					outData[2] = 0;
				} else if (data[0] == 255 && ridgeData[0] == 255) { // háttér marad is
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
}
