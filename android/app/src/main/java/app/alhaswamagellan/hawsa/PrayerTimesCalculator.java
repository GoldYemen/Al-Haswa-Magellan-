package app.alhaswamagellan.hawsa;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Calculates 5 Azan (Adhan) prayer times and Qibla bearing using GPS coordinates.
 * Uses Muslim World League (MWL) method: Fajr 18°, Isha 17°.
 * Time format uses Arabic ص/م (AM/PM).
 */
public class PrayerTimesCalculator
{
  private static double latitude = 19.4431;
  private static double longitude = 40.5167;
  private static double timezoneOffset = 3.0; // UTC+3 for Yemen

  // Kaaba coordinates
  private static final double KAABA_LAT = 21.4225;
  private static final double KAABA_LON = 39.8262;

  // MWL angles
  private static final double FAJR_ANGLE = 18.0;
  private static final double ISHA_ANGLE = 17.0;
  private static final double ISHA_MIDNIGHT_ANGLE = 0.0; // Not used - use fraction of night

  public static class PrayerTimes
  {
    public double fajr;
    public double sunrise;
    public double dhuhr;
    public double asr;
    public double maghrib;
    public double isha;

    public String getFajrStr()
    {
      return formatToArabicAmPm(fajr);
    }
    public String getDhuhrStr()
    {
      return formatToArabicAmPm(dhuhr);
    }
    public String getAsrStr()
    {
      return formatToArabicAmPm(asr);
    }
    public String getMaghribStr()
    {
      return formatToArabicAmPm(maghrib);
    }
    public String getIshaStr()
    {
      return formatToArabicAmPm(isha);
    }

