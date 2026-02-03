package com.intellectgames.optimal;

import java.awt.geom.Point2D;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/**
 * Interfaces with the US Census Geocoder to convert physical addresses
 * into GPS coordinates (Latitude/Longitude).
 * If the list grows over 500 addresses, consider switching to the Google Maps Geocoding API.
 * It requires an API key but handles bulk requests much more reliably.
 */

public class CoordFinder {
    private final HttpClient client = HttpClient.newHttpClient();

    private final Pattern latPattern = Pattern.compile("Latitude \\(Y\\) Coordinates:.*?(-?[0-9]+\\.[0-9]+)");
    private final Pattern longPattern = Pattern.compile("Longitude \\(X\\) Coordinates:.*?(-?[0-9]+\\.[0-9]+)");

    private int failedLookups = 0;

    /**
     * Looks up a set of addresses and tries to get lat/long coordinates
     * @param addresses a set of street addresses, as strings
     * @return a map of street addresses to coordinates
     */
    public Map<String, Point2D> lookupAddresses (Set<String> addresses) {
        Map<String, Point2D> coords = new LinkedHashMap<>();
        for (String address : addresses) {
            Point2D coord = lookupAddress(address);
            if (coord != null) {
                coords.put(address, coord);
            } else {
                failedLookups++;
            }
        }
        return coords;
    }

    /**
     * Gets the number of failed GPS lookups
     * @return the number of failed lookups
     */
    public int getNumFailedLookups () {
        return failedLookups;
    }

    private Point2D lookupAddress (String address) {
        try {
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = "https://geocoding.geo.census.gov/geocoder/geographies/onelineaddress?benchmark=4&vintage=4&address="
                + encodedAddress;

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            Matcher latM = latPattern.matcher(response.body());
            Matcher longM = longPattern.matcher(response.body());

            if (latM.find() && longM.find()) {
                double lat = Double.parseDouble(latM.group(1));
                double lon = Double.parseDouble(longM.group(1));
                return new Point2D.Double(lat, lon);
            }
        } catch (Exception e) {
            System.err.println("Error fetching " + address + ": " + e.getMessage());
        }
        return null;
    }
}
