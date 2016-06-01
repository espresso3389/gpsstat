package jp.espresso3389.gpsstat;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GpsStatus.Listener;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class GpsStatActivity extends MapActivity implements Listener, LocationListener, SensorEventListener, GeoUtils.OnAddressResolutionComplete {

	Handler mHandler = new Handler();
	CanvasView mCanvasView;
	MapView mMapView;
	SensorManager mSensorManager;
	Sensor mSensor;
	LocationManager mLocationManager;
	enum GpsStat {
		Stopped,
		Started,
		FirstFix,
	}
	GpsStat mGpsStat = GpsStat.Stopped;
	Location[] mLocationHistory = new Location[20];
	int mHistoryPos = 0;
	enum GpsFixedStat {
		NotFixed,
		Fixed,
		LocationObtained,
	}
	GpsFixedStat mFixedStat = GpsFixedStat.NotFixed;
	float mNorth = 0f;
	String mAddress = null;
	Date mTimeStarted;

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        mCanvasView = new CanvasView(this);
        ll.addView(mCanvasView);
        mMapView = new MapView(this, MapUtils.getMapApiKey(this));
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.FILL_PARENT);
        ll.addView(mMapView, lp);
        setContentView(ll);
        
        mMapView.setClickable(true);
        mMapView.setBuiltInZoomControls(true);
        mMapView.getOverlays().add(new EffectOverlay());
    }
    
	@Override
	protected void onPause() {
		super.onPause();
		
		mLocationManager.removeGpsStatusListener(this);
		mLocationManager.removeUpdates(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
		mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		mLocationManager.addGpsStatusListener(this);
		
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		//mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
		
		update();
	}
	

	public void callback(String address) {
		mAddress = address;
		update();
	}

	public void onLocationChanged(Location location) {
		addNew(location);
		GeoPoint gp = GeoUtils.getGeoPoint(location);
		update();
		mMapView.getController().animateTo(gp);
		mMapView.invalidate();
		GeoUtils.resolveAddressForLocation(gp, this, mHandler, this);
	}

	public void onProviderDisabled(String provider) {
		update();
	}

	public void onProviderEnabled(String provider) {
		update();
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
	
	public void onGpsStatusChanged(int event) {
		switch(event) {
		case GpsStatus.GPS_EVENT_STARTED:
			mGpsStat = GpsStat.Started;
			mTimeStarted = new Date();
			return;
		case GpsStatus.GPS_EVENT_STOPPED:
			mGpsStat = GpsStat.Stopped;
			return;
		case GpsStatus.GPS_EVENT_FIRST_FIX:
			mGpsStat = GpsStat.FirstFix;
			mFixedStat = GpsFixedStat.Fixed;
			return;
		}
		update();
	}
	
	Location getAt(int index) {
		index = index % mLocationHistory.length + 1;
		int pos = (mHistoryPos - index + mLocationHistory.length) % mLocationHistory.length;
		return mLocationHistory[pos];
	}
	
	Location getLast() {
		int last = (mHistoryPos + mLocationHistory.length - 1) % mLocationHistory.length;
		return mLocationHistory[last];
	}
	
	void addNew(Location location) {
		mLocationHistory[mHistoryPos] = location;
		mHistoryPos = (mHistoryPos + 1) % mLocationHistory.length;
	}

	public void update() {
		mCanvasView.invalidate();
	}
	
	class CanvasView extends View {

		public CanvasView(Context context) {
			super(context);

			// timer routine to animate the things
	        final int TIMER_INTERVAL = 150;
	        mHandler.postDelayed(new Runnable() {
				public void run() {
					for (Iterator<HashMap.Entry<Integer, Satellite>> it = mProgresses.entrySet().iterator(); it.hasNext(); ) {
						Satellite sat = ((HashMap.Entry<Integer, Satellite>)it.next()).getValue();
						if (++sat.progress > Satellite.PROGRESS_MAX)
							it.remove();
					}
					mCanvasView.invalidate();
					
					mHandler.postDelayed(this, TIMER_INTERVAL);
				}}, TIMER_INTERVAL);
	    }
		
		int mWidth, mHeight;
		float mRadHorz, mRadVert;
		PointF mOrigin;
		static final float F = -.2f;
		HashMap<Integer, Satellite> mProgresses = new HashMap<Integer, Satellite>();
		
		class Satellite {
			static final int PROGRESS_ANIM_MAX = 8;
			static final int PROGRESS_MAX = PROGRESS_ANIM_MAX * 2;
			public int progress;
			public GpsSatellite satellite;
			
			public Satellite(GpsSatellite satellite) {
				this.satellite = satellite;
				this.progress = 0;
			}
		}

		@Override
		public void draw(Canvas canvas) {
			
			canvas.drawColor(Color.BLUE);
			
            Paint paintText = new Paint();
            paintText.setAntiAlias(true);
            paintText.setColor(Color.WHITE);
            paintText.setStyle(Paint.Style.FILL_AND_STROKE);
            
            if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            	Rect bounds = new Rect();
            	String s = "GPS is not enabled";
            	paintText.setTextSize(30);
    			paintText.getTextBounds(s, 0, s.length(), bounds);
    			canvas.drawText(s,
    				(mWidth - bounds.width()) / 2,
    				(mHeight - bounds.height()) / 2,
    				paintText);
            	return;
            }
            
            GpsStatus stat = mLocationManager.getGpsStatus(null);
            //LocationProvider lp = mLocationManager.getProvider(LocationManager.GPS_PROVIDER);

			Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f);
            
			Paint fill = new Paint();
			fill.setAntiAlias(true);
			fill.setStyle(Paint.Style.FILL_AND_STROKE);

			Path path = new Path();
			path.addOval(new RectF(-mRadHorz, -mRadVert, mRadHorz, mRadVert), Path.Direction.CW);
			Matrix mat = new Matrix();
			mat.setValues(new float[] {1f, F, mOrigin.x, 0f, 1f, mOrigin.y, 0f, 0f, 1f});
			path.transform(mat);
			fill.setColor(0xff004488);
			canvas.drawPath(path, fill);
			canvas.drawPath(path, paint);
			
			{
				Azimuth azNS = new Azimuth(0f);
				Azimuth azEW = new Azimuth(90f);
				PointF ptN = azNS.getPointRelative(0, false);
				PointF ptE = azEW.getPointRelative(0, false);
				ptE.x /= 4f;
				ptE.y /= 4f;
				
				path = new Path();
				path.moveTo(mOrigin.x + ptN.x, mOrigin.y + ptN.y);
				path.lineTo(mOrigin.x + ptE.x, mOrigin.y + ptE.y);
				path.lineTo(mOrigin.x - ptE.x, mOrigin.y - ptE.y);
				fill.setColor(0xff880000);
				canvas.drawPath(path, fill);
				path = new Path();
				path.moveTo(mOrigin.x - ptN.x, mOrigin.y - ptN.y);
				path.lineTo(mOrigin.x + ptE.x, mOrigin.y + ptE.y);
				path.lineTo(mOrigin.x - ptE.x, mOrigin.y - ptE.y);
				fill.setColor(0xff000088);
				canvas.drawPath(path, fill);
			}
			
			paintText.setColor(Color.WHITE);
			paintText.setTextSize(12);
			final float axes[] = {0f, 90f};
			final String cap[] = {"North", "East"};
			final String capR[] = {"South", "West"};
			for (int i = 0; i < 2; i++) {
				Azimuth az = new Azimuth(axes[i]);
				az.drawLine(0, false, false, canvas, paint);
				az.drawArc(0, 180, canvas, paint);
				az.drawCaption(0, cap[i], false, canvas, paintText);
				az.drawCaption(0, capR[i], true, canvas, paintText);
			}
			
			// shadow
			fill.setColor(0x55000000);
			for (GpsSatellite sat : stat.getSatellites()) {
				Azimuth az = new Azimuth(sat.getAzimuth());
				//az.drawLine(sat.getElevation(), true, true, canvas, fill);
				PointF ptSat = az.getPoint(sat.getElevation(), true);
				float r = sat.getSnr() / 6;
				canvas.drawOval(new RectF(ptSat.x - r, ptSat.y - r / 2, ptSat.x + r, ptSat.y + r / 2), fill);
			}

			/*
			// line
			for (GpsSatellite sat : stat.getSatellites()) {
				Azimuth az = new Azimuth(sat.getAzimuth());
				//az.drawLine(sat.getElevation(), true, false, canvas, paint);
				az.drawArc(0, 180, canvas, paint);
			}
			*/
			
			fill.setColor(0xff00ff00);
			paintText.setTextSize(8);
			int gpsCount = 0;
			for (GpsSatellite sat : stat.getSatellites()) {
				int a = (int)sat.getSnr() * 10;
				if (a > 255) a = 255;
				fill.setColor(0x10000 * (255 - a) + 0x100 * a + 0xff000000);
				
				Azimuth az = new Azimuth(sat.getAzimuth());
				PointF ptSat = az.getPoint(sat.getElevation(), false);
				canvas.drawCircle(ptSat.x, ptSat.y, sat.getSnr() / 4, fill);
				az.drawCaption(sat.getElevation(), String.format("#%d (%f)", sat.getPrn(), sat.getSnr()), false, canvas, paintText);
				gpsCount++;
				
				final int W = 20;
				int x = mWidth - W * gpsCount;
				int y = mHeight - W;
				//canvas.drawRect(x, y, x + W, y + W, paint);
				canvas.drawCircle(x + W / 2, y + W / 2, sat.getSnr() / 4, fill);
				
				Integer pnr = new Integer(sat.getPrn());
				if (!mProgresses.containsKey(pnr))
					mProgresses.put(pnr, new Satellite(sat));
			}
			
			// wave animation from satellite
			Paint satWave = new Paint(fill);
			satWave.setStyle(Paint.Style.STROKE);
			satWave.setColor(0x00ff00);
			satWave.setStrokeWidth(1.5f);
			for (Satellite sat : mProgresses.values()) {
				if (sat.progress > Satellite.PROGRESS_ANIM_MAX)
					continue;
				
				Azimuth az = new Azimuth(sat.satellite.getAzimuth());
				PointF pt = az.getPointRelative(sat.satellite.getElevation(), false);
				float d = (float)sat.progress / Satellite.PROGRESS_ANIM_MAX;
				float r = mRadVert * d;
				satWave.setAlpha((int)(160 * (1 - d)));
				canvas.drawCircle(pt.x + mOrigin.x, pt.y + mOrigin.y, r, satWave);
			}

			paintText.setTextSize(20);
			if (gpsCount < 3)
				paintText.setColor(Color.RED);
			else if(gpsCount == 3)
				paintText.setColor(Color.YELLOW);
			else if(gpsCount == 4)
				paintText.setColor(Color.GREEN);
			
			String s = "";
			switch(mGpsStat) {
			case Stopped:
				s = "GPS Stopped";
				break;
			case Started:
				s = "GPS Started";
				break;
			case FirstFix:
				s = String.format("FirstFix (%d ms)", stat.getTimeToFirstFix());
				break;
			}
			
			long now = new Date().getTime();
			
			Rect bounds = new Rect();
			if (mTimeStarted != null)
				s = String.format("%s %s", MiscUtils.formatDuration(now - mTimeStarted.getTime()), s);
			
			paintText.getTextBounds(s, 0, s.length(), bounds);
			int y = bounds.bottom - bounds.top + 2;
			canvas.drawText(s, 0, y, paintText);

			Location last = getLast();
			if (last != null && mTimeStarted != null) {
				long age = now - last.getTime();
				int a = 255 - (int)(age / 200);
				if (a < 80) a = 80;
				paintText.setAlpha(a);
				
				s = String.format("%s %f,%f(%.1f) %s",
					MiscUtils.formatDuration(last.getTime() - mTimeStarted.getTime()),
					last.getLatitude(), last.getLongitude(),
					last.getAccuracy(),
					last.getProvider());
				paintText.getTextBounds(s, 0, s.length(), bounds);
				y += bounds.height() + 2;
				canvas.drawText(s, 0, y, paintText);
			}
		}
		
		class Azimuth {
			/**
			 * Initializes instance.
			 * @param azimuth Azimuth in 0-360. 0/360 means north.
			 */
			public Azimuth(float azimuth) {
				mAzimuth = azimuth;
				azimuth -= mNorth;
				mY = (float)(-mRadVert * Math.cos(azimuth * Math.PI / 180));
				mX = (float)(mRadHorz * Math.sin(azimuth * Math.PI / 180));
			}
			
			/**
			 * Calculate relative point corresponding to the specified elevation.
			 * @param elevation in 0-360. 0 is horizontal and 90 is the vertical line.
			 * @return Point corresponding to the elevation value.
			 */
			public PointF getPointRelative(float elevation, boolean shadow) {
				float ev = (float)Math.cos(elevation * Math.PI / 180);
				float x = mX * ev;
				float y = mY * ev;
				
				x += y * F;
				if (!shadow)
					y -= (float)(mRadHorz * Math.sin(elevation * Math.PI / 180));
				return new PointF(x, y);
			}
			
			/**
			 * Calculate point corresponding to the specified elevation.
			 * @param elevation in 0-360. 0 is horizontal and 90 is the vertical line.
			 * @return Point corresponding to the elevation value.
			 */
			public PointF getPoint(float elevation, boolean shadow) {
				PointF pt = getPointRelative(elevation, shadow);
				pt.offset(mOrigin.x, mOrigin.y);
				return pt;
			}
			
			public void drawLine(float elevation, boolean half, boolean shadow, Canvas canvas, Paint paint) {
				PointF pt = getPointRelative(elevation, shadow);
				PointF pt0 = new PointF(mOrigin.x + pt.x, mOrigin.y + pt.y);
				PointF pt1 = half ? new PointF(mOrigin.x, mOrigin.y) : new PointF(mOrigin.x - pt.x, mOrigin.y - pt.y);
				canvas.drawLine(pt0.x, pt0.y, pt1.x, pt1.y, paint);
			}
			
			// FIXME: Not a almighty function
			float clamp(float a) {
				if (a < 0) return a + 360;
				if (a >= 360) return a - 360;
				return a;
			}
			
			public void drawArc(float start, float end, Canvas canvas, Paint paint) {
				Paint p = new Paint(paint);
				float alpha = p.getAlpha() / 255.0f;
				float a0 = alpha * (55f + 200f * clamp(AZ_BASE - clamp(mAzimuth - mNorth)) / 360f);
				float a1 = alpha * (55f + 200f * clamp(AZ_BASE - clamp(mAzimuth + 180 - mNorth)) / 360f);
				
				PointF ptPrev = null;
				float a = start;
				while (true) {
					PointF pt = getPoint(a, false);
					if (ptPrev != null) {
						float d = clamp(a) / 180;
						if (d > 1f) d = 2f - d;
						p.setAlpha((int)(a0 * (1 - d) + a1 * d));
						
						canvas.drawLine(ptPrev.x, ptPrev.y, pt.x, pt.y, p);
					}
					
					if (a >= end)
						break;
					
					float next = a + 6f;
					a = next > end ? end : next;
					ptPrev = pt;
				}
			}
			
			public void drawCaption(float elevation, String text, boolean reverseSide, Canvas canvas, Paint paint) {
				Rect bounds = new Rect();
				paint.getTextBounds(text, 0, text.length(), bounds);
				RectF rc = new RectF(bounds);

				PointF pt = getPointRelative(elevation, false);
				if (reverseSide) {
					pt.x = -pt.x;
					pt.y = -pt.y;
				}
				
				float dx = Math.signum(pt.x);
				float dy = Math.signum(pt.y);
				
				final float POINT_TO_CAPTION = 4f;
				float x = pt.x + dx * POINT_TO_CAPTION;
				float y = pt.y + dy * POINT_TO_CAPTION;

				rc.offsetTo(mOrigin.x + x, mOrigin.y + y);
				rc.offset(dx < 0 ? -(bounds.right - bounds.left) : 0, dy > 0 ? bounds.bottom - bounds.top : 0);
				
				canvas.drawText(text, 0, text.length(), rc.left, rc.top, paint);
			}
			
			final float AZ_BASE = 160f;
			
			float mX, mY;
			float mAzimuth;
		}	

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			mWidth = MeasureSpec.getSize(widthMeasureSpec);
			mHeight = mWidth * 3 / 4 + 40;
            float u = mWidth / 2;
            mRadHorz = u * .9f;
            mRadVert = mRadHorz / 2;
            mOrigin = new PointF(u, mHeight - u / 2);

            setMeasuredDimension(mWidth, mHeight);
		}
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	public void onSensorChanged(SensorEvent event) {
		if (Math.abs(mNorth - event.values[0]) > 4f) {
			mNorth = event.values[0];
			mCanvasView.invalidate();
			mMapView.invalidate();
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	class EffectOverlay extends Overlay {

		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {
			Location last = getLast();
			if (last == null)
				return;
			
			Projection proj = mapView.getProjection();
			Point pt = proj.toPixels(GeoUtils.getGeoPoint(last), null);
			
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setStyle(Style.FILL);
			
			Paint fill = new Paint();
			fill.setStyle(Style.FILL);
			fill.setColor(0x88000000);

			if (shadow) {
				float r = proj.metersToEquatorPixels(last.getAccuracy());
				paint.setColor(0x5500ddff);
				canvas.drawCircle(pt.x, pt.y, r, paint);
				
				pt.x++;
				pt.y++;
			} else {
				pt.x--;
				pt.y--;
			}
						
			final float SYMBOL_SIZE = 8f;
			final float BORDER_W = 2;

			Path path = new Path();
			path.addArc(new RectF(-SYMBOL_SIZE, -SYMBOL_SIZE, SYMBOL_SIZE, SYMBOL_SIZE), 180, 180);
			path.moveTo(SYMBOL_SIZE, 0);
			path.lineTo(0, SYMBOL_SIZE * 2);
			path.lineTo(-SYMBOL_SIZE, 0);
			Matrix mat = new Matrix();
			mat.setRotate(mNorth + 180f);
			mat.postTranslate(pt.x, pt.y);
			path.transform(mat);
			paint.setARGB(0xff, shadow ? 0 : 0x88, 0, 0);
			canvas.drawPath(path, paint);
			
			if (!shadow) {
				String cap = String.format("%.1fm", last.getAccuracy());
				Rect bounds = new Rect();
				paint.setTextSize(20);
				paint.getTextBounds(cap, 0, cap.length(), bounds);
				float w = bounds.width() + BORDER_W * 2;
				float h = bounds.height() + BORDER_W * 2;
				float x = pt.x - w / 2;
				float y = pt.y + SYMBOL_SIZE * 3;
				canvas.drawRect(x, y, x + w, y + h, fill);
				paint.setColor(Color.WHITE);
				canvas.drawText(cap, x + BORDER_W, y + h - BORDER_W, paint);
			}
			
			if (mAddress != null) {
				int vw = mapView.getWidth();
				int vh = mapView.getHeight();
				
				Rect bounds = new Rect();
				paint.getTextBounds(mAddress, 0, mAddress.length(), bounds);

				float tw = bounds.width() + BORDER_W * 2;
				float th = bounds.height() + BORDER_W * 2;
				canvas.drawRect(vw - tw, vh - th, vw, vh, fill);
				paint.setColor(Color.WHITE);
				canvas.drawText(mAddress, vw - tw, vh - BORDER_W, paint);
			}
		}
	}
}
