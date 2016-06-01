package jp.espresso3389.gpsstat;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility routines for Google Map integration.
 */
public abstract class MapUtils {
	/**
	 * Gets Google Map API key for the specified context.
	 * @param context Context for application.
	 * @return Google Map API key for the application.
	 */
	public static String getMapApiKey(Context context) {
		PackageManager pm = context.getPackageManager();
		try {
			PackageInfo pi = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
			for (Signature sig : pi.signatures) {
				String key = getMapApiKey(sig);
				if (key != null) {
					Log.i("GpsMapActivity", String.format("API Key: %s", key));
					return key;
				}
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Gets Google MAP API key for the specified signature.
	 * @param sig Application signature.
	 * @return Google Map API key for the signature.
	 */
	public static String getMapApiKey(Signature sig) {
		String sigStr = getSignatureFingerPrint(sig);
		
		Log.i("GpsMapActivity", String.format("Signature: %s", sigStr));
		
		// For debug.keystore
		if (sigStr.equals("E1:50:50:74:10:91:D3:8C:CC:1E:64:03:C1:8C:29:C5"))
			return "0uYCxHsBxtyCl8SIPAHoIQjcO8otqFWmi-aVgSw";
		
		// For releasing with espresso3389.keystore
		if (sigStr.equals("69:13:0F:1B:BB:B3:AC:EA:AA:6C:1F:2D:6A:44:5A:41"))
			return "0uYCxHsBxtyBkE_WTGlAslQygzfrg3Wc9yDd6Ww";
		return null;
	}
	
	/**
	 * Gets string notation of the specified signature finger print (MD5).
	 * @param sig Application signature.
	 * @return MD5 finger print for the signature (in string).
	 */
	static String getSignatureFingerPrint(Signature sig) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(sig.toByteArray());
			return hex(md.digest());
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets hexadecimal notation of the specified binary.
	 * @param bin Binary.
	 * @return Hexadecimal notation of the specified binary.
	 */
	static String hex(byte[] bin) {
		StringBuilder sb = new StringBuilder(bin.length * 3 - 1);
		for (int i = 0; i < bin.length; i++) {
			sb.append(String.format("%02X", bin[i]));
			if (i + 1 < bin.length)
				sb.append(':');
		}
		return sb.toString();
	}
}
