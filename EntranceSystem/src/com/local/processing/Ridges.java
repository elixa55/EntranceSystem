package processing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Ridges {
	
	public String type;
	public boolean differ;
	public Cell newPoint;
	// termination ridges data
	public Set<Cell> terminations;
	public double angleTerm;
	// bifurcation ridges data
	public Set<Cell> bifurcations;
	public Set<Cell> neighbours;
	public Set<Cell> farPoints;
	public Map<Cell, Cell> pointpairs;
	public Set<Double> anglesBif;  
	public double angleBif;
	
	public Ridges(String type) {
		this.type = type;
		if (this.type.equals("ending")) {
			this.terminations = new HashSet<>();  // ridge pixels of terminations
		}
		else if (this.type.equals("bifurcation")) {
			this.anglesBif = new TreeSet<>();
			this.pointpairs = new HashMap<>();
			this.bifurcations = new HashSet<>();  // ridge pixels of bifurcations
			this.neighbours = new HashSet<>();  // for bifurcation - 3 black pixels around minutia point
			this.farPoints = new HashSet<>();    //for bifurcation - the 3 farest pixels around min. 
		}
	}
	
	public boolean isDiffer() {
		return differ;
	}

	public void setDiffer(boolean differ) {
		this.differ = differ;
	}

	
	public Set<Cell> getBifurcations() {
		return bifurcations;
	}

	public void setBifurcations(Set<Cell> bifurcations) {
		this.bifurcations = bifurcations;
	}

	public Set<Cell> getTerminations() {
		return terminations;
	}

	public void setTerminations(Set<Cell> terminations) {
		this.terminations = terminations;
	}

	
	public double getAngleTerm() {
		return angleTerm;
	}

	public void setAngleTerm(double angleTerm) {
		this.angleTerm = angleTerm;
	}
	
	public Cell getNewPoint() {
		return newPoint;
	}

	public void setNewPoint(Cell newPoint) {
		this.newPoint = newPoint;
	}

	public double getAngleBif() {
		return angleBif;
	}

	public void setAngleBif(double angleBif) {
		this.angleBif = angleBif;
	}

}
