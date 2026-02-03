# Purim Route Optimizer

This project uses **Google OR-Tools** to solve the Vehicle Routing Problem (VRP) for Purim bag deliveries.

## Prerequisites
* **Java 11 or higher**, e.g. `brew install openjdk@11`
* **Maven**, e.g. `brew install maven`

## Setup & Troubleshooting
1. **Building and Running the Program**:
    - To run with the default value (4000): `mvn exec:java`
    - To run with a custom distance (e.g. 6000): `mvn exec:java -Dexec.args="6000"`
2. **Maximum Distance Units**:
    - The program requires exactly one input parameter: the maximum distance units.
    - **Too high**: Routes may become extremely long or unevenly distributed.
    - **Too low**: The solver may fail to find any valid route and will exit with an error.

3. **Data Files**:
    - Place your volunteer/recipient list in `data/addresses.csv`.
    - You will likely find it easiest to work in an Excel file or Google Sheet and then export to CSV format.
    - The program caches GPS coordinates in `data/coords.csv` to avoid redundant API calls to the Census Bureau. In some cases, the program fails to look up an address, and it may be necessary to do the lookup by hand and manually add the result to this file before it can proceed.
4. **Likely Errors**:
   - A recipient in a town too far away may cause the algorithm to freak out.
   - Setting the max distance units too small can make a solution impossible.

## CSV Format
The `addresses.csv` expects 13 columns. Here are the most important:
- Column 1: The names of the heads of household
- Column 3: The street address
- Column 4: The unit or apartment number; this needs to be a separate column so that we can use only the street address to look up the lat/long
- Column 5: The town
- Column 12: `Yes` if they want a delivery, `NO` otherwise
- Column 13: Max stops (if > 0, this person is treated as a volunteer driver)
