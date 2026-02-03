package com.intellectgames.optimal;

/**
 * This simple class contains the information associated with a single household
 */

public class Household {
  public String name;
  public String address;
  public String unit;
  public String town;
  public String state;
  public String zip;
  public String phone1, phone2;
  public String email1, email2;
  public int driverMaxStops;
  public String fullAddress;

  /**
   * Helper to format the address for the Geocoder API
   */
  public void generateFullAddress() {
    this.fullAddress = String.format("%s, %s, %s",
      address.trim(), town.trim(), state.trim());
  }
}
