package jp.espresso3389.gpsstat;

import java.util.Date;

import android.content.Context;

public abstract class MiscUtils {
	/**
	 * Format time duration in precision of minutes.
	 * @param context Context to load resource strings.
	 * @param duration Duration in milliseconds.
	 * @return Formatted duration.
	 */
	public static String formatDuration(Context context, long duration) {
		int dur = (int)(duration / 60 / 1000);
		if (dur == 0)
			return context.getResources().getString(R.string.durationLessThanMinute);
		String s = null;
		int parts = 0;
		if (dur > 24 * 60 * 365) {
			int months = dur / 24 / 60 / 365;
			s = context.getResources().getString(R.string.xxYears, months);
			dur -= months * 24 * 60 * 365;
			parts++;
		}
		if (dur > 24 * 60 * 30) {
			int months = dur / 24 / 60 / 30;
			s = append(s, context.getResources().getString(R.string.xxMonths, months));
			dur -= months * 24 * 60 * 30;
			if (dur > 24 * 60 * 28) {
				months++;
				dur = 0;
			}
			parts++;
		}
		if (dur > 24 * 60 && parts < 2) {
			int days = dur / 24 / 60;
			s = append(s, context.getResources().getString(R.string.xxDays, days));
			dur -= days * 24 * 60;
			if (dur > 60 * 20) {
				days++;
				dur = 0;
			}
			parts++;
		}
		if (dur > 60 && parts < 2) {
			int hours = dur / 60;
			dur -= hours * 60;
			if (dur > 45) {
				hours++;
				dur = 0;
			}
			s = append(s, context.getResources().getString(R.string.xxHours, hours));
			
			parts++;
		}
		if (dur > 0 && parts < 2) {
			s = append(s, context.getResources().getString(R.string.xxMinutes, dur));
			parts++;
		}
		
		return s;
	}
	
	/**
	 * Format time duration past in precision of minutes.
	 * @param context Context to load resource strings.
	 * @param duration Duration past in milliseconds.
	 * @return Formatted duration.
	 */
	public static String formatTimePast(Context context, long timePast) {
		String dur = MiscUtils.formatDuration(context, timePast);
		if (dur != null)
			return context.getResources().getString(R.string.xxAgo, dur);
		return context.getResources().getString(R.string.lessThan1minute);
	}
	
	/**
	 * Format a time in relative form.
	 * @param context
	 * @param timeStamp Time-stamp in milliseconds since Jan. 1, 1970, midnight GMT.
	 * @return Formatted time.
	 */
	public static String formatTimeRelative(Context context, long timeStamp) {
		return formatTimePast(context, new Date().getTime() - timeStamp);
	}
	
	/**
	 * Format time duration in fixed form HH:MM:SS.
	 * @param duration Duration in milliseconds.
	 * @return Formatted duration.
	 */
	public static String formatDuration(long duration) {
		int dur = (int)(duration / 1000);
		int days = dur / 24 / 60 / 60;
		dur -= days * 24 * 60 * 60;
		int hours = dur / 60 / 60;
		dur -= hours * 60 * 60;
		int mins = dur / 60;
		dur -= mins * 60;
		int secs = dur;
		
		String s = days == 0 ? "" : String.format("%d days ", days);
		return s + String.format("%02d:%02d:%02d", hours, mins, secs);
	}
	
	static String append(String base, String suffix) {
		if (base == null || base.length() == 0)
			return suffix;
		return base + " " + suffix;
	}

}
