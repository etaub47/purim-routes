package com.intellectgames.optimal;

import com.google.ortools.constraintsolver.*;
import java.awt.geom.Point2D;
import java.util.*;

/**
 * Handles the Vehicle Routing Problem (VRP) using Google OR-Tools.
 * This class attempts to balance route distance and the number of stops per volunteer.
 */

public class Optimizer {
    private final int maxDistancePerDriver;

    // Higher values force the solver to make routes more equal in length
    // See OR Tools documentation for details
    private static final int GLOBAL_SPAN_COST = 100;

    public Optimizer(int maxDistancePerDriver) {
        this.maxDistancePerDriver = maxDistancePerDriver;
    }

    /**
     * Maps driver addresses to their corresponding index in the coordinate list.
     * This is an input needed by the routing model and indicates to the OR Tools
     *   that we want each volunteer driver to start and end their routes at their own homes.
     *
     * @param drivers the list of volunteer drivers, as household objects, in order
     * @param coords the mapping of street addresses to lat/long coordinates
     * @return an array of integers to feed into the routing model
     */
    public int[] determineEnds(List<Household> drivers, Map<String, Point2D> coords) {
        List<String> orderedAddresses = new ArrayList<>(coords.keySet());
        return drivers.stream()
            .mapToInt(d -> orderedAddresses.indexOf(d.fullAddress))
            .toArray();
    }

    /**
     * Maps volunteer drivers to the max number of stops they want to do.
     * This will allow us to add a "vehicle capacity" constraint to the routing model.
     *
     * @param drivers the list of volunteer drivers, as household objects, in order
     * @return an array of longs to feed into the routing model
     */
    public long[] determineMaxStops(List<Household> drivers) {
        return drivers.stream().mapToLong(d -> d.driverMaxStops).toArray();
    }

    /**
     * This is where the magic happens!
     *
     * @param distances the matrix of relative Euclidean distances between all the households
     * @param numDrivers the number of drivers among which we will be dividing the routes
     * @param ends the start/end location indices for each of the drivers
     * @param maxStops the max number of stops for each of the drivers
     * @return a list of routes, where each route is a list of integers corresponding to the
     *   indexes into the street addresses; the driver can be determined based on the start/end location
     */
    public List<List<Integer>> optimize(long[][] distances, int numDrivers, int[] ends, long[] maxStops) {

        // 1. Initialize the manager (number of locations, number of vehicles, starts, ends)
        RoutingIndexManager manager = new RoutingIndexManager(distances.length, numDrivers, ends, ends);
        RoutingModel routing = new RoutingModel(manager);

        // 2. Define cost of travel (distance)
        final int transitCallbackIndex = routing.registerTransitCallback((long fromIndex, long toIndex) -> {
            int fromNode = manager.indexToNode(fromIndex);
            int toNode = manager.indexToNode(toIndex);
            return distances[fromNode][toNode];
        });
        routing.setArcCostEvaluatorOfAllVehicles(transitCallbackIndex);

        // 3. Define Capacity (Number of stops/bags); each location visited counts as "1" stop
        final int demandCallbackIndex = routing.registerUnaryTransitCallback(fromIndex -> 1);

        // 4. Add Constraints (distance constraint)
        routing.addDimension(transitCallbackIndex, 0, maxDistancePerDriver, true, "Distance");
        RoutingDimension distanceDimension = routing.getMutableDimension("Distance");
        distanceDimension.setGlobalSpanCostCoefficient(GLOBAL_SPAN_COST);

        // Max Stops per Volunteer Constraint
        routing.addDimensionWithVehicleCapacity(demandCallbackIndex, 0, maxStops, true, "Capacity");

        // 5. Set Search Parameters
        RoutingSearchParameters searchParameters = main.defaultRoutingSearchParameters().toBuilder()
            .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
            .setLogSearch(false) // set to true if you need to debug why a solution isn't found
            .build();

        System.out.println("Entering native library");
        Assignment solution = routing.solveWithParameters(searchParameters);
        System.out.println("Exiting native library");

        if (solution == null) {
            throw new IllegalStateException("No feasible route found. Try increasing max distance or drivers.");
        }

        return formatSolution(numDrivers, routing, manager, solution);
    }

    private List<List<Integer>> formatSolution(int numVehicles, RoutingModel routing,
                                               RoutingIndexManager manager, Assignment solution) {

        List<List<Integer>> routeIndexes = new ArrayList<>(numVehicles);

        for (int i = 0; i < numVehicles; ++i) {
            List<Integer> path = new ArrayList<>();
            long index = routing.start(i);
            while (!routing.isEnd(index)) {
                path.add(manager.indexToNode(index));
                index = solution.value(routing.nextVar(index));
            }
            path.add(manager.indexToNode(index)); // add the final stop
            routeIndexes.add(path);
        }
        return routeIndexes;
    }
}
