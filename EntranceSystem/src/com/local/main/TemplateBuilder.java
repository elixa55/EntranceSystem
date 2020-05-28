package main;

import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;
import javax.imageio.*;

class TemplateBuilder {
    static final int parallelSmoothinigResolution = 32;
    static final int orthogonalSmoothinigResolution = 11;
    static final int blockSize = 15;
    static final int histogramDepth = 16;
    static final int parallelSmoothinigRadius = 7;
    static final int orthogonalSmoothinigRadius = 4;
    static final double parallelSmoothinigStep = 1.59;
    static final double orthogonalSmoothingStep = 1.11;
    static final int binarizedVoteBorderDistance = 17;
    static final int innerMaskBorderDistance = 14;
    static final int edgeTableNeighbors = 9;
    static final int edgeTableRange = 490;
    static final int binarizedVoteRadius = 2;
    static final double binarizedVoteMajority = 0.61;
    static final double maskDisplacement = 10.06;
    static final int orientationSmoothingRadius = 1;
    static final double minOrientationRadius = 2;
    static final double maxOrientationRadius = 6;
    static final int orientationSplit = 50;
    static final int orientationsChecked = 20;
    static final int blockErrorsVoteRadius = 1;
    static final double blockErrorsVoteMajority = 0.7;
    static final int blockErrorsVoteBorderDistance = 4;
    static final double maxEqualizationScaling = 3.99;
    static final double minEqualizationScaling = 0.25;
    static final double minAbsoluteContrast = 17 / 255.0;
    static final double minRelativeContrast = 0.34;
    static final double clippedContrast = 0.08;
    static final int relativeContrastSample = 168568;
    static final double relativeContrastPercentile = 0.49;
    static final int contrastVoteRadius = 9;
    static final double contrastVoteMajority = 0.86;
    static final int contrastVoteBorderDistance = 7;
    static final int maskVoteRadius = 7;
    static final double maskVoteMajority = 0.51;
    static final int maskVoteBorderDistance = 4;
    
    Cell size;
    Minutia[] minutiae;
    NeighborEdge[][] edges;

    public static String kiiratDoubleMap(DoubleMap dm) {
        StringBuilder sb = new StringBuilder();
        for (int j=0;j<dm.height;j++) {;
            for (int i=0;i<dm.width;i++)
            {
                sb.append(String.format("%.4f", dm.get(i, j)));
                sb.append('\t');
            }
            sb.append('\n');
        }
        return sb.toString();
    }
    
    public static String kiiratIntMap(IntMap im) {
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (int j=0;j<im.height;j++) {;
            for (int i=0;i<im.width;i++)
            {
                int temp = im.get(i, j);
                sb2.append(String.format("%9d", temp));
                switch(temp) {
                case -16777216 : sb1.append(String.format("%d", 0)); break;
                case -12105913 : sb1.append(String.format("%d", 1)); break;
                case -10263709 : sb1.append(String.format("%d", 2)); break;
                case  -8882056 : sb1.append(String.format("%d", 3)); break;
                case  -7763575 : sb1.append(String.format("%d", 4)); break;
                case  -6776680 : sb1.append(String.format("%d", 5)); break;
                case  -5921371 : sb1.append(String.format("%d", 6)); break;
                case  -5131855 : sb1.append(String.format("%d", 7)); break;
                case  -4408132 : sb1.append(String.format("%d", 8)); break;
                case  -3750202 : sb1.append(String.format("%d", 9)); break;
                case  -3092272 : sb1.append(String.format("%d", 10)); break;
                case  -2565928 : sb1.append(String.format("%d", 11)); break;
                case  -1973791 : sb1.append(String.format("%d", 12)); break;
                case  -1447447 : sb1.append(String.format("%d", 13)); break;
                case   -921103 : sb1.append(String.format("%d", 14)); break;
                case   -460552 : sb1.append(String.format("%d", 15)); break;
                default: sb1.append(String.format("%d", im.get(i, j)));
                }
                sb1.append('\t');
                sb2.append('\t');
            }
            sb1.append('\n');
            sb2.append('\n');
        }
        writeFile("integermap.txt", sb2.toString());
        return sb1.toString();
    }    
    
