package main;

import static java.util.stream.Collectors.*;
import java.util.*;

class TemplateData {
	int width;
	int height;
	List<MinutiaData> minutiae;
	TemplateData(Cell size, Minutia[] minutiae) {
		width = size.x;
		height = size.y;
		this.minutiae = Arrays.stream(minutiae).map(MinutiaData::new).collect(toList());
	}
	Cell size() {
		return new Cell(width, height);
	}
	Minutia[] minutiae() {
		return minutiae.stream().map(Minutia::new).toArray(n -> new Minutia[n]);
	}
}
