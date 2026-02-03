package com.intellectgames.optimal;

import com.google.ortools.Loader;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This is the main class for the Purim Routes program.
 * It expects a single input parameter that represents the maximum distance units.
 * Please see README for more information.
 */

public class Main {
    static {
        Loader.loadNativeLibraries();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java Main <max_distance_units>");
            return;
        }

        int maxDistance = Integer.parseInt(args[0]);
        FileHelper fileHelper = new FileHelper();
        CoordFinder coordFinder = new CoordFinder();

        // 1. Data Loading
        System.out.println("--- Phase 1: Loading Data ---");
        List<Household> recipients = fileHelper.loadHouseholds();
        List<Household> drivers = fileHelper.getDrivers();
        Map<String, Point2D> coords = new LinkedHashMap<>(fileHelper.loadCoordinates());

        // 2. Coordinate Management
        syncCoordinates(recipients, coords, coordFinder, fileHelper);

        // 3. Distance Computation
        System.out.println("\n--- Phase 2: Computing Distances ---");
        DistanceComputer distanceComputer = new DistanceComputer();
        long[][] distances = distanceComputer.computeDistances(coords);

        // 4. Optimization
        System.out.println("\n--- Phase 3: Optimizing Routes ---");
        Optimizer optimizer = new Optimizer(maxDistance);
        int[] ends = optimizer.determineEnds(drivers, coords);
        long[] maxStops = optimizer.determineMaxStops(drivers);
        List<List<Integer>> routes = optimizer.optimize(distances, drivers.size(), ends, maxStops);

        // 5. Output Generation
        RoutePrinter.printFinalRoutes(routes, coords, recipients);
    }

    private static void syncCoordinates(List<Household> households, Map<String, Point2D> coords,
                                        CoordFinder finder, FileHelper fileHelper) throws Exception {
        Set<String> currentAddresses = households.stream()
            .map(h -> h.fullAddress).collect(Collectors.toSet());

        // Cleanup old data from the cache that is no longer in the addresses.csv input file
        coords.keySet().removeIf(addr -> !currentAddresses.contains(addr));

        // Find addresses in the addresses.csv input file that do not yet exist in the cache
        Set<String> missing = currentAddresses.stream()
            .filter(addr -> !coords.containsKey(addr))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        // Lookup the coordinates for the missing addresses, and update the cache file
        if (!missing.isEmpty()) {
            System.out.println("Fetching " + missing.size() + " new coordinates...");
            coords.putAll(finder.lookupAddresses(missing));
            fileHelper.storeCoordinates(coords);
        }

        // If we failed to find coordinates for even a single address, we bail out at this point.
        // The address can be fixed, the address can be deleted, or the coordinates can be added manually.
        if (finder.getNumFailedLookups() > 0) {
            System.err.println("Warning: " + finder.getNumFailedLookups() + " addresses failed geocoding.");
            System.exit(1);
        }
    }
}