    public static void writeFile(String whereTo, String what) {
        BufferedWriter bw;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(whereTo)));
            bw.write(what);
            bw.flush();
            bw.close();
            System.out.println("Txt file with bitmap is done.");
        } catch (IOException e) {
            e.getMessage();
        }
    }
    
    void extract(byte[] image)  {
        try {
        DoubleMap raw = decodeImage(image);
        String s = kiiratDoubleMap(raw);
        writeFile("doublemap.txt", s);
        size = raw.size(); // always size.x = 256 size.y = 288
        BlockMap blocks = new BlockMap(raw.width, raw.height, blockSize);  // maxblockSize=15
        Histogram histogram = histogram(blocks, raw);
        Histogram smoothHistogram = smoothHistogram(blocks, histogram);
        BooleanMap mask = mask(blocks, histogram);
        DoubleMap equalized = equalize(blocks, raw, smoothHistogram, mask);
        DoubleMap orientation = orientationMap(equalized, mask, blocks);
        Cell[][] smoothedLines = orientedLines(parallelSmoothinigResolution, parallelSmoothinigRadius,
                parallelSmoothinigStep);
        DoubleMap smoothed = smoothRidges(equalized, orientation, mask, blocks, 0, smoothedLines);
        Cell[][] orthogonalLines = orientedLines(orthogonalSmoothinigResolution, orthogonalSmoothinigRadius,
                orthogonalSmoothingStep);
        DoubleMap orthogonal = smoothRidges(smoothed, orientation, mask, blocks, Math.PI, orthogonalLines);
        BooleanMap binary = binarize(smoothed, orthogonal, mask, blocks);
        BooleanMap pixelMask = fillBlocks(mask, blocks);
        cleanupBinarized(binary, pixelMask); // 94-82-re csökken
        BooleanMap inverted = invert(binary, pixelMask);
        BooleanMap innerMask = innerMask(pixelMask);
        Skeleton ridges = new Skeleton(binary, SkeletonType.RIDGES);
        Skeleton valleys = new Skeleton(inverted, SkeletonType.VALLEYS);
        collectMinutiae(ridges, MinutiaType.ENDING);
        collectMinutiae(valleys, MinutiaType.BIFURCATION);
        maskMinutiae(innerMask); // 94-ről 62-re, cleanup nélkül 48
        buildEdgeTable(); 
        }
        catch (IOException ex)
        {
            System.out.println("Image extracting error");
        }
    }

    void deserialize(String templateSerialized) {
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
        TemplateData templateData = new TemplateData(new Cell(widthImage, heightImage), minutiaArray);
        size = templateData.size();
        minutiae = templateData.minutiae();
        buildEdgeTable();
    }


    private static DoubleMap decodeImage(byte [] serialized) throws IOException {
        BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(serialized));
        if (buffered == null)
            return null;
        else {
        int width = buffered.getWidth();
        int height = buffered.getHeight();
        int maxColorValue = -16777216;
        int[] pixels = new int[width * height];
        buffered.getRGB(0, 0, width, height, pixels, 0, width);
        DoubleMap map = new DoubleMap(width, height);
        IntMap intMap = new IntMap(width, height);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int pixel = pixels[y * width + x]; // adott pozicion levo pixel egesz erteke 16 kozul
                //int color = (pixel & 0xff) + ((pixel >> 8) & 0xff) + ((pixel >> 16) & 0xff);
                intMap.set(x,  y, pixel);
                //map.set(x, y, 1 - color * (1.0 / (3.0 * 255.0)));
                double value = (double) pixel / maxColorValue;
                map.set(x,  y, value);
            }
        }
        String s = kiiratIntMap(intMap);
        writeFile("intmap.txt", s);
        return map;
        }
    }

    private Histogram histogram(BlockMap blocks, DoubleMap image) {
        StringBuilder  sb = new StringBuilder();
        Histogram histogram = new Histogram(blocks.primary.blocks, histogramDepth);  // histogramDepth=256
        for (Cell block : blocks.primary.blocks) {
            Block area = blocks.primary.block(block);
            for (int y = area.top(); y < area.bottom(); ++y)
                for (int x = area.left(); x < area.right(); ++x) {
                    int depth = (int) (image.get(x, y) * histogram.depth);
                    histogram.increment(block, histogram.constrain(depth));
                }
        }
        for (Cell block : blocks.primary.blocks) {
            Block area = blocks.primary.block(block);
            for (int y = area.top(); y < area.bottom(); ++y) {
                for (int x = area.left(); x < area.right(); ++x) {
                    int depth = (int) (image.get(x, y) * histogram.depth);
                    int con = (int) histogram.constrain(depth);
                    sb.append(String.format("x:%d\ty:%d\td:%d\tc:%d\t", x, y, depth, con));
                }
                sb.append("histi: x= " + block.x + " y= " +block.y + " value= ");
            sb.append('\n');
            }
        }
        writeFile("histogram.txt", sb.toString());
        return histogram;
    }

    private Histogram smoothHistogram(BlockMap blocks, Histogram input) {
        Cell[] blocksAround = new Cell[] { new Cell(0, 0), new Cell(-1, 0), new Cell(0, -1), new Cell(-1, -1) };
        Histogram output = new Histogram(blocks.secondary.blocks, input.depth);
        for (Cell corner : blocks.secondary.blocks) {
            for (Cell relative : blocksAround) {
                Cell block = corner.plus(relative);
                if (blocks.primary.blocks.contains(block)) {
                    for (int i = 0; i < input.depth; ++i)
                        output.add(corner, i, input.get(block, i));
                }
            }
        }
        return output;
    }

    private BooleanMap mask(BlockMap blocks, Histogram histogram) {
        DoubleMap contrast = clipContrast(blocks, histogram);
        BooleanMap mask = filterAbsoluteContrast(contrast);
        mask.merge(filterRelativeContrast(contrast, blocks));
        mask.merge(vote(mask, null, contrastVoteRadius, contrastVoteMajority, contrastVoteBorderDistance));
        mask.merge(filterBlockErrors(mask));
        mask.invert();
        mask.merge(filterBlockErrors(mask));
        mask.merge(filterBlockErrors(mask));
        mask.merge(vote(mask, null, maskVoteRadius, maskVoteMajority, maskVoteBorderDistance));
        return mask;
    }

    private DoubleMap clipContrast(BlockMap blocks, Histogram histogram) {
        DoubleMap result = new DoubleMap(blocks.primary.blocks);
        for (Cell block : blocks.primary.blocks) {
            int volume = histogram.sum(block);
            int clipLimit = (int) Math.round(volume * clippedContrast);
            int accumulator = 0;
            int lowerBound = histogram.depth - 1;
            for (int i = 0; i < histogram.depth; ++i) {
                accumulator += histogram.get(block, i);
                if (accumulator > clipLimit) {
                    lowerBound = i;
                    break;
                }
            }
            accumulator = 0;
            int upperBound = 0;
            for (int i = histogram.depth - 1; i >= 0; --i) {
                accumulator += histogram.get(block, i);
                if (accumulator > clipLimit) {
                    upperBound = i;
                    break;
                }
            }
            result.set(block, (upperBound - lowerBound) * (1.0 / (histogram.depth - 1)));
        }
        return result;
    }

    private BooleanMap filterAbsoluteContrast(DoubleMap contrast) {
        BooleanMap result = new BooleanMap(contrast.size());
        for (Cell block : contrast.size())
            if (contrast.get(block) < minAbsoluteContrast)
                result.set(block, true);
        return result;
    }

    private BooleanMap filterRelativeContrast(DoubleMap contrast, BlockMap blocks) {
        List<Double> sortedContrast = new ArrayList<>();
        for (Cell block : contrast.size())
            sortedContrast.add(contrast.get(block));
        sortedContrast.sort(Comparator.<Double>naturalOrder().reversed());
        int pixelsPerBlock = blocks.pixels.area() / blocks.primary.blocks.area();
        int sampleCount = Math.min(sortedContrast.size(), relativeContrastSample / pixelsPerBlock);
        int consideredBlocks = Math.max((int) Math.round(sampleCount * relativeContrastPercentile), 1);
        double averageContrast = sortedContrast.stream().mapToDouble(n -> n).limit(consideredBlocks).average()
                .getAsDouble();
        double limit = averageContrast * minRelativeContrast;
        BooleanMap result = new BooleanMap(blocks.primary.blocks);
        for (Cell block : blocks.primary.blocks)
            if (contrast.get(block) < limit)
                result.set(block, true);
        return result;
    }

    private BooleanMap vote(BooleanMap input, BooleanMap mask, int radius, double majority, int borderDistance) {
        Cell size = input.size();
        Block rect = new Block(borderDistance, borderDistance, size.x - 2 * borderDistance,
                size.y - 2 * borderDistance);
        int[] thresholds = IntStream.range(0, ((2 * radius + 1)*(2 * radius + 1)) + 1).map(i -> (int) Math.ceil(majority * i))
                .toArray();
        IntMap counts = new IntMap(size);
        BooleanMap output = new BooleanMap(size);
        for (int y = rect.top(); y < rect.bottom(); ++y) {
            int superTop = y - radius - 1;
            int superBottom = y + radius;
            int yMin = Math.max(0, y - radius);
            int yMax = Math.min(size.y - 1, y + radius);
            int yRange = yMax - yMin + 1;
            for (int x = rect.left(); x < rect.right(); ++x)
                if (mask == null || mask.get(x, y)) {
                    int left = x > 0 ? counts.get(x - 1, y) : 0;
                    int top = y > 0 ? counts.get(x, y - 1) : 0;
                    int diagonal = x > 0 && y > 0 ? counts.get(x - 1, y - 1) : 0;
                    int xMin = Math.max(0, x - radius);
                    int xMax = Math.min(size.x - 1, x + radius);
                    int ones;
                    if (left > 0 && top > 0 && diagonal > 0) {
                        ones = top + left - diagonal - 1;
                        int superLeft = x - radius - 1;
                        int superRight = x + radius;
                        if (superLeft >= 0 && superTop >= 0 && input.get(superLeft, superTop))
                            ++ones;
                        if (superLeft >= 0 && superBottom < size.y && input.get(superLeft, superBottom))
                            --ones;
                        if (superRight < size.x && superTop >= 0 && input.get(superRight, superTop))
                            --ones;
                        if (superRight < size.x && superBottom < size.y && input.get(superRight, superBottom))
                            ++ones;
                    } else {
                        ones = 0;
                        for (int ny = yMin; ny <= yMax; ++ny)
                            for (int nx = xMin; nx <= xMax; ++nx)
                                if (input.get(nx, ny))
                                    ++ones;
                    }
                    counts.set(x, y, ones + 1);
                    if (ones >= thresholds[yRange * (xMax - xMin + 1)])
                        output.set(x, y, true);
                }
        }
        return output;
    }

    private BooleanMap filterBlockErrors(BooleanMap input) {
        return vote(input, null, blockErrorsVoteRadius, blockErrorsVoteMajority, blockErrorsVoteBorderDistance);
    }

    private DoubleMap equalize(BlockMap blocks, DoubleMap image, Histogram histogram, BooleanMap blockMask) {
        final double rangeMin = -1;
        final double rangeMax = 1;
        final double rangeSize = rangeMax - rangeMin;
        final double widthMax = rangeSize / 256 * maxEqualizationScaling;
        final double widthMin = rangeSize / 256 * minEqualizationScaling;
        double[] limitedMin = new double[histogram.depth];
        double[] limitedMax = new double[histogram.depth];
        double[] dequantized = new double[histogram.depth];
        for (int i = 0; i < histogram.depth; ++i) {
            limitedMin[i] = Math.max(i * widthMin + rangeMin, rangeMax - (histogram.depth - 1 - i) * widthMax);
            limitedMax[i] = Math.min(i * widthMax + rangeMin, rangeMax - (histogram.depth - 1 - i) * widthMin);
            dequantized[i] = i / (double) (histogram.depth - 1);
        }
        Map<Cell, double[]> mappings = new HashMap<>();
        for (Cell corner : blocks.secondary.blocks) {
            double[] mapping = new double[histogram.depth];
            mappings.put(corner, mapping);
            if (blockMask.get(corner, false) || blockMask.get(corner.x - 1, corner.y, false)
                    || blockMask.get(corner.x, corner.y - 1, false)
                    || blockMask.get(corner.x - 1, corner.y - 1, false)) {
                double step = rangeSize / histogram.sum(corner);
                double top = rangeMin;
                for (int i = 0; i < histogram.depth; ++i) {
                    double band = histogram.get(corner, i) * step;
                    double equalized = top + dequantized[i] * band;
                    top += band;
                    if (equalized < limitedMin[i])
                        equalized = limitedMin[i];
                    if (equalized > limitedMax[i])
                        equalized = limitedMax[i];
                    mapping[i] = equalized;
                }
            }
        }
        DoubleMap result = new DoubleMap(blocks.pixels);
        for (Cell block : blocks.primary.blocks) {
            Block area = blocks.primary.block(block);
            if (blockMask.get(block)) {
                double[] topleft = mappings.get(block);
                double[] topright = mappings.get(new Cell(block.x + 1, block.y));
                double[] bottomleft = mappings.get(new Cell(block.x, block.y + 1));
                double[] bottomright = mappings.get(new Cell(block.x + 1, block.y + 1));
                for (int y = area.top(); y < area.bottom(); ++y)
                    for (int x = area.left(); x < area.right(); ++x) {
                        int depth = histogram.constrain((int) (image.get(x, y) * histogram.depth));
                        double rx = (x - area.x + 0.5) / area.width;
                        double ry = (y - area.y + 0.5) / area.height;
                        result.set(x, y, Doubles.interpolate(bottomleft[depth], bottomright[depth], topleft[depth],
                                topright[depth], rx, ry));
                    }
            } else {
                for (int y = area.top(); y < area.bottom(); ++y)
                    for (int x = area.left(); x < area.right(); ++x)
                        result.set(x, y, -1);
            }
        }
        return result;
    }

    private DoubleMap orientationMap(DoubleMap image, BooleanMap mask, BlockMap blocks) {
        PointMap accumulated = pixelwiseOrientation(image, mask, blocks);
        PointMap byBlock = blockOrientations(accumulated, blocks, mask);
        PointMap smooth = smoothOrientation(byBlock, mask);
        return orientationAngles(smooth, mask);
    }

    private static class ConsideredOrientation {
        Cell offset;
        Point orientation;
    }

    private static class OrientationRandom {
        static final int prime = 1610612741;
        static final int bits = 30;
        static final int mask = (1 << bits) - 1;
        static final double scaling = 1.0 / (1 << bits);
        long state = prime * prime * prime;

        double next() {
            state *= prime;
            return ((state & mask) + 0.5) * scaling;
        }
    }

    private ConsideredOrientation[][] planOrientations() {
        OrientationRandom random = new OrientationRandom();
        ConsideredOrientation[][] splits = new ConsideredOrientation[orientationSplit][];
        for (int i = 0; i < orientationSplit; ++i) {
            ConsideredOrientation[] orientations = splits[i] = new ConsideredOrientation[orientationsChecked];
            for (int j = 0; j < orientationsChecked; ++j) {
                ConsideredOrientation sample = orientations[j] = new ConsideredOrientation();
                do {
                    double angle = random.next() * Math.PI;
                    double distance = Doubles.interpolateExponential(minOrientationRadius, maxOrientationRadius,
                            random.next());
                    sample.offset = Angle.toVector(angle).multiply(distance).round();
                } while (sample.offset.equals(Cell.zero) || sample.offset.y < 0
                        || Arrays.stream(orientations).limit(j).anyMatch(o -> o.offset.equals(sample.offset)));
                sample.orientation = Angle
                        .toVector(Angle.add(Angle.toOrientation(Angle.atan(sample.offset.toPoint())), Math.PI));
            }
        }
        return splits;
    }

    private PointMap pixelwiseOrientation(DoubleMap input, BooleanMap mask, BlockMap blocks) {
        ConsideredOrientation[][] neighbors = planOrientations();
        PointMap orientation = new PointMap(input.size());
        for (int blockY = 0; blockY < blocks.primary.blocks.y; ++blockY) {
            Range maskRange = maskRange(mask, blockY);
            if (maskRange.length() > 0) {
                Range validXRange = new Range(blocks.primary.block(maskRange.start, blockY).left(),
                        blocks.primary.block(maskRange.end - 1, blockY).right());
                for (int y = blocks.primary.block(0, blockY).top(); y < blocks.primary.block(0, blockY).bottom(); ++y) {
                    for (ConsideredOrientation neighbor : neighbors[y % neighbors.length]) {
                        int radius = Math.max(Math.abs(neighbor.offset.x), Math.abs(neighbor.offset.y));
                        if (y - radius >= 0 && y + radius < input.height) {
                            Range xRange = new Range(Math.max(radius, validXRange.start),
                                    Math.min(input.width - radius, validXRange.end));
                            for (int x = xRange.start; x < xRange.end; ++x) {
                                double before = input.get(x - neighbor.offset.x, y - neighbor.offset.y);
                                double at = input.get(x, y);
                                double after = input.get(x + neighbor.offset.x, y + neighbor.offset.y);
                                double strength = at - Math.max(before, after);
                                if (strength > 0)
                                    orientation.add(x, y, neighbor.orientation.multiply(strength));
                            }
                        }
                    }
                }
            }
        }
        return orientation;
    }

    private static Range maskRange(BooleanMap mask, int y) {
        int first = -1;
        int last = -1;
        for (int x = 0; x < mask.width; ++x)
            if (mask.get(x, y)) {
                last = x;
                if (first < 0)
                    first = x;
            }
        if (first >= 0)
            return new Range(first, last + 1);
        else
            return Range.zero;
    }

    private PointMap blockOrientations(PointMap orientation, BlockMap blocks, BooleanMap mask) {
        PointMap sums = new PointMap(blocks.primary.blocks);
        for (Cell block : blocks.primary.blocks) {
            if (mask.get(block)) {
                Block area = blocks.primary.block(block);
                for (int y = area.top(); y < area.bottom(); ++y)
                    for (int x = area.left(); x < area.right(); ++x)
                        sums.add(block, orientation.get(x, y));
            }
        }
        return sums;
    }

    private PointMap smoothOrientation(PointMap orientation, BooleanMap mask) {
        Cell size = mask.size();
        PointMap smoothed = new PointMap(size);
        for (Cell block : size)
            if (mask.get(block)) {
                Block neighbors = Block.around(block, orientationSmoothingRadius).intersect(new Block(size));
                for (int ny = neighbors.top(); ny < neighbors.bottom(); ++ny)
                    for (int nx = neighbors.left(); nx < neighbors.right(); ++nx)
                        if (mask.get(nx, ny))
                            smoothed.add(block, orientation.get(nx, ny));
            }
        return smoothed;
    }

    private static DoubleMap orientationAngles(PointMap vectors, BooleanMap mask) {
        Cell size = mask.size();
        DoubleMap angles = new DoubleMap(size);
        for (Cell block : size)
            if (mask.get(block))
                angles.set(block, Angle.atan(vectors.get(block)));
        return angles;
    }

    private Cell[][] orientedLines(int resolution, int radius, double step) {
        Cell[][] result = new Cell[resolution][];
        for (int orientationIndex = 0; orientationIndex < resolution; ++orientationIndex) {
            List<Cell> line = new ArrayList<>();
            line.add(Cell.zero);
            Point direction = Angle.toVector(Angle.fromOrientation(Angle.bucketCenter(orientationIndex, resolution)));
            for (double r = radius; r >= 0.5; r /= step) {
                Cell sample = direction.multiply(r).round();
                if (!line.contains(sample)) {
                    line.add(sample);
                    line.add(sample.negate());
                }
            }
            result[orientationIndex] = line.toArray(new Cell[line.size()]);
        }
        return result;
    }

    private static DoubleMap smoothRidges(DoubleMap input, DoubleMap orientation, BooleanMap mask, BlockMap blocks,
            double angle, Cell[][] lines) {
        DoubleMap output = new DoubleMap(input.size());
        for (Cell block : blocks.primary.blocks) {
            if (mask.get(block)) {
                Cell[] line = lines[Angle.quantize(Angle.add(orientation.get(block), angle), lines.length)];
                for (Cell linePoint : line) {
                    Block target = blocks.primary.block(block);
                    Block source = target.move(linePoint).intersect(new Block(blocks.pixels));
                    target = source.move(linePoint.negate());
                    for (int y = target.top(); y < target.bottom(); ++y)
                        for (int x = target.left(); x < target.right(); ++x)
                            output.add(x, y, input.get(x + linePoint.x, y + linePoint.y));
                }
                Block blockArea = blocks.primary.block(block);
                for (int y = blockArea.top(); y < blockArea.bottom(); ++y)
                    for (int x = blockArea.left(); x < blockArea.right(); ++x)
                        output.multiply(x, y, 1.0 / line.length);
            }
        }
        return output;
    }

    private BooleanMap binarize(DoubleMap input, DoubleMap baseline, BooleanMap mask, BlockMap blocks) {
        Cell size = input.size();
        BooleanMap binarized = new BooleanMap(size);
        for (Cell block : blocks.primary.blocks)
            if (mask.get(block)) {
                Block rect = blocks.primary.block(block);
                for (int y = rect.top(); y < rect.bottom(); ++y)
                    for (int x = rect.left(); x < rect.right(); ++x)
                        if (input.get(x, y) - baseline.get(x, y) > 0)
                            binarized.set(x, y, true);
            }
        return binarized;
    }

    private static BooleanMap fillBlocks(BooleanMap mask, BlockMap blocks) {
        BooleanMap pixelized = new BooleanMap(blocks.pixels);
        for (Cell block : blocks.primary.blocks)
            if (mask.get(block))
                for (Cell pixel : blocks.primary.block(block))
                    pixelized.set(pixel, true);
        return pixelized;
    }

    private static BooleanMap invert(BooleanMap binary, BooleanMap mask) {
        Cell size = binary.size();
        BooleanMap inverted = new BooleanMap(size);
        for (int y = 0; y < size.y; ++y)
            for (int x = 0; x < size.x; ++x)
                inverted.set(x, y, !binary.get(x, y) && mask.get(x, y));
        return inverted;
    }

    private BooleanMap innerMask(BooleanMap outer) {
        Cell size = outer.size();
        BooleanMap inner = new BooleanMap(size);
        for (int y = 1; y < size.y - 1; ++y)
            for (int x = 1; x < size.x - 1; ++x)
                inner.set(x, y, outer.get(x, y));
        if (innerMaskBorderDistance >= 1)
            inner = shrinkMask(inner, 1);
        int total = 1;
        for (int step = 1; total + step <= innerMaskBorderDistance; step *= 2) {
            inner = shrinkMask(inner, step);
            total += step;
        }
        if (total < innerMaskBorderDistance)
            inner = shrinkMask(inner, innerMaskBorderDistance - total);
        return inner;
    }

    private static BooleanMap shrinkMask(BooleanMap mask, int amount) {
        Cell size = mask.size();
        BooleanMap shrunk = new BooleanMap(size);
        for (int y = amount; y < size.y - amount; ++y)
            for (int x = amount; x < size.x - amount; ++x)
                shrunk.set(x, y, mask.get(x, y - amount) && mask.get(x, y + amount) && mask.get(x - amount, y)
                        && mask.get(x + amount, y));
        return shrunk;
    }

    private void collectMinutiae(Skeleton skeleton, MinutiaType type) {
        minutiae = Stream
                .concat(Arrays.stream(Optional.ofNullable(minutiae).orElse(new Minutia[0])),
                        skeleton.minutiae.stream().filter(m -> m.ridges.size() == 1)
                                .map(m -> new Minutia(m.position, m.ridges.get(0).direction(), type)))
                .toArray(Minutia[]::new);
    }

    private void maskMinutiae(BooleanMap mask) {
        minutiae = Arrays.stream(minutiae).filter(minutia -> {
            Cell arrow = Angle.toVector(minutia.direction).multiply(-maskDisplacement).round();
            return mask.get(minutia.position.plus(arrow), false);
        }).toArray(Minutia[]::new);
    }

    private void cleanupBinarized(BooleanMap binary, BooleanMap mask) {
        Cell size = binary.size();
        BooleanMap inverted = new BooleanMap(binary);
        inverted.invert();
        BooleanMap islands = vote(inverted, mask, binarizedVoteRadius, binarizedVoteMajority,
                binarizedVoteBorderDistance);
        BooleanMap holes = vote(binary, mask, binarizedVoteRadius, binarizedVoteMajority, binarizedVoteBorderDistance);
        for (int y = 0; y < size.y; ++y)
            for (int x = 0; x < size.x; ++x)
                binary.set(x, y, binary.get(x, y) && !islands.get(x, y) || holes.get(x, y));
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
