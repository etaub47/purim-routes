package com.intellectgames.optimal;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.awt.geom.Point2D;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This file helps read and write the files from disk
 */

public class FileHelper {
    private final List<Household> drivers = new ArrayList<>();

    // the names of the input file and the coordinate cache file are hard-coded here
    private static final String ADDR_FILE = "data/addresses.csv";
    private static final String COORD_FILE = "data/coords.csv";

    public List<Household> getDrivers() {
        return drivers;
    }

    /**
     * Loads all the households from the input file, which also indicates the volunteer drivers
     * @return a list of households that were found in the file
     * @throws Exception the file could not be found or could not be read
     */
    public List<Household> loadHouseholds() throws Exception {
        List<Household> households = new ArrayList<>();
        drivers.clear();

        try (Reader reader = new FileReader(ADDR_FILE, StandardCharsets.UTF_8)) {

            // Using RFC4180 format which handles quotes and commas correctly
            CSVFormat format = CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

            CSVParser parser = format.parse(reader);

            for (CSVRecord record : parser) {
                Household h = new Household();

                h.name = record.get(0);
                // column 1 has the number of adults; currently unused
                h.address = record.get(2);
                h.unit = record.get(3);
                h.town = record.get(4);
                h.state = record.get(5);
                h.zip = record.get(6);
                h.phone1 = record.get(7);
                h.phone2 = record.get(8);
                h.email1 = record.get(9);
                h.email2 = record.get(10);

                boolean deliver = "yes".equalsIgnoreCase(record.get(11));
                h.driverMaxStops = Integer.parseInt(record.get(12));

                // This household does not want a Purim bag delivered, and this is not a volunteer driver,
                //   so we can short-circuit the loop iteration here
                if (!deliver && h.driverMaxStops == 0)
                    continue;

                h.generateFullAddress();

                // A household with max stops > 0 indicates that this is a volunteer driver (or walker).
                if (h.driverMaxStops > 0) {
                    drivers.add(h);
                }
                households.add(h);
            }
        }
        return households;
    }

    /**
     * Loads all the known coordinates from the cache file
     * @return a map of street addresses to coordinates
     * @throws Exception the file was corrupted or could not be found
     */
    public Map<String, Point2D> loadCoordinates() throws Exception {
        Map<String, Point2D> coords = new LinkedHashMap<>();
        File file = new File(COORD_FILE);
        if (!file.exists()) return coords;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Use a pipe separator for the cache file since addresses usually contain commas
                String[] pieces = line.split("\\|");
                if (pieces.length == 3) {
                    Point2D pt = new Point2D.Double(
                        Double.parseDouble(pieces[1]),
                        Double.parseDouble(pieces[2])
                    );
                    coords.put(pieces[0], pt);
                }
            }
        }
        return coords;
    }

    /**
     * Stores the coordinates in the cache file
     * @param coords a map of street addresses to coordinates
     * @throws Exception the file could not be saved
     */
    public void storeCoordinates(Map<String, Point2D> coords) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter(COORD_FILE))) {
            for (Map.Entry<String, Point2D> entry : coords.entrySet()) {
                writer.printf("%s|%f|%f%n",
                    entry.getKey(),
                    entry.getValue().getX(),
                    entry.getValue().getY()
                );
            }
        }
    }
}
