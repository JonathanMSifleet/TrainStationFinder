package com.jsifleet.hackathon;

public class Station {

	private String StationName;
	private double lat;
	private double lng;
	private double distance;

	// getters and setters for Station variables:

	/**
	 * Retrieves station's distance
	 * @return Station's distance (double)
	 */
	public double getDistance() {
		return distance;
	}

	/**
	 * Sets station's distance
	 * @param Station's distance (double)
	 */
	public void setDistance(double distance) {
		this.distance = distance;
	}

	/**
	 * Retrieves station's name
	 * @return Station's name (String)
	 */
	public String getStationName() {
		return StationName;
	}

	/**
	 * Sets station's name
	 * @param Station's name (String)
	 */
	public void setStationName(String stationName) {
		StationName = stationName;
	}

	/**
	 * Retrieves station's latitude
	 * @return Station's latitude (double)
	 */
	public double getLat() {
		return lat;
	}

	/**
	 * Sets station's latitude
	 * @param Stations latitude (double)
	 */
	public void setLat(double lat) {
		this.lat = lat;
	}

	/**
	 * Retrieves station's longitude
	 * @return Station's longitude (double)
	 */
	public double getLng() {
		return lng;
	}

	/**
	 * Sets station's longitude
	 * @param Station's longitude double)
	 */
	public void setLng(double lng) {
		this.lng = lng;
	}

}
