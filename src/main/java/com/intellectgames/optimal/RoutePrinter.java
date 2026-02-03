package com.intellectgames.optimal;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * This class is responsible for printing the final routes to STDOUT
 */

public class RoutePrinter {
    static void printFinalRoutes(List<List<Integer>> routes, Map<String, Point2D> coords,
                                 List<Household> allHouseholds) {

        List<String> addressKeyList = new ArrayList<>(coords.keySet());

        for (int i = 0; i < routes.size(); i++) {
            System.out.println("\n================================");
            System.out.println("ROUTE #" + (i + 1));
            System.out.println("================================");

            StringJoiner mapUrl = new StringJoiner("/", "https://www.google.com/maps/dir/", "");
            List<Integer> nodeIndices = routes.get(i);

            for (int stopNum = 0; stopNum < nodeIndices.size(); stopNum++) {
                String address = addressKeyList.get(nodeIndices.get(stopNum));
                Point2D pt = coords.get(address);

                // Print household details
                List<Household> matches = allHouseholds.stream()
                    .filter(h -> h.fullAddress.equals(address))
                    .collect(Collectors.toList());

                for (Household h : matches) {
                    System.out.printf("%d. %s (%s)\n", stopNum + 1, h.name, h.fullAddress);
                }

                mapUrl.add(pt.getX() + "," + pt.getY());
            }
            System.out.println("\nGoogle Maps Link:");
            System.out.println(mapUrl);
        }
    }
}
