package processing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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

	/*************************
	 * I. FALSE TERMINATION ELIMINATION according to ZHAO algorithm
	 *******************/

	/*
	 * 0/0. step: dots elimination if rested after preprocessing
	 */
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
		// resizeAndShow(out, "too short term");
	}

	/*
	 * 0/1. step: lakes elimination if rested after preprocessing
	 */
	public void eliminationLake() throws IOException {
		Mat out = this.ridge.getMatrix().clone();
		Set<Minutiae> section = new HashSet<>();
		for (Map.Entry<Minutiae, Ridges> termExt : valley.getMinutiaeMapFinal().entrySet()) {
			for (Map.Entry<Minutiae, Ridges> termInt : valley.getMinutiaeMapFinal().entrySet()) {
				if (termExt.getKey().getType().equals("ending") && termInt.getKey().getType().equals("ending")
						&& !termExt.getKey().getLocation().equals(termInt.getKey().getLocation())
						&& distanceEuclidean(termExt.getKey().getLocation(), termInt.getKey().getLocation()) < 30) {
					for (Map.Entry<Minutiae, Ridges> bifExt : ridge.getMinutiaeMapFinal().entrySet()) {
						for (Map.Entry<Minutiae, Ridges> bifInt : ridge.getMinutiaeMapFinal().entrySet()) {
							if (bifExt.getKey().getType().equals("bifurcation")
									&& bifInt.getKey().getType().equals("bifurcation")
									&& !bifExt.getKey().getLocation().equals(bifInt.getKey().getLocation())
									&& distanceEuclidean(bifExt.getKey().getLocation(),
											bifInt.getKey().getLocation()) < 40) {
								if ((distanceEuclidean(bifExt.getKey().getLocation(),
										termExt.getKey().getLocation()) < 6
										|| distanceEuclidean(bifExt.getKey().getLocation(),
												termInt.getKey().getLocation()) < 6)
										&& (distanceEuclidean(bifInt.getKey().getLocation(),
												termExt.getKey().getLocation()) < 6
												|| distanceEuclidean(bifInt.getKey().getLocation(),
														termInt.getKey().getLocation()) < 6)) {
									boolean containTerm = false;
//									boolean containBif = false;
									double distanceBif = distanceEuclidean(bifExt.getKey().getLocation(),
											bifInt.getKey().getLocation());
									double distanceTerm = distanceEuclidean(termExt.getKey().getLocation(),
											termInt.getKey().getLocation());
									for (Cell c : termExt.getValue().terminations) {
										if (termInt.getValue().terminations.contains(c))
											containTerm = true;
									}
//									for (Cell c : bifExt.getValue().bifurcations) {
//										if (bifInt.getValue().bifurcations.contains(c))
//											containBif = true;
//									}
									if (containTerm) {
										section.add(bifExt.getKey());
										section.add(bifInt.getKey());
										Imgproc.circle(out, bifExt.getKey().getLocation(), 3, purple);
										Imgproc.circle(out, bifInt.getKey().getLocation(), 3, purple);
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
		//resizeAndShow(out, "Marked lakes");
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

	/*
	 * 0/2. step: too near terminations
	 */
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
		// resizeAndShow(out, "Too near terms");
	}

	/*
	 * 1/1. step: islands in ridge (gaps) elimination - in ridge the distance
	 * between 2 terminations < 12 - the difference of ang1 of one termination and
	 * ang2 of other is within [0,15] (deg) - if the line session(term1, term2) =
	 * session -> the difference between session and ang1 || difference between
	 * session and ang2 is within [0,15] (deg)
	 */
	public void eliminationGap() throws IOException {
		Mat out = ridge.getMatrix().clone();
		Set<Minutiae> section = new HashSet<>();
		// minutiaeListRidge.removeAll(hpointsValley);????
		double low = Math.toRadians(0);
		double high = Math.toRadians(20);
		for (Map.Entry<Minutiae, Ridges> minExt : this.ridge.getMinutiaeMapFinal().entrySet()) {
			for (Map.Entry<Minutiae, Ridges> minInt : this.ridge.getMinutiaeMapFinal().entrySet()) {
				if (minExt.getKey().getType().equals("ending") && minInt.getKey().getType().equals("ending")
						&& distanceEuclidean(minExt.getKey().getLocation(), minInt.getKey().getLocation()) < 20
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
		// resizeAndShow(out, "ridge breaks");
	}

	public double angleTransform(double angle) {
		if (angle >= Math.PI) {
			angle -= Math.PI;
		}
		return angle;
	}

	/*
	 * 0/3. step: a bifurcation in a spur could be real bifurcation
	 */
	public void changeType() throws IOException {
		Mat out = this.ridge.getMatrix().clone();
		for (Map.Entry<Minutiae, Ridges> bifRidge : ridge.getMinutiaeMapFinal().entrySet()) {
			for (Map.Entry<Minutiae, Ridges> termRidge : ridge.getMinutiaeMapFinal().entrySet()) {
				if (bifRidge.getKey().getType().equals("bifurcation") && termRidge.getKey().getType().equals("ending")
						&& distanceEuclidean(bifRidge.getKey().getLocation(), termRidge.getKey().getLocation()) < 6) {
					for (Map.Entry<Minutiae, Ridges> termIntRidge : ridge.getMinutiaeMapFinal().entrySet()) {
						double sessionAngle = orientation(pointToCell(termRidge.getKey().getLocation()),
								pointToCell(termIntRidge.getKey().getLocation()));
						sessionAngle = orientationForFullAngle(sessionAngle);
						double diffAngle = Math.abs(termIntRidge.getValue().angleTerm - sessionAngle);
						if (!termRidge.getKey().equals(termIntRidge.getKey())
								&& termIntRidge.getKey().getType().equals("ending")
								&& distanceEuclidean(termRidge.getKey().getLocation(),
										termIntRidge.getKey().getLocation()) < 12
								&& diffAngle <= Math.toRadians(15)) {
							for (Map.Entry<Minutiae, Ridges> bifValley : valley.getMinutiaeMapFinal().entrySet()) {
								if (bifValley.getKey().getType().equals("bifurcation")) {
									double dist = minDistDotLine(termRidge.getKey().getLocation(),
											termIntRidge.getKey().getLocation(), bifValley.getKey().getLocation());
									if (dist <= 0.7) {
										// típust vált: term -> bif, átállítjuk az adatait is, ne legyen nullpointer ex.
										termRidge.getKey().setType("bifurcation");
										termRidge.getValue().setAngleBif(bifValley.getKey().getOrientation());
										termRidge.getValue().setBifurcations(bifValley.getValue().getBifurcations());
										Imgproc.circle(out, termRidge.getKey().getLocation(), 4, purple);
									}
								}
							}
						}
					}
				}
			}
		}
		//resizeAndShow(out, "change type term -> bif");
	}
	
	public void changeTypeInverse() throws IOException {
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
		//resizeAndShow(out, "change type bif->term");
	}

	public void changeType2() throws IOException {
		Mat out = this.ridge.getMatrix().clone();
		for (Map.Entry<Minutiae, Ridges> bifRidge : ridge.getMinutiaeMapFinal().entrySet()) {
			for (Map.Entry<Minutiae, Ridges> bifValley : valley.getMinutiaeMapFinal().entrySet()) {
				if (bifRidge.getKey().getType().equals("bifurcation")
						&& bifValley.getKey().getType().equals("bifurcation")
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
		//resizeAndShow(out, "change type 2, bif->term");
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

	/*
	 * 2. step: spur elimination if in ridge the distance between a bifurcation and
	 * a termination < threshold = 20 AND they are connected each other -> spur must
	 * be eliminated
	 */
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
		// resizeAndShow(out, "spur elimination");
	}

	/*
	 * 3. step: H-points detection and elimination: - 2 line sessions are
	 * intersected (1: in ridge bif-bif, 2: in valley term-term) - the distance
	 * between midpoint of session bb and midpoint of session tt < 6 - 25 >= angle
	 * of intersection <= 155
	 */
	public void eliminationHpoint() throws IOException {
		Mat out = ridge.getMatrix().clone();
		Set<Minutiae> section = new HashSet<>();
		for (Map.Entry<Minutiae, Ridges> bifMinExt : ridge.getMinutiaeMapFinal().entrySet()) {
			for (Map.Entry<Minutiae, Ridges> bifMinInt : ridge.getMinutiaeMapFinal().entrySet()) {
				if (bifMinExt.getKey().getType().equals("bifurcation")
						&& bifMinInt.getKey().getType().equals("bifurcation")
						&& distanceEuclidean(bifMinExt.getKey().getLocation(), bifMinInt.getKey().getLocation()) <= 12
						&& distanceEuclidean(bifMinExt.getKey().getLocation(), bifMinInt.getKey().getLocation()) != 0) {
					for (Map.Entry<Minutiae, Ridges> termMin1 : valley.getMinutiaeMapFinal().entrySet()) {
						for (Map.Entry<Minutiae, Ridges> termMin2 : valley.getMinutiaeMapFinal().entrySet()) {
							if (termMin1.getKey().getType().equals("ending")
									&& termMin2.getKey().getType().equals("ending")
									&& distanceEuclidean(termMin1.getKey().getLocation(),
											bifMinExt.getKey().getLocation()) <= 5
									&& distanceEuclidean(termMin2.getKey().getLocation(),
											bifMinInt.getKey().getLocation()) <= 5
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
//		// dual
//		for (Map.Entry<Minutiae, Ridges> bifMinExt : valley.getMinutiaeMapFinal().entrySet()) {
//			for (Map.Entry<Minutiae, Ridges> bifMinInt : valley.getMinutiaeMapFinal().entrySet()) {
//				if (bifMinExt.getKey().getType().equals("bifurcation")
//						&& bifMinInt.getKey().getType().equals("bifurcation")
//						&& distanceEuclidean(bifMinExt.getKey().getLocation(), bifMinInt.getKey().getLocation()) < 12
//						&& distanceEuclidean(bifMinExt.getKey().getLocation(), bifMinInt.getKey().getLocation()) != 0) {
//					for (Map.Entry<Minutiae, Ridges> termMin1 : ridge.getMinutiaeMapFinal().entrySet()) {
//						for (Map.Entry<Minutiae, Ridges> termMin2 : ridge.getMinutiaeMapFinal().entrySet()) {
//							if (termMin1.getKey().getType().equals("ending")
//									&& termMin2.getKey().getType().equals("ending")
//									&& distanceEuclidean(termMin1.getKey().getLocation(),
//											bifMinExt.getKey().getLocation()) <= 6
//									&& distanceEuclidean(termMin2.getKey().getLocation(),
//											bifMinExt.getKey().getLocation()) <= 6
//									&& !termMin1.getKey().getLocation().equals(termMin2.getKey().getLocation())) {
//								if (sessionsIntersect(bifMinExt.getKey().getLocation(),
//										bifMinInt.getKey().getLocation(), termMin1.getKey().getLocation(),
//										termMin2.getKey().getLocation())) {
//									Point midPointRidge = midPoint(bifMinExt.getKey().getLocation(),
//											bifMinInt.getKey().getLocation());
//									Point midPointValley = midPoint(termMin1.getKey().getLocation(),
//											termMin2.getKey().getLocation());
//									double distanceMidPoint = distanceEuclidean(midPointRidge, midPointValley);
//									if (distanceMidPoint < 6) {
//										double angleIntersect = Math.toDegrees(angleOfIntersectPoint(
//												bifMinExt.getKey().getLocation(), bifMinInt.getKey().getLocation(),
//												termMin1.getKey().getLocation(), termMin2.getKey().getLocation())); // radiánba
//										// 0-180-ig
//										if (angleIntersect >= 25 && angleIntersect <= 155) {
//											section.add(bifMinExt.getKey());
//											section.add(bifMinInt.getKey());
//											section.add(termMin1.getKey());
//											section.add(termMin2.getKey());
//											Imgproc.circle(out, bifMinExt.getKey().getLocation(), 3, red);
//											Imgproc.circle(out, bifMinInt.getKey().getLocation(), 3, red);
//											Imgproc.circle(out, termMin1.getKey().getLocation(), 3, blue);
//											Imgproc.circle(out, termMin2.getKey().getLocation(), 3, blue);
//										}
//									}
//								}
//							}
//						}
//					}
//				}
//			}
//		}

		for (Minutiae m : section) {
			ridge.removeRecord(m);
		}
		//resizeAndShow(out, "H-pontok eltávolítása");
	}

	/*
	 * 4/1. step: too near minutiae
	 */
	public void eliminationTooNearMinutiae() throws IOException {
		Mat out = this.ridge.getMatrix().clone();
		Set<Minutiae> section = new HashSet<>();
		for (Minutiae minExt : ridge.getMinutiaeMapFinal().keySet()) {
			for (Minutiae minInt : ridge.getMinutiaeMapFinal().keySet()) {
				if (!minExt.getLocation().equals(minInt.getLocation())
						&& distanceEuclidean(minExt.getLocation(), minInt.getLocation()) <= 8) {
					section.add(minExt);
					section.add(minInt);
					Imgproc.circle(out, minExt.getLocation(), 3, red);
					Imgproc.circle(out, minInt.getLocation(), 3, red);
				}

			}
		}
		for (Minutiae m : section) {
			ridge.removeRecord(m);
		}
		//resizeAndShow(out, "Too near minutiae");
	}

	/*
	 * 4/2. step: if a minutia has more than one minutia corresponding in dual
	 * skeleton
	 */

	public void notRealMinutiae() {
		Set<Minutiae> section = new HashSet<>();
		for (Map.Entry<Minutiae, Ridges> bifMin : ridge.getMinutiaeMapFinal().entrySet()) {
			HashSet<Minutiae> set = new HashSet<>();
			for (Map.Entry<Minutiae, Ridges> termMin : valley.getMinutiaeMapFinal().entrySet()) {
				if (bifMin.getKey().getType().equals("bifurcation") && termMin.getKey().getType().equals("ending")
						&& distanceEuclidean(termMin.getKey().getLocation(), bifMin.getKey().getLocation()) <= 5) {
					set.add(termMin.getKey());
				}
			}
			if (set.size() != 1 && bifMin.getKey().getType().equals("bifurcation")) {
				section.add(bifMin.getKey());
			}
		}
		for (Map.Entry<Minutiae, Ridges> termMin : ridge.getMinutiaeMapFinal().entrySet()) {
			HashSet<Minutiae> set = new HashSet<>();
			for (Map.Entry<Minutiae, Ridges> bifMin : valley.getMinutiaeMapFinal().entrySet()) {
				if (bifMin.getKey().getType().equals("bifurcation") && termMin.getKey().getType().equals("ending")
						&& distanceEuclidean(termMin.getKey().getLocation(), bifMin.getKey().getLocation()) <= 5) {
					set.add(bifMin.getKey());
				}
			}
			if (set.size() != 1 && termMin.getKey().getType().equals("ending")) {
				section.add(termMin.getKey());
			}
		}
		for (Minutiae m : section) {
			ridge.removeRecord(m);
		}
	}

	/*
	 * 5. step: minutiae's elimination if are too near to border
	 */
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
		// resizeAndShow(out, "border minutiae");
	}

	/**
	 * to static variable allContour calculates new contour that the convex border
	 * of non zero points of image
	 * 
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

	/**
	 * scales the mask size to mark the border area of ROI
	 * 
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

	/**
	 * make inverse of mask black -> background, white -> ROI
	 * 
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
	// pr the line section, is q in the line session?
	public boolean onSegment(Point p, Point q, Point r) {
		if (q.x <= Math.max(p.x, r.x) && q.x >= Math.min(p.x, r.x) && q.y <= Math.max(p.y, r.y)
				&& q.y >= Math.min(p.y, r.y))
			return true;
		return false;
	}

	public int orientationSegment(Point p, Point q, Point r) {
		double val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
		if (val == 0)
			return 0; 
		return (val > 0) ? 1 : 2; 
	}

	public boolean sessionsIntersect(Point p1, Point p2, Point q1, Point q2) {
		int o1 = orientationSegment(p1, p2, q1);
		int o2 = orientationSegment(p1, p2, q2);
		int o3 = orientationSegment(q1, q2, p1);
		int o4 = orientationSegment(q1, q2, p2);
		if (o1 != o2 && o3 != o4)
			return true;
		if (o1 == 0 && onSegment(p1, p2, q1))
			return true;
		if (o2 == 0 && onSegment(p1, q2, q1))
			return true;
		if (o3 == 0 && onSegment(p2, p1, q2))
			return true;
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

	public double orientationVector(Cell cell1, Cell cell2) {
		double x = cell2.x - cell1.x;
		double y = cell2.y - cell1.y;
		return Math.atan2(y, x);
	}

	/************
	 * 7. all potential minutiaes detection
	 * 
	 * crossing number if  CN==1 -> term, ha CN==3 -> bif
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

		// angles in radians
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
				Point p2 = angle(Math.toDegrees(angle), p1, 8);
				Point a = new Point(minutiae.getKey().getLocation().x - 3, minutiae.getKey().getLocation().y - 3);
				Point b = new Point(minutiae.getKey().getLocation().x + 3, minutiae.getKey().getLocation().y + 3);
				Imgproc.rectangle(out, a, b, blue, 1);// BGR - green
				Imgproc.line(out, p1, p2, blue, 2);
			} else {
				Point p1 = minutiae.getKey().getLocation();
				double angle = orientationForFullAngle(minutiae.getKey().getOrientation());
				Point p2 = angle(Math.toDegrees(angle), p1, 8);
				Imgproc.circle(out, minutiae.getKey().getLocation(), 4, termColor, 1);// BGR - purple
				Imgproc.line(out, p1, p2, termColor, 2);
			}
		}
		return out;
	}

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
