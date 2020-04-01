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

	public JSONArray getStationsFromURL(double lat, double lng) {

		URL u = buildURL(lat, lng);
		//Log.e("URL", u.toString());
		try {
			HttpURLConnection tc = (HttpURLConnection) u.openConnection();
			InputStreamReader isr = new InputStreamReader(tc.getInputStream());
			BufferedReader in = new BufferedReader(isr);

			String line;
			StringBuilder json = new StringBuilder();

			while ((line = in.readLine()) != null) {
				json.append(line);
			}
			in.close();

			return new JSONArray(json.toString());

		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		return new JSONArray();
	}

	public static URL buildURL(double lat, double lng) {
		try {
			return new URL("http://10.0.2.2:8080/stations?lat=" + lat + "&lng=" + lng);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

}
