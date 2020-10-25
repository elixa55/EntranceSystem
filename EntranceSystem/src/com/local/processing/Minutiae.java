package processing;

import org.opencv.core.Point;

public class Minutiae {
	/**member fields of class
	 * 1, location: gives the coordinate of minutia
	 * 2, direction: gives the angle between the ridge line and x axis
	 * 		the first time set in 0
	 * 3, type: type of minutia (could be termination or bifurcation)
	 */
	private Point location;
	private double orientation;
	private String type;
	
	/**constructor
	 * @param location
	 * @param type
	 */
	public Minutiae(Point location, String type) {
		this.location = location;
		this.orientation = 0.0;
		this.type = type;
	}
	
	public Minutiae(Point location, double orientation, String type) {
		this.location = location;
		this.orientation = orientation;
		this.type = type;
	}
	
	/**getters - setters
	 * @return
	 */
	public Point getLocation() {
		return location;
	}
	public void setLocation(Point location) {
		this.location = location;
	}
	public double getOrientation() {
		return orientation;
	}
	public void setOrientation(double orientation) {
		this.orientation = orientation;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
}
