package processing;

import java.util.Map;

public class Block {

	private double orientationAngle;
	private double coherence;
	private double mean;
	private double variance;
	private boolean darkness;
	private Map<Double, Integer> histogramMap;
	private boolean isBackground;
	

	/** constructors
	 * @param mean
	 * @param variance
	 */
	public Block(double mean, double variance) {
		this.mean = mean;
		this.variance = variance;
	}
	
	public Block(double mean, double variance, double orientAngle, double coherence, boolean darkness) {
		this.mean = mean;
		this.variance = variance;
		this.orientationAngle = orientAngle;
		this.coherence = coherence;
		this.darkness = darkness;
	}

	/**getters- setters
	 * @return
	 */
	public boolean isBackground() {
		return isBackground;
	}
	
	public void setBackground(boolean isBackground) {
		this.isBackground = isBackground;
	}
	
	public Map<Double, Integer> getHistogramMap() {
		return histogramMap;
	}

	public void setHistogramMap(Map<Double, Integer> histogramMap) {
		this.histogramMap = histogramMap;
	}
	
	public double getOrientationAngle() {
		return orientationAngle;
	}
	public void setOrientationAngle(double orientationAngle) {
		this.orientationAngle = orientationAngle;
	}
	public double getCoherence() {
		return coherence;
	}
	public void setCoherence(double coherence) {
		this.coherence = coherence;
	}
	public double getMean() {
		return mean;
	}
	public void setMean(double mean) {
		this.mean = mean;
	}
	public double getVariance() {
		return variance;
	}
	public void setVariance(double variance) {
		this.variance = variance;
	}
	public boolean isDarkness() {
		return darkness;
	}
	public void setDarkness(boolean darkness) {
		this.darkness = darkness;
	}

	

}
