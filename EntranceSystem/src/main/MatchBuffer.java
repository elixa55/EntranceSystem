package main;
import java.util.*;


class MatchBuffer {
    static final int maxTriedRoots = 70;
    static final int maxDistanceError = 13;
    static final double maxAngleError = Math.toRadians(10);
    static final int minRootEdgeLength = 58;
    static final int maxRootEdgeLookups = 1633;
    private static final ThreadLocal<MatchBuffer> local = ThreadLocal.withInitial(MatchBuffer::new);
    ImmutableTemplate probe;
    private HashMap<Integer, List<IndexedEdge>> edgeHash;
    ImmutableTemplate candidate;
    private MinutiaPair[] pool = new MinutiaPair[1];
    private int pooled;
    private PriorityQueue<MinutiaPair> queue = new PriorityQueue<>(Comparator.comparing(p -> p.distance));
    int count;
    MinutiaPair[] tree;
    private MinutiaPair[] byProbe;
    private MinutiaPair[] byCandidate;
    private MinutiaPair[] roots;
    private HashSet<Integer> duplicates = new HashSet<>();
    private Score score = new Score();
    static MatchBuffer current() {
        return local.get();
    }
    void selectMatcher(ImmutableMatcher matcher) {
        probe = matcher.template;
        if (tree == null || probe.minutiae.length > tree.length) {
            tree = new MinutiaPair[probe.minutiae.length];
            byProbe = new MinutiaPair[probe.minutiae.length];
        }
        edgeHash = matcher.edgeHash;
    }
    void selectCandidate(ImmutableTemplate template) {
        candidate = template;
        if (byCandidate == null || byCandidate.length < candidate.minutiae.length)
            byCandidate = new MinutiaPair[candidate.minutiae.length];
    }
    double match() {
        try {
            int totalRoots = enumerateRoots();
            double high = 0;
            for (int i = 0; i < totalRoots; ++i) {
                double partial = tryRoot(roots[i]);
                if (partial > high) {
                    high = partial;
                }
                clearPairing();
            }
            return high;
        } catch (Throwable e) {
            local.remove();
            throw e;
        }
    }
    private int enumerateRoots() {
        if (roots == null || roots.length < maxTriedRoots)
            roots = new MinutiaPair[maxTriedRoots];
        int totalLookups = 0;
        int totalRoots = 0;
        int triedRoots = 0;
        duplicates.clear();
        for (boolean shortEdges : new boolean[] { false, true }) {
            for (int period = 1; period < candidate.minutiae.length; ++period) {
                for (int phase = 0; phase <= period; ++phase) {
                    for (int candidateReference = phase; candidateReference < candidate.minutiae.length; candidateReference += period + 1) {
                        int candidateNeighbor = (candidateReference + period) % candidate.minutiae.length;
                        EdgeShape candidateEdge = new EdgeShape(candidate.minutiae[candidateReference], candidate.minutiae[candidateNeighbor]);
                        if ((candidateEdge.length >= minRootEdgeLength) ^ shortEdges) {
                            List<IndexedEdge> matches = edgeHash.get(hashShape(candidateEdge));
                            if (matches != null) {
                                for (IndexedEdge match : matches) {
                                    if (matchingShapes(match, candidateEdge)) {
                                        int duplicateKey = (match.reference << 16) | candidateReference;
                                        if (!duplicates.contains(duplicateKey)) {
                                            duplicates.add(duplicateKey);
                                            MinutiaPair pair = allocate();
                                            pair.probe = match.reference;
                                            pair.candidate = candidateReference;
                                            roots[totalRoots] = pair;
                                            ++totalRoots;
                                        }
                                        ++triedRoots;
                                        if (triedRoots >= maxTriedRoots)
                                            return totalRoots;
                                    }
                                }
                            }
                            ++totalLookups;
                            if (totalLookups >= maxRootEdgeLookups)
                                return totalRoots;
                        }
                    }
                }
            }
        }
        return totalRoots;
    }
    private int hashShape(EdgeShape edge) {
        int lengthBin = edge.length / maxDistanceError;
        int referenceAngleBin = (int)(edge.referenceAngle / maxAngleError);
        int neighborAngleBin = (int)(edge.neighborAngle / maxAngleError);
        return (referenceAngleBin << 24) + (neighborAngleBin << 16) + lengthBin;
    }
    private boolean matchingShapes(EdgeShape probe, EdgeShape candidate) {
        int lengthDelta = probe.length - candidate.length;
        if (lengthDelta >= -maxDistanceError && lengthDelta <= maxDistanceError) {
            double complementaryAngleError = Angle.complementary(maxAngleError);
            double referenceDelta = Angle.difference(probe.referenceAngle, candidate.referenceAngle);
            if (referenceDelta <= maxAngleError || referenceDelta >= complementaryAngleError) {
                double neighborDelta = Angle.difference(probe.neighborAngle, candidate.neighborAngle);
                if (neighborDelta <= maxAngleError || neighborDelta >= complementaryAngleError)
                    return true;
            }
        }
        return false;
    }
    private double tryRoot(MinutiaPair root) {
        queue.add(root);
        do {
            addPair(queue.remove());
            collectEdges();
            skipPaired();
        } while (!queue.isEmpty());
        score.compute(this);
        return score.shapedScore;
    }
    private void clearPairing() {
        for (int i = 0; i < count; ++i) {
            byProbe[tree[i].probe] = null;
            byCandidate[tree[i].candidate] = null;
            release(tree[i]);
            tree[i] = null;
        }
        count = 0;
    }
    private void collectEdges() {
        MinutiaPair reference = tree[count - 1];
        NeighborEdge[] probeNeighbors = probe.edges[reference.probe];
        NeighborEdge[] candidateNeigbors = candidate.edges[reference.candidate];
        for (MinutiaPair pair : matchPairs(probeNeighbors, candidateNeigbors)) {
            pair.probeRef = reference.probe;
            pair.candidateRef = reference.candidate;
            if (byCandidate[pair.candidate] == null && byProbe[pair.probe] == null)
                queue.add(pair);
            else {
                if (byProbe[pair.probe] != null && byProbe[pair.probe].candidate == pair.candidate)
                    addSupportingEdge(pair);
                release(pair);
            }
        }
    }
    private List<MinutiaPair> matchPairs(NeighborEdge[] probeStar, NeighborEdge[] candidateStar) {
        double complementaryAngleError = Angle.complementary(maxAngleError);
        List<MinutiaPair> results = new ArrayList<>();
        int start = 0;
        int end = 0;
        for (int candidateIndex = 0; candidateIndex < candidateStar.length; ++candidateIndex) {
            NeighborEdge candidateEdge = candidateStar[candidateIndex];
            while (start < probeStar.length && probeStar[start].length < candidateEdge.length - maxDistanceError)
                ++start;
            if (end < start)
                end = start;
            while (end < probeStar.length && probeStar[end].length <= candidateEdge.length + maxDistanceError)
                ++end;
            for (int probeIndex = start; probeIndex < end; ++probeIndex) {
                NeighborEdge probeEdge = probeStar[probeIndex];
                double referenceDiff = Angle.difference(probeEdge.referenceAngle, candidateEdge.referenceAngle);
                if (referenceDiff <= maxAngleError || referenceDiff >= complementaryAngleError) {
                    double neighborDiff = Angle.difference(probeEdge.neighborAngle, candidateEdge.neighborAngle);
                    if (neighborDiff <= maxAngleError || neighborDiff >= complementaryAngleError) {
                        MinutiaPair pair = allocate();
                        pair.probe = probeEdge.neighbor;
                        pair.candidate = candidateEdge.neighbor;
                        pair.distance = candidateEdge.length;
                        results.add(pair);
                    }
                }
            }
        }
        return results;
    }
    private void skipPaired() {
        while (!queue.isEmpty() && (byProbe[queue.peek().probe] != null || byCandidate[queue.peek().candidate] != null)) {
            MinutiaPair pair = queue.remove();
            if (byProbe[pair.probe] != null && byProbe[pair.probe].candidate == pair.candidate)
                addSupportingEdge(pair);
            release(pair);
        }
    }
    private void addPair(MinutiaPair pair) {
        tree[count] = pair;
        byProbe[pair.probe] = pair;
        byCandidate[pair.candidate] = pair;
        ++count;
    }
    private void addSupportingEdge(MinutiaPair pair) {
        ++byProbe[pair.probe].supportingEdges;
        ++byProbe[pair.probeRef].supportingEdges;
    }
    private MinutiaPair allocate() {
        if (pooled > 0) {
            --pooled;
            MinutiaPair pair = pool[pooled];
            pool[pooled] = null;
            return pair;
        } else
            return new MinutiaPair();
    }
    private void release(MinutiaPair pair) {
        if (pooled >= pool.length)
            pool = Arrays.copyOf(pool, 2 * pool.length);
        pair.probe = 0;
        pair.candidate = 0;
        pair.probeRef = 0;
        pair.candidateRef = 0;
        pair.distance = 0;
        pair.supportingEdges = 0;
        pool[pooled] = pair;
    }
}
