package app.alhaswamagellan.hawsa;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import app.alhaswamagellan.R;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Compass & Qibla activity with 5 Azan times.
 * Modern immersive design. GPS-based location with saved fallback.
 * Status bar is visible (not immersive) matching the dark navy background.
 * Qibla direction shown prominently with arrow and Kaaba symbol.
 */
public class HawsaCompassActivity extends AppCompatActivity
{
  private CompassView compassView;
  private ImageView btnBack;
  private TextView tvQiblaLabel;
  private TextView tvQiblaDegrees;
  private LinearLayout prayerTimesContainer;

  private LocationManager locationManager;
  private static final int LOCATION_PERMISSION_REQUEST = 1001;
  private static final String PREFS_NAME = "hawsa_compass_prefs";
  private static final String KEY_LAT = "last_lat";
  private static final String KEY_LON = "last_lon";
  private static final String KEY_TZ = "last_tz";

  private boolean locationAcquired = false;

  // GPS dialog reference
  private AlertDialog gpsDialog = null;

  // Theme colors
  private static final int COLOR_BG = 0xFF0D1B2A;
  private static final int COLOR_CARD = 0xFF111827;
  private static final int COLOR_GOLD = 0xFFD4AF37;
  private static final int COLOR_TEXT = 0xFFE0E1DD;
  private static final int COLOR_TEXT_DIM = 0xFF778DA9;
  private static final int COLOR_SEPARATOR = 0xFF1E293B;

