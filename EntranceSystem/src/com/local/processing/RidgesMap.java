package processing;

import static processing.ImageProcessing.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class RidgesMap {

	/**
	 * private class variables
	 * 
	 */
	private Set<Minutiae> set;
	private Mat matrix;
	private Map<Cell, Ridges> minutiaeMap;
	private Map<Minutiae, Ridges> minutiaeMapFinal;

	/**
	 * constructor create the minutiaeMapFinal member in that maps the minutiae ->
	 * Ridges (all calculated data of minutiae)
	 * 
	 * @param matrix
	 * @throws IOException
	 */
	public RidgesMap(Mat matrix) throws IOException {
		this.matrix = matrix;
		double[][] thinnedArrayRidge = matToArray(matrix);
		//double [][] correctedThinnedRidge = doubleBifurcation(thinnedArrayRidge);
		this.set = minutiaeList(thinnedArrayRidge);
		this.minutiaeMap = new HashMap<Cell, Ridges>();
		Map<Cell, Minutiae> minutiaePoints = new HashMap<Cell, Minutiae>();
		for (Minutiae m : this.set) {
			Cell cell = pointToCell(m.getLocation());
			minutiaePoints.put(cell, m);
		}
		for (Map.Entry<Cell, Minutiae> mapMin : minutiaePoints.entrySet()) {
			if (mapMin.getValue().getType().equals("ending")) {
				Ridges ridge = new Ridges("ending");
				this.minutiaeMap.put(mapMin.getKey(), ridge);
			} else if (mapMin.getValue().getType().equals("bifurcation")) {
				Ridges ridge = new Ridges("bifurcation");
				this.minutiaeMap.put(mapMin.getKey(), ridge);
			}
		}
		LogicMatrix matrixRidge = new LogicMatrix(matrix);
		calculateRidgesData(matrixRidge);
		this.minutiaeMapFinal = new HashMap<Minutiae, Ridges>();
		for (Minutiae m : this.set) {
			for (Map.Entry<Cell, Ridges> map : this.minutiaeMap.entrySet()) {
				if (m.getLocation().equals(map.getKey().toPoint())) {
					this.minutiaeMapFinal.put(m, map.getValue());
				}
			}
		}
	}


	/**
	 * calculates all of data about terminations, bifurcations
	 * 
	 * @param thinned
	 */
	public void calculateRidgesData(LogicMatrix thinned) {
		findTerminations(thinned);
		findBifurcations(thinned);
		farPoints();
		pointPairs();
		terminationAngle();
		bifurcationAngle();
	}

	
	public double [][] doubleBifurcation(double[][] array)  {
		double [][] out = new double[288][256];
		for (int i= 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				out[i][j] = array[i][j];
			}
		}
		for (int i = 1; i < height - 1; i++) {
			for (int j = 1; j < width - 1; j++) {
				double cn = cnCalculation(i, j, out);
				if (cn == 2 && out[i][j] == 1 && validationBugPixel(i, j, out)) {
					out[i][j] = 0;
				}
				if (cn == 2 && out[i][j] == 1 && !validationBugPixel(i, j, out)) {
					out[i][j] = 1;
				} 
			}
		}
		return out;
	}
	
	
	/**
	 * marks all minutiae in thinned image by Cross Number (CN) method
	 * 
	 * @param array
	 * @return in Minutiae Set
	 * @throws IOException
	 */
	public Set<Minutiae> minutiaeList(double[][] array) throws IOException {
		Mat out = this.matrix.clone();
		Set<Minutiae> allMinutiaeList = new HashSet<Minutiae>();
		for (int i = 1; i < height - 1; i++) {
			for (int j = 1; j < width - 1; j++) {
				double cn = cnCalculation(i, j, array);
				if (cn == 3 && array[i][j] == 1) {
					Minutiae temp = new Minutiae(new Point(j, i), "bifurcation");
					allMinutiaeList.add(temp);
				} else if (cn == 1 && array[i][j] == 1) {
					Minutiae temp = new Minutiae(new Point(j, i), "ending");
					allMinutiaeList.add(temp);
				} if (cn == 4 && array[i][j] == 1) {
					Imgproc.circle(out, new Point(j, i), 5, new Scalar(255,  0,  0));
				} 
			}
		}
		//resizeAndShow(out, "double bifurcation");
		return allMinutiaeList;
	}

	public int isFourConnected(int i, int j, double[][] array) {
		boolean valid = false;
		int count = 0;
		for (int u = i - 1; u < i + 2; u++) {
			for (int v = j - 1; v < j + 2; v++) {
				if (!(u == i && v == j)) {
					if (array[u][v] == 1) {
						count++;
					}
				}
			}
		}
		if (count >= 4)
			valid = true;
		return count;
	}

	public boolean validationBugPixel(int i, int j, double[][] array) {
		boolean valid = false;
		int count = 0;
		for (int u = i - 1; u < i + 2; u++) {
			for (int v = j - 1; v < j + 2; v++) {
				if (!(i == u && v == j)) {
					if (isFourConnected(u, v, array) >=4) {
						count++;
					}
				}
			}
		}
		if (count > 2) {
			valid = true;
			System.out.println("ertek: " + j + " " + i + " " + count);
		}
		return valid;
	}

	/**calculate the Crossing Number for each pixel
	 * @param i
	 * @param j
	 * @param array
	 * @return Crossing Number
	 */
	public double cnCalculation(int i, int j, double[][] array) {
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
		return cn;
	}

	/**
	 * method auxiliary: Mat object (3 channels) -> double array of 2 dimensions
	 * (256 × 288)
	 * 
	 * @param m
	 * @return
	 */
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

	/**
	 * draw marking circles for candidate minutiae in input (always thinned) image
	 * 
	 * @return
	 */
	public Mat drawMinutiae() {
		Mat out = this.matrix.clone();
		for (Minutiae minutiae : this.set) {
			if (minutiae.getType().equals("bifurcation"))
				Imgproc.circle(out, minutiae.getLocation(), 2, new Scalar(148, 34, 40), 1);// BGR - blue
			else
				Imgproc.circle(out, minutiae.getLocation(), 2, new Scalar(0, 0, 255), 1);// BGR - red
		}
		return out;
	}

	public void removeRecord(Minutiae minutia) {
		this.minutiaeMapFinal.remove(minutia);
	}

	/**
	 * visualizes in thinned image each termination
	 * 
	 * @return
	 */
	public Mat terminationMatrix() {
		Mat out = matrix.clone();
		for (Map.Entry<Minutiae, Ridges> m : this.minutiaeMapFinal.entrySet()) {
			int dist = 12;
			if (m.getValue().type.equals("ending")) {
				for (Cell c : m.getValue().terminations) {
					int i = c.y;
					int j = c.x;
					double[] data = new double[3];
					data[0] = 0;
					data[1] = 0;
					data[2] = 255;
					out.put(i, j, data);

				}
				Point p1 = new Point(m.getKey().getLocation().x - dist, m.getKey().getLocation().y - dist);
				Point p2 = new Point(m.getKey().getLocation().x + dist, m.getKey().getLocation().y + dist);
				Rect rec = new Rect(p1, p2);
				Imgproc.circle(out, new Point(m.getKey().getLocation().x, m.getKey().getLocation().y), 1,
						new Scalar(255, 0, 0));
				// Imgproc.rectangle(out, rec, new Scalar(0, 255,0), 1);
			}
		}
		return out;
	}

	/**
	 * visualizes in thinned image each bifurcation (blue)
	 * 
	 * @return
	 */
	public Mat bifurcationMatrix() {
		Mat out = matrix.clone();
		for (Map.Entry<Minutiae, Ridges> m : this.minutiaeMapFinal.entrySet()) {
			int dist = 12;
			if (m.getValue().type.equals("bifurcation")) {
				for (Cell c : m.getValue().bifurcations) {
					int i = c.y;
					int j = c.x;
					double[] data = new double[3];
					data[0] = 100;
					data[1] = 255;
					data[2] = 100;
					out.put(i, j, data);
				}
				Point p1 = new Point(m.getKey().getLocation().x - dist, m.getKey().getLocation().y - dist);
				Point p2 = new Point(m.getKey().getLocation().x + dist, m.getKey().getLocation().y + dist);
				Rect rec = new Rect(p1, p2);
				Imgproc.circle(out, new Point(m.getKey().getLocation().x, m.getKey().getLocation().y), 1,
						new Scalar(255, 0, 0));
				// Imgproc.rectangle(out, rec, new Scalar(0, 255,0), 1);
			}
		}
		return out;
	}

	/**
	 * calculates the angles of bifurcations in base of the angles of terminations
	 * of its dual image
	 * 
	 * @param dualMap
	 */
	public void calculateBifurcation(Map<Minutiae, Ridges> dualMap) {
		for (Map.Entry<Minutiae, Ridges> bif : this.minutiaeMapFinal.entrySet()) {
			TreeMap<Double, Minutiae> neighbours = new TreeMap<>();
			boolean findNeighbour = false;
			for (Map.Entry<Minutiae, Ridges> term : dualMap.entrySet()) {
				double dist = distanceEuclidean(bif.getKey().getLocation(), term.getKey().getLocation());
				if (dist < 15) {
					if (bif.getKey().getType().equals("bifurcation") && term.getKey().getType().equals("ending")) {
						findNeighbour = true;
						if (neighbours.containsKey(dist)) {
							dist += 0.000001;
						}
						neighbours.put(dist, term.getKey());
					}
				}
			}
			if (findNeighbour) {
				Map.Entry<Double, Minutiae> minutia = neighbours.firstEntry();
				bif.getKey().setOrientation(minutia.getValue().getOrientation());
			}
		}
	}

	/**
	 * in member minutiaeMapFinal set the angle of each termination
	 * 
	 */
	public void setOrientationForTermination() {
		for (Map.Entry<Minutiae, Ridges> m : this.minutiaeMapFinal.entrySet()) {
			if (m.getKey().getType().equals("ending")) {
				m.getKey().setOrientation(m.getValue().angleTerm);
			}
		}
	}

	/**
	 * gives the suitable orientation (from 8 direction) of the mask
	 * 
	 * @param start
	 * @param length
	 * @param index
	 * @return
	 */
	public int location(int start, int length, int index) {
		return start + index < length ? start + index : start + index - length;
	}

	/**
	 * for each termination point find the nearest pixels round the minutia and save
	 * them in terminations member
	 * 
	 * @param thinned
	 */
	private void findTerminations(LogicMatrix thinned) {
		int ridgePixelSize = 15;
		for (Map.Entry<Cell, Ridges> minutiaPoint : this.minutiaeMap.entrySet()) {
			if (minutiaPoint.getValue().type.equals("ending")) {
				minutiaPoint.getValue().terminations.add(minutiaPoint.getKey());
				for (Cell startRelative : Cell.cornerNeighbors) {
					Cell start = minutiaPoint.getKey().plus(startRelative);
					if (thinned.get(start, false) && !minutiaPoint.getValue().terminations.contains(start)) {
						minutiaPoint.getValue().terminations.add(start);
						Cell previous = minutiaPoint.getKey();
						Cell current = start;
						int count = 0;
						do {
							Cell next = Cell.zero;
							for (Cell nextRelative : Cell.cornerNeighbors) {
								next = current.plus(nextRelative);
								if (thinned.get(next, false) && !next.equals(previous)) {
									break;
								}
							}
							previous = current;
							current = next;
							if (thinned.get(current, false))
								minutiaPoint.getValue().terminations.add(current);
							count++;
						} while (count < ridgePixelSize);
					}
				}
			}
		}
	}

	/**
	 * each bifurcation points has 3 branches find the nearest pixels round the
	 * minutia and save them in bifurcations member
	 * 
	 * @param thinned
	 */
	private void findBifurcations(LogicMatrix thinned) {
		for (Map.Entry<Cell, Ridges> minutiaPoint : this.minutiaeMap.entrySet()) {
			if (minutiaPoint.getValue().type.equals("bifurcation")) {
				minutiaPoint.getValue().bifurcations.add(minutiaPoint.getKey());
				for (Cell cell : Cell.cornerNeighbors) {
					Cell start = minutiaPoint.getKey().plus(cell);
					if (thinned.get(start, false)) {
						minutiaPoint.getValue().neighbours.add(start);
					}
				}
			}
		}
		for (Map.Entry<Cell, Ridges> map : this.minutiaeMap.entrySet()) {
			if (map.getValue().type.equals("bifurcation")) {
				int length = 8;
				int countMask = 8;
				int neighbourhoodSize = 15;
				int c, d, e = 0;
				for (int step = 0; step < countMask; step++) {
					c = 0;
					while (c < length) {
						int loc = location(step, length, c);
						c++;
						Cell start = map.getKey().plus(Cell.cornerNeighbors[loc]);
						if (thinned.get(start, false)) {
							map.getValue().bifurcations.add(start);
							Cell previous = map.getKey();
							Cell current = start;
							e = 0;
							do {
								Cell next = Cell.zero;
								d = 0;
								while (d < length) {
									int loc2 = location(step, length, d);
									d++;
									next = current.plus(Cell.cornerNeighbors[loc2]);
									if (thinned.get(next, false) && !next.equals(previous)) {
										break;
									}
								}
								previous = current;
								current = next;
								if (thinned.get(current, false)) {
									map.getValue().bifurcations.add(current);
								}
								e++;
							} while (e < neighbourhoodSize);
						}
					}
				} 
			} 
		} 
	}

	/**
	 * each bifurcation has 3 neighbours each neighbour has the farest point from
	 * all ridge pixels (bifurcations member)
	 */
	private void farPoints() {
		for (Map.Entry<Cell, Ridges> e : this.minutiaeMap.entrySet()) {
			Map<Double, Cell> distances = new TreeMap<>();
			if (e.getValue().type.equals("bifurcation")) {
				for (Cell c : e.getValue().bifurcations) {
					double distance = distanceEuclidean(e.getKey(), c);
					if (distances.containsKey(distance)) {
						distance -= 0.000000001;
					}
					distances.put(distance, c);
				}

				Map<Double, Cell> distancesDesc = new TreeMap<>(Collections.reverseOrder());
				distancesDesc.putAll(distances);
				Object o1 = distancesDesc.values().toArray()[0];
				Cell cMax1 = (Cell) o1;
				Cell cMax2 = null, cMax3 = null;
				for (int i = 0; i < distancesDesc.values().size(); i++) {
					if (!near((Cell) distancesDesc.values().toArray()[i], cMax1, 2)) {
						cMax2 = (Cell) distancesDesc.values().toArray()[i];
						break;
					}
				}
				for (int i = 0; i < distancesDesc.values().size(); i++) {
					if (!near((Cell) distancesDesc.values().toArray()[i], cMax1, 2)
							&& !near((Cell) distancesDesc.values().toArray()[i], cMax2, 2)) {
						cMax3 = (Cell) distancesDesc.values().toArray()[i];
						break;
					}
				}
				this.minutiaeMap.get(e.getKey()).farPoints.add(cMax1);
				this.minutiaeMap.get(e.getKey()).farPoints.add(cMax2);
				this.minutiaeMap.get(e.getKey()).farPoints.add(cMax3);
			}
		}
	}

	/**
	 * each termination has 3 branches generate the pairs map of the nearest and the
	 * farest points from the pixels of branches
	 */
	private void pointPairs() {
		for (Map.Entry<Cell, Ridges> m : this.minutiaeMap.entrySet()) {
			if (m.getValue().type.equals("bifurcation")) {
				for (Cell far : m.getValue().farPoints) {
					double min = Double.MAX_VALUE;
					Cell minElement = null;
					for (Cell near : m.getValue().neighbours) {
						double distance = distanceEuclidean(far, near);
						if (distance < min) {
							min = distance;
							minElement = near;
						}
					}
					m.getValue().pointpairs.put(far, minElement);
				}
			}
		}
	}

	/**
	 * calculates the angle from x axis of each termination angle of the session
	 * between the minutia point and the farest point
	 * 
	 */
	private void terminationAngle() {
		for (Map.Entry<Cell, Ridges> m : this.minutiaeMap.entrySet()) {
			if (m.getValue().type.equals("ending")) {
				double max = Double.MIN_VALUE;
				Cell maxCell = null;
				for (Cell cell : m.getValue().terminations) {
					double distance = distanceEuclidean(m.getKey(), cell);
					if (distance > max) {
						max = distance;
						maxCell = cell;
					}
				}
//				List <Cell> list = new ArrayList<>();
//		    	list.addAll(m.getValue().terminations);
//		    	Collections.sort(list, createComparator(maxCell));
				double angleOriginal = orientation(m.getKey(), maxCell);
//				Cell newTerm = termangle2(maxCell, list, angleOriginal);
				angleOriginal = orientationForFullAngle(angleOriginal);
				m.getValue().setAngleTerm(angleOriginal);
			}
		}
	}

	/**
	 * marks the terminations differing from normal ridge orientation
	 * 
	 * @param base
	 * @param list
	 * @param angleOriginal
	 * @return
	 */
	public static Cell termangle2(Cell base, List<Cell> list, double angleOriginal) {
		Map<Cell, Double> map = new HashMap<>();
		double sum = 0;
		double avg = 0;
		for (int i = 1; i < list.size(); i++) {
			double temp = orientation(base, list.get(i));
			sum += temp;
			map.put(list.get(i), temp);
		}
		avg = sum / (list.size() - 1);
		double max = Double.MIN_VALUE;
		Cell p = null;
		double szog = 0;
		for (Map.Entry<Cell, Double> m : map.entrySet()) {
			double diff = Math.abs(m.getValue() - angleOriginal);
			if (diff > max) {
				max = diff;
				p = m.getKey();
				szog = m.getValue();
			}
		}
//		Cell before = base;
//		Cell newTermination = null;
//		for (Cell r : list) {
//			if (r == p) {
//				newTermination = before;
//			}
//			before = r;
//		}
		return p;
	}

	/**
	 * calculates the angle of branches of bifurcation to member: anglesBif in base
	 * of the member pointpairs, that means the distance between the near and far
	 * points of the branches
	 */
	private void bifurcationAngle() {
		for (Map.Entry<Cell, Ridges> e : this.minutiaeMap.entrySet()) { // a Ridges osztÃ¡ly adattagja
			if (e.getValue().type.equals("bifurcation")) {
				for (Map.Entry<Cell, Cell> c : e.getValue().pointpairs.entrySet()) {
					double angle = orientation(c.getValue(), c.getKey());
					angle = orientationForFullAngle(angle);
					if (e.getValue().anglesBif.contains(angle)) {
						angle -= 0.00000001;
					}
					e.getValue().anglesBif.add(angle);
				}
			}
		}
	}

	/**
	 * toString method
	 * 
	 * @return
	 */
	public String print() {
		String out = "Class: " + this.getClass();
		int term = 1;
		int bif = 1;
		for (Map.Entry<Cell, Ridges> m : this.minutiaeMap.entrySet()) {
			if (m.getValue().type.equals("ending")) {
				out += "\n" + term + ". minutia, tipus: " + m.getValue().type + " " + m.getKey();
				if (m.getValue().terminations.size() != 0) {
					out += ", angle: " + Math.toDegrees(m.getValue().angleTerm);
				}
				term++;
			} else if (m.getValue().type.equals("bifurcation")) {
				out += "\n" + bif + ". minutia, tipus: " + m.getValue().type + " " + m.getKey();
				if (m.getValue().bifurcations.size() != 0) {
					out += ", angle: " + Math.toDegrees(m.getValue().angleBif);
					out += " branch angles: \n";
					for (double d : m.getValue().anglesBif) {
						out += Math.toDegrees(d) + "\t";
					}
				}
				bif++;
			}
		}
		return out;
	}

	/**
	 * getters - setters
	 * 
	 * @return
	 */
	public Set<Minutiae> getSet() {
		return set;
	}

	public void setSet(Set<Minutiae> set) {
		this.set = set;
	}

	public Mat getMatrix() {
		return matrix;
	}

	public void setMatrix(Mat matrix) {
		this.matrix = matrix;
	}

	public Map<Cell, Ridges> getMinutiaeMap() {
		return minutiaeMap;
	}

	public void setMinutiaeMap(Map<Cell, Ridges> minutiaeMap) {
		this.minutiaeMap = minutiaeMap;
	}

	public Map<Minutiae, Ridges> getMinutiaeMapFinal() {
		return minutiaeMapFinal;
	}

	public void setMinutiaeMapFinal(Map<Minutiae, Ridges> minutiaeMapFinal) {
		this.minutiaeMapFinal = minutiaeMapFinal;
	}

	/**
	 * compare two Cell in base of their distance from a given point
	 * 
	 * @param c
	 * @return
	 */
	public static Comparator<Cell> createComparator(Cell c) {
		Cell init = new Cell(c.x, c.y);
		return new Comparator<Cell>() {
			@Override
			public int compare(Cell c1, Cell c2) {
				double dist1 = distanceEuclidean(init, c1);
				double dist2 = distanceEuclidean(init, c2);
				return Double.compare(dist2, dist1);
			}
		};
	}
}
