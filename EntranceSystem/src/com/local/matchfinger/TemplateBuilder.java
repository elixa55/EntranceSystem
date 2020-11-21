package matchfinger;

import java.util.*;

public class TemplateBuilder {

    static final int binarizedVoteBorderDistance = 17;
    static final int edgeTableNeighbors = 9;
    static final int edgeTableRange = 490;
    static final int binarizedVoteRadius = 2;
    static final double binarizedVoteMajority = 0.61;
    static final double maskDisplacement = 10.06;
    Cell size;
    Minutia[] minutiae;
    NeighborEdge[][] edges;

    public void deserialize(String templateSerialized) {
        String[] parts = templateSerialized.split(" ");
        int widthImage = Integer.parseInt(parts[0].replace("width:", ""));
        int heightImage = Integer.parseInt(parts[1].replace("height:", ""));
        String[] minutiaParts = parts[2].split(";");
        Minutia[] minutiaArray = new Minutia[minutiaParts.length];
        for (int i = 0; i < minutiaParts.length; i++) {
            String[] split = minutiaParts[i].split("#");
            Minutia minutia;
            if (split[3].equals("bifurcation")) {
                minutia = new Minutia(new Cell(Integer.parseInt(split[0]), Integer.parseInt(split[1])),
                        Double.parseDouble(split[2]), MinutiaType.BIFURCATION);
            } else {
                minutia = new Minutia(new Cell(Integer.parseInt(split[0]), Integer.parseInt(split[1])),
                        Double.parseDouble(split[2]), MinutiaType.ENDING);
            }
            minutiaArray[i] = minutia;
        }
        size = new Cell(widthImage, heightImage);
        minutiae = minutiaArray;
        buildEdgeTable();
    }

    private void buildEdgeTable() {
        edges = new NeighborEdge[minutiae.length][];
        List<NeighborEdge> star = new ArrayList<>();
        int[] allSqDistances = new int[minutiae.length];
        for (int reference = 0; reference < edges.length; ++reference) {
            Cell referencePosition = minutiae[reference].position;
            int sqMaxDistance = edgeTableRange*edgeTableRange;
            if (minutiae.length - 1 > edgeTableNeighbors) {
                for (int neighbor = 0; neighbor < minutiae.length; ++neighbor)
                    allSqDistances[neighbor] = referencePosition.minus(minutiae[neighbor].position).lengthSq();
                Arrays.sort(allSqDistances);
                sqMaxDistance = allSqDistances[edgeTableNeighbors];
            }
            for (int neighbor = 0; neighbor < minutiae.length; ++neighbor) {
                if (neighbor != reference
                        && referencePosition.minus(minutiae[neighbor].position).lengthSq() <= sqMaxDistance)
                    star.add(new NeighborEdge(minutiae, reference, neighbor));
            }
            star.sort(Comparator.<NeighborEdge>comparingInt(e -> e.length).thenComparingInt(e -> e.neighbor));
            while (star.size() > edgeTableNeighbors)
                star.remove(star.size() - 1);
            edges[reference] = star.toArray(new NeighborEdge[star.size()]);
            star.clear();
        }
    }
}
