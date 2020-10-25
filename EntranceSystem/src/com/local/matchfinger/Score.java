package matchfinger;
class Score {
    static final double thresholdMaxFMR = 8.48;
    static final double thresholdFMR2 = 11.12;
    static final double thresholdFMR10 = 14.15;
    static final double thresholdFMR100 = 18.22;
    static final double thresholdFMR1000 = 22.39;
    static final double thresholdFMR10_000 = 27.24;
    static final double thresholdFMR100_000 = 32.01;
    static final int minSupportingEdges = 1;
    static final double distanceErrorFlatness = 0.69;
    static final double angleErrorFlatness = 0.27;
    static final double pairCountScore = 0.032;
    static final double pairFractionScore = 8.98;
    static final double correctTypeScore = 0.629;
    static final double supportedCountScore = 0.193;
    static final double edgeCountScore = 0.265;
    static final double distanceAccuracyScore = 9.9;
    static final double angleAccuracyScore = 2.79;
    static final int maxDistanceError = 13;
    static final double maxAngleError = Math.toRadians(10);
    
    int matchedMinutiae;
    double matchedMinutiaeScore;
    double matchedFractionOfProbeMinutiae;
    double matchedFractionOfCandidateMinutiae;
    double matchedFractionOfAllMinutiaeScore;
    int matchedEdges;
    double matchedEdgesScore;
    int minutiaeWithSeveralEdges;
    double minutiaeWithSeveralEdgesScore;
    int correctMinutiaTypeCount;
    double correctMinutiaTypeScore;
    double accurateEdgeLengthScore;
    double accurateMinutiaAngleScore;
    double totalScore;
    double shapedScore;
    void compute(MatchBuffer match) {
        matchedMinutiae = match.count;
        matchedMinutiaeScore = pairCountScore * matchedMinutiae;
        matchedFractionOfProbeMinutiae = match.count / (double)match.probe.minutiae.length;
        matchedFractionOfCandidateMinutiae = match.count / (double)match.candidate.minutiae.length;
        matchedFractionOfAllMinutiaeScore = pairFractionScore * (matchedFractionOfProbeMinutiae + matchedFractionOfCandidateMinutiae) / 2;
        matchedEdges = match.count;
        minutiaeWithSeveralEdges = 0;
        correctMinutiaTypeCount = 0;
        for (int i = 0; i < match.count; ++i) {
            MinutiaPair pair = match.tree[i];
            matchedEdges += pair.supportingEdges;
            if (pair.supportingEdges >= minSupportingEdges)
                ++minutiaeWithSeveralEdges;
            if (match.probe.minutiae[pair.probe].type == match.candidate.minutiae[pair.candidate].type)
                ++correctMinutiaTypeCount;
        }
        matchedEdgesScore = edgeCountScore * matchedEdges;
        minutiaeWithSeveralEdgesScore = supportedCountScore * minutiaeWithSeveralEdges;
        correctMinutiaTypeScore = correctTypeScore * correctMinutiaTypeCount;
        int innerDistanceRadius = (int)Math.round(distanceErrorFlatness * maxDistanceError);
        int innerAngleRadius = (int)Math.round(angleErrorFlatness * maxAngleError);
        int distanceErrorSum = 0;
        int angleErrorSum = 0;
        for (int i = 1; i < match.count; ++i) {
            MinutiaPair pair = match.tree[i];
            EdgeShape probeEdge = new EdgeShape(match.probe.minutiae[pair.probeRef], match.probe.minutiae[pair.probe]);
            EdgeShape candidateEdge = new EdgeShape(match.candidate.minutiae[pair.candidateRef], match.candidate.minutiae[pair.candidate]);
            distanceErrorSum += Math.max(innerDistanceRadius, Math.abs(probeEdge.length - candidateEdge.length));
            angleErrorSum += Math.max(innerAngleRadius, Angle.distance(probeEdge.referenceAngle, candidateEdge.referenceAngle));
            angleErrorSum += Math.max(innerAngleRadius, Angle.distance(probeEdge.neighborAngle, candidateEdge.neighborAngle));
        }
        accurateEdgeLengthScore = 0;
        accurateMinutiaAngleScore = 0;
        if (match.count >= 2) {
            double pairedDistanceError = maxDistanceError * (match.count - 1);
            accurateEdgeLengthScore = distanceAccuracyScore * (pairedDistanceError - distanceErrorSum) / pairedDistanceError;
            double pairedAngleError = maxAngleError * (match.count - 1) * 2;
            accurateMinutiaAngleScore = angleAccuracyScore * (pairedAngleError - angleErrorSum) / pairedAngleError;
        }
        totalScore = matchedMinutiaeScore
            + matchedFractionOfAllMinutiaeScore
            + minutiaeWithSeveralEdgesScore
            + matchedEdgesScore
            + correctMinutiaTypeScore
            + accurateEdgeLengthScore
            + accurateMinutiaAngleScore;
        shapedScore = shape(totalScore);
        }
    private static double shape(double raw) {
        if (raw < thresholdMaxFMR)
            return 0;
        if (raw < thresholdFMR2)
            return interpolate(raw, thresholdMaxFMR, thresholdFMR2, 0, 3);
        if (raw < thresholdFMR10)
            return interpolate(raw, thresholdFMR2, thresholdFMR10, 3, 7);
        if (raw < thresholdFMR100)
            return interpolate(raw, thresholdFMR10, thresholdFMR100, 10, 10);
        if (raw < thresholdFMR1000)
            return interpolate(raw, thresholdFMR100, thresholdFMR1000, 20, 10);
        if (raw < thresholdFMR10_000)
            return interpolate(raw, thresholdFMR1000, thresholdFMR10_000, 30, 10);
        if (raw < thresholdFMR100_000)
            return interpolate(raw, thresholdFMR10_000, thresholdFMR100_000, 40, 10);
        return (raw - thresholdFMR100_000) / (thresholdFMR100_000 - thresholdFMR100) * 30 + 50;
    }
    private static double interpolate(double raw, double min, double max, double start, double length) {
        return (raw - min) / (max - min) * length + start;
    }
}