    /**
     * Format decimal hour to Arabic AM/PM notation.
     * Uses ص (صباحاً = morning) and م (مساءً = evening).
     */
    private static String formatToArabicAmPm(double decimalHour)
    {
      if (decimalHour < 0 || decimalHour >= 24)
        return "--:--";
      int hour24 = (int) decimalHour;
      int minute = (int) Math.round((decimalHour - hour24) * 60);
      if (minute == 60)
      {
        minute = 0;
        hour24++;
      }
      if (hour24 == 24)
        hour24 = 0;

      int hour12 = hour24 % 12;
      if (hour12 == 0)
        hour12 = 12;

      String ampm;
      if (hour24 < 12)
      {
        ampm = "\u0635"; // ص = صباحاً
      }
      else
      {
        ampm = "\u0645"; // م = مساءً
      }

      return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, ampm);
    }
  }

  public static void setLocation(double lat, double lon, double tzOffset)
  {
    latitude = lat;
    longitude = lon;
    timezoneOffset = tzOffset;
  }

  public static double getLatitude()
  {
    return latitude;
  }
  public static double getLongitude()
  {
    return longitude;
  }

  /**
   * Calculate Qibla bearing from current location to Kaaba.
   * Returns bearing in degrees from North (clockwise).
   */
  public static double calculateQiblaBearing()
  {
    double latRad = Math.toRadians(latitude);
    double kaabaLatRad = Math.toRadians(KAABA_LAT);
    double deltaLon = Math.toRadians(KAABA_LON - longitude);

    double x = Math.sin(deltaLon) * Math.cos(kaabaLatRad);
    double y = Math.cos(latRad) * Math.sin(kaabaLatRad) - Math.sin(latRad) * Math.cos(kaabaLatRad) * Math.cos(deltaLon);

    double bearing = Math.toDegrees(Math.atan2(x, y));
    return (bearing + 360) % 360;
  }

  public static PrayerTimes getTodayPrayerTimes()
  {
    Calendar cal = Calendar.getInstance(TimeZone.getDefault());
    int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
    int year = cal.get(Calendar.YEAR);
    return calculatePrayerTimes(dayOfYear, year);
  }

  public static PrayerTimes calculatePrayerTimes(int dayOfYear, int year)
  {
    PrayerTimes times = new PrayerTimes();

    // Calculate Julian Date
    double jd = julianDate(year, dayOfYear);

    // Solar declination and equation of time
    double D = jd - 2451545.0;
    double g = Math.toRadians(357.529 + 0.98560028 * D);
    double q = Math.toRadians(280.459 + 0.98564736 * D);
    double L = q + Math.toRadians(1.915 * Math.sin(g) + 0.020 * Math.sin(2 * g));

    double e = q - L; // Equation of center approximation
    double equationOfTime = 0.0;
    {
      // Better equation of time
      double B = Math.toRadians((360.0 / 365.24) * (dayOfYear - 81));
      equationOfTime = 9.87 * Math.sin(2 * B) - 7.53 * Math.cos(B) - 1.5 * Math.sin(B);
    }

    double declination = Math.asin(Math.sin(Math.toRadians(23.4397)) * Math.sin(L));

    // Sun's hour angle at various twilights
    double latRad = Math.toRadians(latitude);

    // Fajr (sun angle -18°)
    double fajrAngle = Math.toRadians(-FAJR_ANGLE);
    double fajrCosHA =
        (Math.sin(fajrAngle) - Math.sin(latRad) * Math.sin(declination)) / (Math.cos(latRad) * Math.cos(declination));
    double fajrHA = Math.toDegrees(Math.acos(Math.min(1, Math.max(-1, fajrCosHA))));

    // Isha (sun angle -17°)
    double ishaAngle = Math.toRadians(-ISHA_ANGLE);
    double ishaCosHA =
        (Math.sin(ishaAngle) - Math.sin(latRad) * Math.sin(declination)) / (Math.cos(latRad) * Math.cos(declination));
    double ishaHA = Math.toDegrees(Math.acos(Math.min(1, Math.max(-1, ishaCosHA))));

    // Sunrise (sun angle -0.8333° accounting for refraction)
    double riseAngle = Math.toRadians(-0.8333);
    double riseCosHA =
        (Math.sin(riseAngle) - Math.sin(latRad) * Math.sin(declination)) / (Math.cos(latRad) * Math.cos(declination));
    double riseHA = Math.toDegrees(Math.acos(Math.min(1, Math.max(-1, riseCosHA))));

    // Solar noon
    double solarNoon = 12 - longitude / 15 + timezoneOffset - equationOfTime / 60.0;

    // Calculate times
    times.fajr = solarNoon - fajrHA / 15.0;
    times.sunrise = solarNoon - riseHA / 15.0;
    times.dhuhr = solarNoon + 0.0167; // Small addition for safety margin
    times.maghrib = solarNoon + riseHA / 15.0;
    times.isha = solarNoon + ishaHA / 15.0;

    // Asr (Shafi'i: shadow = object + shadow at noon)
    double asrAngle = Math.atan(1.0 / (1.0 + Math.abs(Math.tan(Math.abs(latRad - declination)))));
    if (latitude < declination)
    {
      asrAngle = Math.atan(1.0 / (1.0 - Math.abs(Math.tan(Math.abs(latRad - declination)))));
    }
    double asrCosHA =
        (Math.sin(asrAngle) - Math.sin(latRad) * Math.sin(declination)) / (Math.cos(latRad) * Math.cos(declination));
    double asrHA = Math.toDegrees(Math.acos(Math.min(1, Math.max(-1, asrCosHA))));
    times.asr = solarNoon + asrHA / 15.0;

    // Handle extreme latitudes
    if (fajrCosHA > 1 || fajrCosHA < -1)
    {
      times.fajr = times.sunrise - 1.0; // Fallback: 1 hour before sunrise
    }
    if (ishaCosHA > 1 || ishaCosHA < -1)
    {
      times.isha = times.maghrib + 1.0; // Fallback: 1 hour after maghrib
    }

    return times;
  }

  private static double julianDate(int year, int dayOfYear)
  {
    int a = (year - 1) / 100;
    int b = 2 - a + (a / 4);
    return 365.25 * (year + 4716) + 30.6001 * 14 + dayOfYear + b - 1524.5;
  }
}
