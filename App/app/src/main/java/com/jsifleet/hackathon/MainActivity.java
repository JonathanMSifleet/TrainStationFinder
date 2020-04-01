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

	private void drawMap() {
		// add location to map
		this.addMapMarker(deviceLat, deviceLng, "Your location");
		// display local stations on map
		this.displayStationsMapBox(listOfStations);
		// reset map to current location
		this.resetCameraLocation(map);
	}

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

	private void displayStationsMapBox(ArrayList<Station> stations) {
		// add a marker for each station onto map
		for (Station curStation : stations) {
			this.addMapMarker(curStation.getLat(), curStation.getLng(), curStation.getStationName());
		}
	}

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

	private double calcDistanceHaversine(double deviceLat, double deviceLng, double lat2, double lng2) {

		// calculates distance using Haversine algorithm:
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

	private void resetCameraLocation(@NonNull MapboxMap mapboxMap) {
		//resets camera to device location:
		mapboxMap.setCameraPosition(
				new CameraPosition.Builder()
						.target(new LatLng(deviceLat, deviceLng))
						.zoom(12.0)
						.build()
		);
	}

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

	class task extends AsyncTask<URL, Void, ArrayList<Station>> {
		// creates a thread that runs in background to get
		// stations location, stores as JSON Array
		protected ArrayList<Station> doInBackground(URL... urls) {
			JSONArray JSONStations = webService.getStationsFromURL(deviceLat, deviceLng);
			return saveJSONToArrayList(JSONStations);
		}

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