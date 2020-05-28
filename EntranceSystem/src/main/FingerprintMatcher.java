package main;
import java.util.*;

public class FingerprintMatcher {
    private final int maxDistanceError = 13;
    private final double maxAngleError = Math.toRadians(10);
     private volatile ImmutableMatcher immutable = ImmutableMatcher.empty;

    public FingerprintMatcher() {
    }


    public FingerprintMatcher index(FingerprintTemplate probe) {
        ImmutableTemplate template = probe.immutable;
        immutable = new ImmutableMatcher(template, buildEdgeHash(template));
        return this;
    }
    private HashMap<Integer, List<IndexedEdge>> buildEdgeHash(ImmutableTemplate template) {
        HashMap<Integer, List<IndexedEdge>> map = new HashMap<>();
        for (int reference = 0; reference < template.minutiae.length; ++reference)
            for (int neighbor = 0; neighbor < template.minutiae.length; ++neighbor)
                if (reference != neighbor) {
                    IndexedEdge edge = new IndexedEdge(template.minutiae, reference, neighbor);
                    for (int hash : shapeCoverage(edge)) {
                        List<IndexedEdge> list = map.get(hash);
                        if (list == null)
                            map.put(hash, list = new ArrayList<>());
                        list.add(edge);
                    }
                }
        return map;
    }
    private List<Integer> shapeCoverage(EdgeShape edge) {
        int minLengthBin = (edge.length - maxDistanceError) / maxDistanceError;
        int maxLengthBin = (edge.length + maxDistanceError) / maxDistanceError;
        int angleBins = (int)Math.ceil(2 * Math.PI / maxAngleError);
        int minReferenceBin = (int)(Angle.difference(edge.referenceAngle, maxAngleError) / maxAngleError);
        int maxReferenceBin = (int)(Angle.add(edge.referenceAngle, maxAngleError) / maxAngleError);
        int endReferenceBin = (maxReferenceBin + 1) % angleBins;
        int minNeighborBin = (int)(Angle.difference(edge.neighborAngle, maxAngleError) / maxAngleError);
        int maxNeighborBin = (int)(Angle.add(edge.neighborAngle, maxAngleError) / maxAngleError);
        int endNeighborBin = (maxNeighborBin + 1) % angleBins;
        List<Integer> coverage = new ArrayList<>();
        for (int lengthBin = minLengthBin; lengthBin <= maxLengthBin; ++lengthBin)
            for (int referenceBin = minReferenceBin; referenceBin != endReferenceBin; referenceBin = (referenceBin + 1) % angleBins)
                for (int neighborBin = minNeighborBin; neighborBin != endNeighborBin; neighborBin = (neighborBin + 1) % angleBins)
                    coverage.add((referenceBin << 24) + (neighborBin << 16) + lengthBin);
        return coverage;
    }
  
    public double match(FingerprintTemplate candidate) {
        MatchBuffer buffer = MatchBuffer.current();
        try {
            buffer.selectMatcher(immutable);
            buffer.selectCandidate(candidate.immutable);
            return buffer.match();
        } finally {

        }
    }
}
