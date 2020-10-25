package processing;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.opencv.core.Mat;
import org.opencv.core.Size;

public class Histogram {
	public double size;
	public Mat matrix;
	public Map<Double, Integer> histogramMap;
	
	public Histogram(Mat matrix) throws IOException {
		Size size = matrix.size();
		this.size = size.width * size.height;
		this.matrix = matrix;
		Map<Double, Integer> map = pixelFrequency(matrix);
		this.histogramMap = map;
	}
	public Map<Double, Integer> pixelFrequency(Mat input) throws IOException {
		Map<Double, Integer> sorted = new TreeMap<Double, Integer>();
		Map<Double, Integer> unsorted = new TreeMap<Double, Integer>();
		int count = 0;
		// fill the map by all pixel values of image
		for (int i = 0; i < input.height(); i++) {
			for (int j = 0; j < input.width(); j++) {
				double[] dataIn = input.get(i, j);
				unsorted.merge(dataIn[0], 1, Integer::sum);
			}
		}
		sorted = sortByValue(unsorted);  // érték szerint csökkenõbe rendezi, mégsem kell
		return unsorted;
	}

	
	/************************* functions auxiliar ****************************/
	private Map<Double, Integer> sortByValue(Map<Double, Integer> unsortMap) {
		List<Map.Entry<Double, Integer>> list = new LinkedList<Map.Entry<Double, Integer>>(unsortMap.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<Double, Integer>>() {
			public int compare(Map.Entry<Double, Integer> o1, Map.Entry<Double, Integer> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});
		Map<Double, Integer> sortedMap = new LinkedHashMap<Double, Integer>();
		for (Map.Entry<Double, Integer> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}

	public <K, V extends Comparable<? super V>> TreeMap<K, V> rendez(TreeMap<K, V> a) {
		TreeMap<K, V> sortByValue = new TreeMap<>(new Comparator<K>() {
			@Override
			public int compare(K o1, K o2) {
				int compare = a.get(o2).compareTo(a.get(o1));
				if (compare == 0) {
					return 1;
				} else {
					return compare;
				}

			}
		});
		sortByValue.putAll(a);
		return sortByValue;
	}
	
	public void print() {
			int count = 0;
			for (Map.Entry<Double, Integer> m : this.histogramMap.entrySet()) {
				System.out.println(count + ". intensity: " + m.getKey() + " -> frequency: " + m.getValue());
				count++;
			}
		}

}
