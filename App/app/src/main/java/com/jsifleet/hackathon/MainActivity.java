package com.jsifleet.hackathon;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, Style.OnStyleLoaded {

	private MapView mapView;
	private MapboxMap map;
	private SymbolManager sm;

	private Button getStations;
	private ScrollView stationOutput;
	private LinearLayout stationLayout;
	private TextView stationTextView;
	private final WebService webService = new WebService();

	private Double deviceLat = 0.0;
	private Double deviceLng = 0.0;

	private final ArrayList<Station> listOfStations = new ArrayList<>();

	/**
	 * Initialises all required elements and gets device location
	 *
	 * @param Device's last saved state (savedInstanceState)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// get device's location
		this.getLocation();

		// token for mapbox
		final String mapboxToken = "pk.eyJ1Ijoiam9uYXRoYW53YXNoZXJlIiwiYSI6ImNrOGg1dmFmZzAxamMzZXBuYTgzeTVkOWYifQ.YoryZ31-wW4WCy_5orU6MA";

		Mapbox.getInstance(this, mapboxToken);
		setContentView(R.layout.activity_main);

		getStations = this.findViewById(R.id.getStations);
		stationOutput = this.findViewById(R.id.stationOutput);
		stationLayout = this.findViewById(R.id.stationLayout);
		stationTextView = this.findViewById(R.id.stationTextView);

		mapView = findViewById(R.id.mapView);
		mapView.onCreate(savedInstanceState);
		mapView.getMapAsync(this);
	}

	/**
	 * Runs when a button is clicked. Only handles the get stations button.
	 * Clears map and searches for stations
	 *
	 * @param Device view (View)
	 */
	public void onClick(View v) {

		if (v.getId() == R.id.getStations) {
			// clears list of stations
			listOfStations.clear();
			// removes all markers from map box
			map.clear();
			// searches for nearby stations
			this.searchStations();
		}
	}

	/**
	 * Runs function that searches for nearby stations
	 */
	private void searchStations() {
		String[] Permissions = {
				Manifest.permission.INTERNET
		};

		// checks for permissions
		if (checkGotPermission(Permissions)) {
			// run task on thread to get and display local restaurants
			new task().execute(WebService.buildURL(deviceLat, deviceLng));
		} else {
			Log.e("Message", "Do not have permissions");
		}
	}

	/**
	 * Draws all required elements onto map
	 */
	private void drawMap() {
		// add location to map
		this.addMapMarker(deviceLat, deviceLng, "Your location");
		// display local stations on map
		this.displayStationsMapBox(listOfStations);
		// reset map to current location
		this.resetCameraLocation(map);
	}

	/**
	 * Displays each stations name and distance from an Array List of train stations
	 *
	 * @param List of stored train stations (ArrayList<Station>)
	 */
	private void displayStationsText(ArrayList<Station> stations) {
		stationTextView.setText("");

		// display each station's name and and distance to view
		for (Station curStation : stations) {
			stationTextView.append("Name: " + curStation.getStationName() + "\n");
			double tempDistance = curStation.getDistance();
			tempDistance = Math.floor(tempDistance * 100) / 100;
			stationTextView.append("Distance: " + tempDistance + " miles");
			stationTextView.append("\n\n");
		}
	}

	/**
	 * Calls the function to display a marker on the map for each train station
	 *
	 * @param List of stored train stations (ArrayList<Station>)
	 */
	private void displayStationsMapBox(ArrayList<Station> stations) {
		// add a marker for each station onto map
		for (Station curStation : stations) {
			this.addMapMarker(curStation.getLat(), curStation.getLng(), curStation.getStationName());
		}
	}

	/**
	 * Converts a JSON Array containing local train stations, into an Array List of train station objects
	 *
	 * @param JSON Array of stored train stations (JSONArray stations)
	 * @return List of stored train stations (ArrayList<Station>)
	 */
	private ArrayList<Station> saveJSONToArrayList(JSONArray stations) {
		ArrayList<Station> tempListOfStations = new ArrayList<>();

		try {
			// for each station in the JSON Array
			// create a station object to store station data
			for (int i = 0; i < stations.length(); i++) {
				Station tempStation = new Station();

				JSONObject jo = stations.getJSONObject(i);

				tempStation.setStationName(jo.getString("StationName"));
				tempStation.setLat(jo.getDouble("Latitude"));
				tempStation.setLng(jo.getDouble("Longitude"));
				tempStation.setDistance(calcDistanceHaversine(deviceLat, deviceLng, tempStation.getLat(), tempStation.getLng()));

				tempListOfStations.add(tempStation);
			}
		} catch (JSONException e) {
			Log.e("Error", "Something has gone wrong");
			e.printStackTrace();
		}

		return tempListOfStations;
	}

	/**
	 * Creates a location listener service which updates the latitude and longitude that correspond to the device's locations
	 */
	private void getLocation() {

		String[] locationPermissions = {
				Manifest.permission.INTERNET,
				Manifest.permission.ACCESS_FINE_LOCATION,
				Manifest.permission.ACCESS_COARSE_LOCATION
		};

		// checks for permissions
		if (checkGotPermission(locationPermissions)) {

			// create a listener for the devices location
			LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
				@Override
				public void onLocationChanged(Location location) {
					// store device latitude and longitude
					deviceLat = location.getLatitude();
					deviceLng = location.getLongitude();
				}

				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {

				}

				@Override
				public void onProviderEnabled(String provider) {

				}

				@Override
				public void onProviderDisabled(String provider) {

				}
			});
		}
	}

	/**
	 * Checks if the app has all the required permissions, and requests them if not
	 *
	 * @param Array of required permissions (String[])
	 * @return True or false regarding whether the app has got required permissions (boolean)
	 */
	private boolean checkGotPermission(String[] requiredPermissions) {

		// checks for permissions
		boolean ok = true;
		for (String requiredPermission : requiredPermissions) {
			int result = ActivityCompat.checkSelfPermission(this, requiredPermission);
			if (result != PackageManager.PERMISSION_GRANTED) {
				ok = false;
			}
		}

		// requests permissions if required
		if (!ok) {
			ActivityCompat.requestPermissions(this, requiredPermissions, 1);
			// last permission must be > 0
		} else {
			return true;
		}
		return false;
	}

	/**
	 * Uses the Haversine formula to determine the distance between the device and the location
	 *
	 * @param Device's    latitude (double)
	 * @param Device's    longitude (double)
	 * @param Location's  latitude (double)
	 * @param Locations's longitude (double)
	 * @return Distance from device to location in miles (double)
	 */
	private double calcDistanceHaversine(double deviceLat, double deviceLng, double lat2, double lng2) {

		// calculates distance using Haversine formula:
		final double R = 6372.8; // kilometers

		double tempDeviceLat = deviceLat;

		double dLat = Math.toRadians(lat2 - tempDeviceLat);
		double dLon = Math.toRadians(lng2 - deviceLng);

		tempDeviceLat = Math.toRadians(tempDeviceLat);
		lat2 = Math.toRadians(lat2);

		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(tempDeviceLat) * Math.cos(lat2);
		double c = 2 * Math.asin(Math.sqrt(a));

		return convertToMiles(R * c);
	}

	/**
	 * Converts a distance in kilometers to miles
	 *
	 * @param Distance in kilometers (double)
	 * @return Distance in miles (double)
	 */
	private double convertToMiles(double distance) {
		// converts kilometers to miles
		return distance * 0.62137;
	}

	@Override
	public void onMapReady(@NonNull MapboxMap mapboxMap) {
		map = mapboxMap;
		//required:
		mapboxMap.setStyle(Style.OUTDOORS, this);
		//resets camera to device location:
		this.resetCameraLocation(mapboxMap);
	}

	@Override
	public void onStyleLoaded(@NonNull Style style) {
		// symbol manager is responsible for adding map markers:
		sm = new SymbolManager(mapView, map, style);
		// adds a marker where the device's location is
		addMapMarker(deviceLat, deviceLng, "Your location");
	}

	/**
	 * Resets the camera location to the device's location
	 *
	 * @param Mapbox Map (mapboxMap)
	 */
	private void resetCameraLocation(@NonNull MapboxMap mapboxMap) {
		//resets camera to device location:
		mapboxMap.setCameraPosition(
				new CameraPosition.Builder()
						.target(new LatLng(deviceLat, deviceLng))
						.zoom(12.0)
						.build()
		);
	}

	/**
	 * Adds a map marker onto the map box map based upon the locations latitude and longitude
	 *
	 * @param Location's latitude (double)
	 * @param Location's longitude (double)
	 * @param Location's name (String)
	 */
	private void addMapMarker(double lat, double lng, String name) {
		// adds a marker to specified latitude and longitude:
		MarkerOptions tempMarker = new MarkerOptions();
		tempMarker.position(new LatLng(lat, lng));
		tempMarker.title(name);

		// add market to map:
		map.addMarker(tempMarker);
	}

	@Override
	public void onStart() {
		super.onStart();
		mapView.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
		mapView.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		mapView.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
		mapView.onStop();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mapView.onLowMemory();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mapView.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

	/**
	 * Class that contains asynchronous functions for threading
	 */
	class task extends AsyncTask<URL, Void, ArrayList<Station>> {

		/**
		 * Creates a thread that runs in the background to get all of the station's data, and stores it in a JSON Array
		 *
		 * @param URL for server app (URL)
		 * @return List of nearby train stations (ArrayList<Station>)
		 */
		protected ArrayList<Station> doInBackground(URL... urls) {
			JSONArray JSONStations = webService.getStationsFromURL(deviceLat, deviceLng);
			return saveJSONToArrayList(JSONStations);
		}

		/**
		 * Runs after the asynchronous task has completed. Displays the nearby train station's name and distance onto the text view.
		 * Draws all required elements onto the map
		 *
		 * @param List of nearby train stations (ArrayList<Station>)
		 */
		protected void onPostExecute(ArrayList<Station> curStationList) {

			// sorts array of stations by distance ascending:
			curStationList = bubbleSortArray(curStationList);

			listOfStations.addAll(curStationList);

			// displays station name and distance in view:
			displayStationsText(listOfStations);
			// draws required elements to mapbox map
			drawMap();
		}
	}

	/**
	 * Uses bubble sort to sort all nearby train stations by distance ascending
	 *
	 * @param List of stations to be sorted (ArrayList<Station>)
	 * @return Sorted list of stations (ArrayList<Station>)
	 */
	private ArrayList<Station> bubbleSortArray(ArrayList<Station> curStationList) {
		ArrayList<Station> tempStations = new ArrayList<>(curStationList);

		// checks if the stations are displayed in the correct order,
		// if not arrange by distance ascending
		boolean hasSwapped = true;
		while (hasSwapped) {
			Station temp;
			hasSwapped = false;
			for (int i = 0; i < tempStations.size() - 1; i++) {
				if (tempStations.get(i + 1).getDistance() < tempStations.get(i).getDistance()) {
					temp = tempStations.get(i);
					tempStations.set(i, tempStations.get(i + 1));
					tempStations.set(i + 1, temp);
					hasSwapped = true;
				}
			}
		}

		return tempStations;
	}
}