package com.intellectgames.optimal;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for computing the Euclidean distances between each point in the matrix.
 * Distances use arbitrary units and are measured "as the crow flies".
 */

public class DistanceComputer {
    public long[][] computeDistances(Map<String, Point2D> coords) {
        long[][] distances = new long[coords.size()][coords.size()];
        List<Point2D> coordsList = new ArrayList<>(coords.values());
        for (int cx = 0; cx < coords.size() - 1; cx++) {
            for (int cy = cx + 1; cy < coords.size(); cy++) {
                Point2D start = coordsList.get(cx);
                Point2D end = coordsList.get(cy);
                double distanceDbl = Point2D.distance(start.getX(), start.getY(), end.getX(), end.getY());
                distances[cx][cy] = (long) (distanceDbl * 10000); // convert double to long for the solver
                distances[cy][cx] = (long) (distanceDbl * 10000); // convert double to long for the solver
            }
        }
        return distances;
    }
}