  // Only 5 Azan prayers (NO Sunrise)
  private static final String[] PRAYER_NAMES_AR = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};

  private static final int[] PRAYER_COLORS = {
      0xFF4FC3F7, // Fajr - light blue
      0xFFFFD54F, // Dhuhr - gold
      0xFFFF8A65, // Asr - deep orange
      0xFFFF5722, // Maghrib - red-orange
      0xFF7C4DFF // Isha - purple
  };

  private static final int[] PRAYER_ICON_RES = {R.drawable.ic_hawsa_prayer_fajr, R.drawable.ic_hawsa_prayer_dhuhr,
                                                R.drawable.ic_hawsa_prayer_asr, R.drawable.ic_hawsa_prayer_maghrib,
                                                R.drawable.ic_hawsa_prayer_isha};

  private final LocationListener locationListener = new LocationListener() {
    @Override
    public void onLocationChanged(@NonNull Location location)
    {
      saveLocation(location);
      locationAcquired = true;
      updateFromLocation(location.getLatitude(), location.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {}

    @Override
    public void onProviderEnabled(String provider)
    {
      requestLocationUpdates();
      dismissGpsDialog();
    }

    @Override
    public void onProviderDisabled(String provider)
    {
      showGPSPrompt();
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    // === Status bar VISIBLE with matching color (not immersive/hidden) ===
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
    {
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
      getWindow().setStatusBarColor(COLOR_BG);
      getWindow().setNavigationBarColor(COLOR_BG);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
      {
        View decor = getWindow().getDecorView();
        int flags = decor.getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        decor.setSystemUiVisibility(flags);
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
      {
        getWindow().setDecorFitsSystemWindows(true);
      }
    }

    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

    // Build the layout programmatically
    ScrollView scrollView = new ScrollView(this);
    scrollView.setBackgroundColor(COLOR_BG);
    scrollView.setFillViewport(true);

    LinearLayout rootLayout = new LinearLayout(this);
    rootLayout.setOrientation(LinearLayout.VERTICAL);
    rootLayout.setBackgroundColor(COLOR_BG);
    rootLayout.setPadding(dp(20), dp(12), dp(20), dp(30));
    rootLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    rootLayout.setClipToPadding(false);

    // === Back button + Title ===
    LinearLayout topBar = new LinearLayout(this);
    topBar.setOrientation(LinearLayout.HORIZONTAL);
    topBar.setGravity(Gravity.CENTER_VERTICAL);
    topBar.setPadding(0, 0, 0, dp(12));

    btnBack = new ImageView(this);
    btnBack.setImageResource(R.drawable.ic_hawsa_back);
    btnBack.setColorFilter(COLOR_GOLD);
    btnBack.setPadding(dp(4), dp(4), dp(4), dp(4));
    LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(40), dp(40));
    btnBack.setLayoutParams(backParams);
    btnBack.setOnClickListener(v -> {
      finish();
      overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    });

    TextView tvTitle = new TextView(this);
    tvTitle.setText("البوصلة والقبلة");
    tvTitle.setTextSize(22);
    tvTitle.setTextColor(COLOR_GOLD);
    tvTitle.setTypeface(null, Typeface.BOLD);
    LinearLayout.LayoutParams titleParams =
        new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    tvTitle.setLayoutParams(titleParams);
    tvTitle.setGravity(Gravity.CENTER);

    View spacer = new View(this);
    spacer.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));

    topBar.addView(btnBack);
    topBar.addView(tvTitle);
    topBar.addView(spacer);
    rootLayout.addView(topBar);

    // === Compass View ===
    compassView = new CompassView(this);
    LinearLayout.LayoutParams compassParams =
        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(340));
    compassView.setLayoutParams(compassParams);
    rootLayout.addView(compassView);

    // === Qibla Direction Card ===
    LinearLayout qiblaCard = createCard();
    qiblaCard.setGravity(Gravity.CENTER);

    // Qibla title
    TextView tvQiblaTitle = new TextView(this);
    tvQiblaTitle.setText("اتجاه القبلة");
    tvQiblaTitle.setTextSize(18);
    tvQiblaTitle.setTextColor(COLOR_GOLD);
    tvQiblaTitle.setTypeface(null, Typeface.BOLD);
    tvQiblaTitle.setGravity(Gravity.CENTER);
    tvQiblaTitle.setPadding(0, dp(8), 0, dp(4));
    qiblaCard.addView(tvQiblaTitle);

    // Qibla arrow (custom view with Kaaba symbol)
    View qiblaArrowView = new View(this) {
      private double qiblaDeg = 0;
      private String qiblaDir = "";

      public View init(double deg, String dir)
      {
        this.qiblaDeg = deg;
        this.qiblaDir = dir;
        return this;
      }
    };
    // We'll update the Qibla info after location is acquired
    tvQiblaLabel = new TextView(this);
    tvQiblaLabel.setText("جارٍ تحديد الموقع...");
    tvQiblaLabel.setTextSize(14);
    tvQiblaLabel.setTextColor(COLOR_TEXT_DIM);
    tvQiblaLabel.setGravity(Gravity.CENTER);
    tvQiblaLabel.setPadding(0, dp(4), 0, dp(4));
    qiblaCard.addView(tvQiblaLabel);

    tvQiblaDegrees = new TextView(this);
    tvQiblaDegrees.setText("");
    tvQiblaDegrees.setTextSize(24);
    tvQiblaDegrees.setTextColor(COLOR_GOLD);
    tvQiblaDegrees.setTypeface(null, Typeface.BOLD);
    tvQiblaDegrees.setGravity(Gravity.CENTER);
    tvQiblaDegrees.setPadding(0, dp(2), 0, dp(4));
    qiblaCard.addView(tvQiblaDegrees);

    // Qibla Kaaba mini-icon view
    View kaabaIcon = new View(this) {
      @Override
      protected void onDraw(Canvas canvas)
      {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float size = Math.min(w, h) / 2f - dp(4);

        // Kaaba body
        Paint kaabaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        kaabaPaint.setColor(COLOR_GOLD);
        kaabaPaint.setStyle(Paint.Style.FILL);
        RectF kaabaRect = new RectF(cx - size, cy - size, cx + size, cy + size);
        canvas.drawRect(kaabaRect, kaabaPaint);

        // Kaaba stripe (kiswah gold stripe)
        Paint stripePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stripePaint.setColor(0xFFB8860B);
        stripePaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(new RectF(cx - size, cy - dp(2), cx + size, cy + dp(2)), stripePaint);

        // Kaaba door
        Paint doorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        doorPaint.setColor(0xFF8B6914);
        doorPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(new RectF(cx - dp(4), cy + dp(2), cx + dp(4), cy + size - dp(2)), doorPaint);

        // Border
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(0xFF8B6914);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRect(kaabaRect, borderPaint);
      }
    };
    int iconSize = dp(50);
    LinearLayout.LayoutParams kaabaParams = new LinearLayout.LayoutParams(iconSize, iconSize);
    kaabaParams.gravity = Gravity.CENTER;
    kaabaParams.setMargins(0, dp(4), 0, dp(8));
    kaabaIcon.setLayoutParams(kaabaParams);
    qiblaCard.addView(kaabaIcon);

    // Qibla direction hint
    TextView tvQiblaHint = new TextView(this);
    tvQiblaHint.setText("وجّه هاتفك نحو اتجاه القبلة المشار إليه بالسهم الذهبي على البوصلة");
    tvQiblaHint.setTextSize(12);
    tvQiblaHint.setTextColor(COLOR_TEXT_DIM);
    tvQiblaHint.setGravity(Gravity.CENTER);
    tvQiblaHint.setPadding(dp(8), 0, dp(8), dp(8));
    qiblaCard.addView(tvQiblaHint);

    rootLayout.addView(qiblaCard);

    // === Prayer Times Card ===
    LinearLayout prayerCard = createCard();
    prayerCard.setPadding(dp(20), dp(16), dp(20), dp(16));

    TextView tvPrayerTitle = new TextView(this);
    tvPrayerTitle.setText("أوقات الأذان");
    tvPrayerTitle.setTextSize(18);
    tvPrayerTitle.setTextColor(COLOR_GOLD);
    tvPrayerTitle.setTypeface(null, Typeface.BOLD);
    tvPrayerTitle.setPadding(0, 0, 0, dp(12));
    prayerCard.addView(tvPrayerTitle);

    prayerTimesContainer = new LinearLayout(this);
    prayerTimesContainer.setOrientation(LinearLayout.VERTICAL);
    prayerCard.addView(prayerTimesContainer);

    rootLayout.addView(prayerCard);

    // === Designer credit ===
    TextView tvCredit = new TextView(this);
    tvCredit.setText("تم تصميم هذا التطبيق من قبل أصيل صادق");
    tvCredit.setTextSize(10);
    tvCredit.setTextColor(0xFF415A77);
    tvCredit.setGravity(Gravity.CENTER);
    tvCredit.setPadding(0, dp(20), 0, 0);
    rootLayout.addView(tvCredit);

    scrollView.addView(rootLayout);
    setContentView(scrollView);

    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    // Try saved location first
    loadSavedLocation();

    // Check location permission and request GPS
    checkLocationAndRequest();

    buildPrayerTimes();
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    if (compassView != null)
    {
      compassView.startSensors();
    }
    if (locationAcquired)
    {
      requestLocationUpdates();
    }

    // Auto-dismiss GPS dialog if GPS was enabled while away
    if (gpsDialog != null && gpsDialog.isShowing())
    {
      if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
      {
        dismissGpsDialog();
        requestLocationUpdates();
      }
    }
  }

  @Override
  protected void onPause()
  {
    super.onPause();
    if (compassView != null)
    {
      compassView.stopSensors();
    }
    stopLocationUpdates();
  }

  private void checkLocationAndRequest()
  {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED)
    {
      ActivityCompat.requestPermissions(
          this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
          LOCATION_PERMISSION_REQUEST);
    }
    else
    {
      if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
      {
        requestLocationUpdates();
      }
      else
      {
        showGPSPrompt();
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
  {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == LOCATION_PERMISSION_REQUEST)
    {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
      {
        if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        {
          requestLocationUpdates();
        }
        else
        {
          showGPSPrompt();
        }
      }
      else
      {
        Toast.makeText(this, "يجب تفعيل إذن الموقع لدقة اتجاه القبلة", Toast.LENGTH_LONG).show();
      }
    }
  }

  @SuppressLint("MissingPermission")
  private void requestLocationUpdates()
  {
    if (locationManager == null)
      return;
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED)
    {
      try
      {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 10, locationListener);
        Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnown != null)
        {
          saveLocation(lastKnown);
          locationAcquired = true;
          updateFromLocation(lastKnown.getLatitude(), lastKnown.getLongitude());
        }
      }
      catch (Exception ignored)
      {}
    }
  }

  @SuppressLint("MissingPermission")
  private void stopLocationUpdates()
  {
    if (locationManager != null)
    {
      try
      {
        locationManager.removeUpdates(locationListener);
      }
      catch (Exception ignored)
      {}
    }
  }

  private void saveLocation(Location location)
  {
    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        .edit()
        .putFloat(KEY_LAT, (float) location.getLatitude())
        .putFloat(KEY_LON, (float) location.getLongitude())
        .putFloat(KEY_TZ, (float) TimeZone.getDefault().getRawOffset() / 3600000f)
        .apply();
  }

  private void loadSavedLocation()
  {
    float savedLat = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getFloat(KEY_LAT, Float.MIN_VALUE);
    float savedLon = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getFloat(KEY_LON, Float.MIN_VALUE);

    if (savedLat != Float.MIN_VALUE && savedLon != Float.MIN_VALUE)
    {
      locationAcquired = true;
      updateFromLocation(savedLat, savedLon);
    }
  }

  private void updateFromLocation(double lat, double lon)
  {
    double tzOffset = TimeZone.getDefault().getRawOffset() / 3600000.0;
    PrayerTimesCalculator.setLocation(lat, lon, tzOffset);

    // Calculate Qibla bearing
    double qibla = PrayerTimesCalculator.calculateQiblaBearing();
    if (compassView != null)
    {
      compassView.setQiblaBearing(qibla);
    }

    // Update Qibla direction text
    String directionAr = getQiblaDirectionArabic(qibla);
    if (tvQiblaLabel != null)
    {
      tvQiblaLabel.setText("اتجاه القبلة: " + directionAr);
      tvQiblaLabel.setTextColor(COLOR_TEXT);
    }
    if (tvQiblaDegrees != null)
    {
      tvQiblaDegrees.setText(String.format(Locale.US, "%.0f° من الشمال", qibla));
    }

    buildPrayerTimes();
  }

  private String getQiblaDirectionArabic(double bearing)
  {
    if (bearing >= 337.5 || bearing < 22.5)
      return "شمال";
    if (bearing >= 22.5 && bearing < 67.5)
      return "شمال شرق";
    if (bearing >= 67.5 && bearing < 112.5)
      return "شرق";
    if (bearing >= 112.5 && bearing < 157.5)
      return "جنوب شرق";
    if (bearing >= 157.5 && bearing < 202.5)
      return "جنوب";
    if (bearing >= 202.5 && bearing < 247.5)
      return "جنوب غرب";
    if (bearing >= 247.5 && bearing < 292.5)
      return "غرب";
    if (bearing >= 292.5 && bearing < 337.5)
      return "شمال غرب";
    return "شمال";
  }

  // GPS prompt with single-dismiss
  private void showGPSPrompt()
  {
    if (gpsDialog != null && gpsDialog.isShowing())
      return;

    gpsDialog =
        new AlertDialog.Builder(this, R.style.HawsaAlertDialog)
            .setTitle("تفعيل الموقع")
            .setMessage("يجب تفعيل خدمة الموقع (GPS) لدقة اتجاه القبلة وأوقات الأذان. هل تريد الذهاب إلى الإعدادات؟")
            .setPositiveButton("فتح الإعدادات",
                               (dialog, which) -> {
                                 try
                                 {
                                   startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                 }
                                 catch (Exception ignored)
                                 {}
                                 gpsDialog = null;
                               })
            .setNegativeButton("إلغاء", (dialog, which) -> { gpsDialog = null; })
            .setCancelable(true)
            .setOnCancelListener(dialog -> { gpsDialog = null; })
            .show();
  }

  private void dismissGpsDialog()
  {
    if (gpsDialog != null)
    {
      try
      {
        gpsDialog.dismiss();
      }
      catch (Exception ignored)
      {}
      gpsDialog = null;
    }
  }

  private void buildPrayerTimes()
  {
    if (prayerTimesContainer == null)
      return;
    prayerTimesContainer.removeAllViews();

    PrayerTimesCalculator.PrayerTimes times = PrayerTimesCalculator.getTodayPrayerTimes();
    Calendar now = Calendar.getInstance(TimeZone.getDefault());
    double currentHour = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) / 60.0;

    int nextPrayerIdx = -1;
    double[] prayerHours = {times.fajr, times.dhuhr, times.asr, times.maghrib, times.isha};

    for (int i = 0; i < 5; i++)
    {
      if (currentHour < prayerHours[i])
      {
        nextPrayerIdx = i;
        break;
      }
    }
    if (nextPrayerIdx == -1)
      nextPrayerIdx = 0;

    String[] prayerTimes = {times.getFajrStr(), times.getDhuhrStr(), times.getAsrStr(), times.getMaghribStr(),
                            times.getIshaStr()};

    for (int i = 0; i < 5; i++)
    {
      LinearLayout row = new LinearLayout(this);
      row.setOrientation(LinearLayout.HORIZONTAL);
      row.setGravity(Gravity.CENTER_VERTICAL);
      row.setPadding(dp(12), dp(10), dp(12), dp(10));

      boolean isNext = (i == nextPrayerIdx);
      if (isNext)
      {
        row.setBackgroundColor(0x20D4AF37);
      }

      // Prayer icon circle
      View iconCircle = new View(this);
      int iconSz = dp(28);
      LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSz, iconSz);
      iconParams.setMargins(0, 0, dp(12), 0);
      iconCircle.setLayoutParams(iconParams);
      iconCircle.setBackground(createCircleDrawable(PRAYER_COLORS[i]));

      // Prayer name
      TextView tvName = new TextView(this);
      tvName.setText(PRAYER_NAMES_AR[i]);
      tvName.setTextSize(16);
      tvName.setTextColor(isNext ? COLOR_GOLD : COLOR_TEXT);
      tvName.getPaint().setFakeBoldText(isNext);
      LinearLayout.LayoutParams nameParams =
          new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
      tvName.setLayoutParams(nameParams);

      // Time
      TextView tvTime = new TextView(this);
      tvTime.setText(prayerTimes[i]);
      tvTime.setTextSize(14);
      tvTime.setTextColor(isNext ? COLOR_GOLD : COLOR_TEXT_DIM);
      tvTime.getPaint().setFakeBoldText(isNext);

      if (isNext)
      {
        tvTime.append(" القادمة");
      }

      row.addView(iconCircle);
      row.addView(tvName);
      row.addView(tvTime);

      // Separator
      if (i < 4)
      {
        View separator = new View(this);
        separator.setBackgroundColor(COLOR_SEPARATOR);
        LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1);
        sepParams.setMargins(dp(48), 0, 0, 0);
        separator.setLayoutParams(sepParams);
        prayerTimesContainer.addView(row);
        prayerTimesContainer.addView(separator);
      }
      else
      {
        prayerTimesContainer.addView(row);
      }
    }
  }

  // ===== Helpers =====

  private int dp(int value)
  {
    float density = getResources().getDisplayMetrics().density;
    return (int) (value * density + 0.5f);
  }

  private LinearLayout createCard()
  {
    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(dp(20), dp(16), dp(20), dp(16));

    GradientDrawable bg = new GradientDrawable();
    bg.setColor(COLOR_CARD);
    bg.setCornerRadius(dp(16));
    bg.setStroke(dp(1), COLOR_SEPARATOR);
    card.setBackground(bg);

    LinearLayout.LayoutParams params =
        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    params.setMargins(0, dp(12), 0, 0);
    card.setLayoutParams(params);

    return card;
  }

  private GradientDrawable createCircleDrawable(int color)
  {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setShape(GradientDrawable.OVAL);
    drawable.setColor(color);
    return drawable;
  }
}
