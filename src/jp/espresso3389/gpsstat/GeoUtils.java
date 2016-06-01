package jp.espresso3389.gpsstat;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;

import com.google.android.maps.GeoPoint;

/**
 * Utilities for measure distances between two points on map.
 */
public abstract class GeoUtils {
	/**
	 * Calculates vertical distance in meters between two latitudes.
	 * @param y0 Latitude #0
	 * @param y1 Latitude #1
	 * @return Distance between y0 and y1 in meters.
	 */
	public static double getDistY(double y0, double y1) {
		return (y1 - y0) * 40007880 / 360;
	}
	
	/**
	 * Calculates horizontal distance in meters between two longitudes.
	 * Please note that the distance between two longitudes depends on its latitude and you
	 * should pass latitude to calculate two longitudes. And, if y0 and y1 are not near
	 * enough, the calculation has much error.
	 * @param x0 Longitude #0
	 * @param y0 Latitude #0
	 * @param x1 Longitude #1
	 * @param y1 Latitude #1
	 * @return Distance between x0 and x1 in meters.
	 */
	public static double getDistX(double x0, double y0, double x1, double y1) {
		return (x1 - x0) * Math.cos((y0 + y1) / 2 / 180 * Math.PI) * 40075017 / 360;
	}
	
	/**
	 * Calculates square of distance in meters^2 between two points.
	 * @param x0 Longitude #0
	 * @param y0 Latitude #0
	 * @param x1 Longitude #1
	 * @param y1 Latitude #1
	 * @return Square of distance between two points in meters.
	 */
	public static double getDistance2(double x0, double y0, double x1, double y1) {
		double dy = getDistY(y0, y1);
		double dx = getDistX(x0, y0, x1, y1);
		return dx * dx + dy * dy;
	}
	
	/**
	 * Calculates distance in meters between two points.
	 * @param x0 Longitude #0
	 * @param y0 Latitude #0
	 * @param x1 Longitude #1
	 * @param y1 Latitude #1
	 * @return Distance between two points in meters.
	 */
	public static double getDistance(double x0, double y0, double x1, double y1) {
		return Math.sqrt(getDistance2(x0, y0, x1, y1));
	}
	
	/**
	 * This interface is used to notify completion of an address resolution.
	 */
	public interface OnAddressResolutionComplete {
		/**
		 * Called on completion of an address resolution.
		 * @param address Address corresponding to the queried location if succeeded; otherwise null.
		 */
		void callback(String address);
	}
	
	/**
	 * Gets Geo-point of the location.
	 * @return {@link com.google.android.maps.GeoPoint} of the location.
	 */
	public static GeoPoint getGeoPoint(Location location) {
		return new GeoPoint((int)(location.getLatitude() * 1E6), (int)(location.getLongitude() * 1E6));
	}
	
	/**
	 * Resolve address for a location in background.
	 * @param location Location to query.
	 * @param context Context for the current application.
	 * @param handler Handler to receive completion callback.
	 * @param onComplete Completion callback.
	 */
	public static void resolveAddressForLocation(GeoPoint location, Context context, Handler handler, OnAddressResolutionComplete onComplete) {
		final GeoPoint _location = location;
		final Context _context = context;
		final Handler _handler = handler;
		final OnAddressResolutionComplete _onComplete = onComplete;
		
		_threadPoolExecutor.execute(new Runnable() {
			public void run() {
				final String addr = resolveAddressForLocation(_location, _context);
				_handler.post(new Runnable() {
					public void run() {
						_onComplete.callback(addr);
					}});
			}});
	}
	
	static ThreadPoolExecutor _threadPoolExecutor = new ThreadPoolExecutor(1, 1, 2, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()); 
	
	/**
	 * Resolve address for a location.
	 * Please note that the method may take long and you had better use another version of the method:
	 * {@link resolveAddressForLocation(GeoPoint,Context,Handler,OnAddressResolutionComplete)}.
	 * @param location Location to query.
	 * @param context Context for the current application.
	 * @return The address for the queried location if available; otherwise null.
	 */
	public static String resolveAddressForLocation(GeoPoint location, Context context) {
		try {
			Geocoder gc = new Geocoder(context, Locale.getDefault());
			List<Address> addrs = gc.getFromLocation(location.getLatitudeE6() / 1e6, location.getLongitudeE6() / 1e6, 1);
			return getAddressString(addrs);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	static String getAddressString(List<Address> addrs) {
		if (addrs == null || addrs.size() == 0)
			return null;
		
		String country = Locale.getDefault().getDisplayCountry();	
		Address addr = addrs.get(0);
		String s = "";
		int last = addr.getMaxAddressLineIndex();
		for (int i = 0; i <= last; i++) {
			String line = addr.getAddressLine(i);
			if (line.equals(country))
				continue; // omit my country
			
			if (!s.equals(""))
				s += "\n";
			s += line;
		}
		return s;
	}
}
