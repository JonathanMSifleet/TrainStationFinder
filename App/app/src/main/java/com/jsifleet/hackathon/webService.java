package com.jsifleet.hackathon;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class WebService {

	/**
	 * Gets a JSON Array of all the nearby train stations, based upon the latitude and longitude supplied
	 * @param Device's latitude (double)
	 * @param Device's longitude (double)
	 * @return JSON Array containing all station data
	 */
	public JSONArray getStationsFromURL(double lat, double lng) {
		// pass lat and long to URL function
		URL u = buildURL(lat, lng);
		try {
			// open HTTP connection to URL
			HttpURLConnection tc = (HttpURLConnection) u.openConnection();
			// create readers for data from HTTP connection
			InputStreamReader isr = new InputStreamReader(tc.getInputStream());
			BufferedReader in = new BufferedReader(isr);

			String line;
			StringBuilder json = new StringBuilder();

			// append all lines from URL to string
			while ((line = in.readLine()) != null) {
				json.append(line);
			}
			// close readers
			in.close();
			isr.close();

			// return encoded string as JSONArray
			return new JSONArray(json.toString());

		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		return new JSONArray();
	}

	/**
	 * Builds a URL to use for getting nearby train stations, based upon the latitude and longitude supplied
	 * @param Device's latitude (double)
	 * @param Device's longitude (double)
	 * @return URL to use for getting nearby station
	 */
	public static URL buildURL(double lat, double lng) {
		// build URL based on latitude and longitude:
		try {
			return new URL("http://10.0.2.2:8080/stations?lat=" + lat + "&lng=" + lng);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

}
