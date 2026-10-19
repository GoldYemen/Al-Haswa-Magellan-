package app.alhaswamagellan.hawsa;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Moon phase calculator for Al-Haswa
 * Uses synodic month calculation for accurate phase prediction.
 * Moonrise/moonset times use astronomical algorithms based on
 * Meeus / NOAA methodology for accurate transit calculations.
 */
public class MoonCalculator
{
  // Al-Haswa coordinates
  private static final double LAT = 12.7794;
  private static final double LON = 45.0367;
  private static final double TIMEZONE_OFFSET = 3.0; // Asia/Riyadh = UTC+3

  // Known new moon: Jan 6 2000 18:14 UTC
  private static final long REF_NEW_MOON = 947182440000L;
  static final double SYNODIC_MONTH = 29.53058867;

  // ---- Phase calculations (unchanged) ----

  /**
   * Get lunar age (0 to ~29.53 days)
   */
  public static double getLunarAge(long timeMillis)
  {
    double daysSince = (timeMillis - REF_NEW_MOON) / 86400000.0;
    double age = daysSince % SYNODIC_MONTH;
    if (age < 0)
      age += SYNODIC_MONTH;
    return age;
  }

  /**
   * Get illumination fraction (0.0 to 1.0)
   */
  public static double getIllumination(long timeMillis)
  {
    double age = getLunarAge(timeMillis);
    return 0.5 * (1.0 - Math.cos(2.0 * Math.PI * age / SYNODIC_MONTH));
  }

  /**
   * Get phase name in Arabic
   */
  public static String getPhaseNameArabic(double age)
  {
    if (age < 1.85)
      return "محاق";
    if (age < 5.53)
      return "هلال متزايد";
    if (age < 9.22)
      return "تربيع أول";
    if (age < 12.91)
      return "أحدب متزايد";
    if (age < 16.61)
      return "بدر";
    if (age < 20.30)
      return "أحدب متناقص";
    if (age < 23.99)
      return "تربيع أخير";
    if (age < 27.68)
      return "هلال متناقص";
    return "محاق";
  }

  /**
   * Get phase index (0-7) for drawing
   */
  public static int getPhaseIndex(double age)
  {
    if (age < 1.85)
      return 0;
    if (age < 5.53)
      return 1;
    if (age < 9.22)
      return 2;
    if (age < 12.91)
      return 3;
    if (age < 16.61)
      return 4;
    if (age < 20.30)
      return 5;
    if (age < 23.99)
      return 6;
    if (age < 27.68)
      return 7;
    return 0;
  }

  // ---- Astronomical moonrise/moonset (Meeus/NOAA) ----

  /**
   * Convert degrees to radians
   */
  private static double deg2rad(double d)
  {
    return d * Math.PI / 180.0;
  }

  /**
   * Convert radians to degrees
   */
  private static double rad2deg(double r)
  {
    return r * 180.0 / Math.PI;
  }

  /**
   * Normalize angle to 0-360 range
   */
  private static double normalize(double angle)
  {
    angle = angle % 360.0;
    if (angle < 0)
      angle += 360.0;
    return angle;
  }

  /**
   * Calculate Julian Day Number from Calendar
   */
  private static double toJulianDay(Calendar cal)
  {
    int year = cal.get(Calendar.YEAR);
    int month = cal.get(Calendar.MONTH) + 1;
    int day = cal.get(Calendar.DAY_OF_MONTH);
    double hour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60.0 + cal.get(Calendar.SECOND) / 3600.0;

    if (month <= 2)
    {
      year--;
      month += 12;
    }

    int A = (int) Math.floor(year / 100.0);
    int B = 2 - A + (int) Math.floor(A / 4.0);

    return Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + hour / 24.0 + B - 1524.5;
  }

  /**
   * Calculate moon's equatorial coordinates for a given Julian Day.
   * Based on Meeus "Astronomical Algorithms" Chapter 47 (simplified).
   */
  private static class MoonCoords
  {
    double ra; // right ascension in degrees
    double dec; // declination in degrees
  }

  private static MoonCoords getMoonCoords(double JD)
  {
    double T = (JD - 2451545.0) / 36525.0; // Julian centuries from J2000.0

    // Mean arguments (Meeus Ch. 47)
    double L0 =
        218.3164477 + 481267.88123421 * T - 0.0015786 * T * T + T * T * T / 538841.0 - T * T * T * T / 65194000.0;
    double M = 134.9633964 + 477198.8675055 * T + 0.0087414 * T * T + T * T * T / 69699.0 - T * T * T * T / 14712000.0;
    double F =
        93.2720950 + 483202.0175233 * T - 0.0036539 * T * T - T * T * T / 3526000.0 + T * T * T * T / 863310000.0;

    double D =
        297.8501921 + 445267.1114034 * T - 0.0018819 * T * T + T * T * T / 545868.0 - T * T * T * T / 113065000.0;

    // Longitude with principal terms
    double l = L0 + 6.289 * Math.sin(deg2rad(M)) + 1.274 * Math.sin(deg2rad(2 * D - M))
             + 0.658 * Math.sin(deg2rad(2 * D)) + 0.214 * Math.sin(deg2rad(2 * M))
             - 0.186 * Math.sin(deg2rad(M - 2 * D + 180)) - 0.114 * Math.sin(deg2rad(M + 2 * F))
             + 0.059 * Math.sin(deg2rad(2 * D - 2 * M)) + 0.057 * Math.sin(deg2rad(2 * D + M - 2 * F))
             + 0.053 * Math.sin(deg2rad(2 * D + M)) + 0.046 * Math.sin(deg2rad(M - 2 * D))
             + 0.041 * Math.sin(deg2rad(M - D)) - 0.035 * Math.sin(deg2rad(D)) - 0.031 * Math.sin(deg2rad(M + 2 * F))
             - 0.015 * Math.sin(deg2rad(2 * F - 2 * D)) + 0.011 * Math.sin(deg2rad(M - 4 * D));

    // Latitude with principal terms
    double b = 5.128 * Math.sin(deg2rad(F)) + 0.281 * Math.sin(deg2rad(M + F)) + 0.278 * Math.sin(deg2rad(M - F))
             + 0.173 * Math.sin(deg2rad(2 * D - F)) + 0.055 * Math.sin(deg2rad(M + 2 * D - F))
             + 0.046 * Math.sin(deg2rad(2 * M + F)) - 0.046 * Math.sin(deg2rad(2 * D + F))
             - 0.015 * Math.sin(deg2rad(2 * M - F)) + 0.011 * Math.sin(deg2rad(M - 2 * D + F));

    // Obliquity of ecliptic
    double eps = 23.439291 - 0.0130042 * T;

    // Convert ecliptic to equatorial
    double ra =
        Math.atan2(Math.sin(deg2rad(l)) * Math.cos(deg2rad(eps)) - Math.tan(deg2rad(b)) * Math.sin(deg2rad(eps)),
                   Math.cos(deg2rad(l)));
    ra = rad2deg(ra);
    ra = normalize(ra);

    double dec = Math.asin(Math.sin(deg2rad(b)) * Math.cos(deg2rad(eps))
                           + Math.cos(deg2rad(b)) * Math.sin(deg2rad(eps)) * Math.sin(deg2rad(l)));
    dec = rad2deg(dec);

    MoonCoords c = new MoonCoords();
    c.ra = ra;
    c.dec = dec;
    return c;
  }

  /**
   * Calculate approximate sidereal time at Greenwich for JD (in degrees)
   */
  private static double greenwichSiderealTime(double JD)
  {
    double T = (JD - 2451545.0) / 36525.0;
    double theta0 = 280.46061837 + 360.98564736629 * (JD - 2451545.0) + 0.000387933 * T * T - T * T * T / 38710000.0;
    return normalize(theta0);
  }

  /**
   * Calculate hour angle of the moon at a given moment.
   * Returns hour angle in degrees.
   */
  private static double getHourAngle(double JD, double ra, double lon)
  {
    double gst = greenwichSiderealTime(JD);
    double lst = gst + lon; // local sidereal time in degrees
    if (lst < 0)
      lst += 360;
    if (lst >= 360)
      lst -= 360;
    return lst - ra; // hour angle
  }

  /**
   * Calculate altitude of the moon given hour angle, declination, latitude.
   * Returns altitude in degrees.
   */
  private static double getAltitude(double ha, double dec, double lat)
  {
    double haRad = deg2rad(ha);
    double decRad = deg2rad(dec);
    double latRad = deg2rad(lat);
    double alt = Math.asin(Math.sin(latRad) * Math.sin(decRad) + Math.cos(latRad) * Math.cos(decRad) * Math.cos(haRad));
    return rad2deg(alt);
  }

  /**
   * Find the transit (upper culmination) time for the moon on a given date.
   * This is when the moon crosses the meridian (highest point).
   * Returns hours in local time (0-24), or NaN if not found.
   */
  private static double findLunarTransit(Calendar date)
  {
    // Search for when hour angle ≈ 0 (meridian transit)
    Calendar cal = (Calendar) date.clone();
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    double JD0 = toJulianDay(cal);

    // Search through 24 hours in small steps
    double bestTime = Double.NaN;
    double bestHA = 999;

    for (double h = 0; h <= 24.0; h += 0.05)
    {
      double JD = JD0 + h / 24.0;
      MoonCoords mc = getMoonCoords(JD);
      double ha = getHourAngle(JD, mc.ra, LON);
      // Normalize hour angle to -180..180
      if (ha > 180)
        ha -= 360;
      if (ha < -180)
        ha += 360;

      if (Math.abs(ha) < Math.abs(bestHA))
      {
        bestHA = ha;
        bestTime = h;
      }
    }

    return bestTime; // hours from midnight UTC
  }

  /**
   * Find when the moon reaches a given altitude (typically -0.833 for rise/set
   * accounting for standard refraction + semi-diameter).
   * Searches around the transit time.
   * Returns UTC hours from midnight, or NaN if not found that day.
   */
  private static double findAltitudeCrossing(Calendar date, double targetAlt, boolean rising)
  {
    Calendar cal = (Calendar) date.clone();
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    double JD0 = toJulianDay(cal);

    // Find transit first
    double transitH = findLunarTransit(date);
    if (Double.isNaN(transitH))
      return Double.NaN;

    // Search before transit for rising, after transit for setting
    double startH, endH;
    if (rising)
    {
      startH = transitH - 12;
      endH = transitH;
    }
    else
    {
      startH = transitH;
      endH = transitH + 12;
    }

    // Clamp to day range
    if (startH < -2)
      startH = -2;
    if (endH > 26)
      endH = 26;

    double lastAlt = Double.NaN;
    double lastH = Double.NaN;
    boolean crossed = false;
    double crossH = Double.NaN;

    for (double h = startH; h <= endH; h += 0.02)
    {
      double JD = JD0 + h / 24.0;
      MoonCoords mc = getMoonCoords(JD);
      double ha = getHourAngle(JD, mc.ra, LON);
      double alt = getAltitude(ha, mc.dec, LAT);

      if (!Double.isNaN(lastAlt))
      {
        if (rising && lastAlt < targetAlt && alt >= targetAlt)
        {
          crossed = true;
          // Linear interpolation for more precision
          double fraction = (targetAlt - lastAlt) / (alt - lastAlt);
          crossH = lastH + fraction * (h - lastH);
          break;
        }
        else if (!rising && lastAlt > targetAlt && alt <= targetAlt)
        {
          crossed = true;
          double fraction = (targetAlt - lastAlt) / (alt - lastAlt);
          crossH = lastH + fraction * (h - lastH);
          break;
        }
      }
      lastAlt = alt;
      lastH = h;
    }

    return crossH; // UTC hours from midnight (can be negative or >24)
  }

  /**
   * Get astronomically accurate moonrise time.
   * Returns formatted string in Arabic locale with ص/م.
   */
  public static String getMoonriseTime(long timeMillis)
  {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
    cal.setTimeInMillis(timeMillis);

    // Check today
    double riseH = findAltitudeCrossing(cal, -0.833, true);

    // If not found for today, check previous day (moon may rise before midnight)
    if (Double.isNaN(riseH))
    {
      Calendar prev = (Calendar) cal.clone();
      prev.add(Calendar.DAY_OF_MONTH, -1);
      riseH = findAltitudeCrossing(prev, -0.833, true);
      if (!Double.isNaN(riseH))
        riseH += 24.0; // shift to next day reference
    }

    if (Double.isNaN(riseH))
      return "--:--";

    // Convert UTC hours to local hours
    double localH = riseH + TIMEZONE_OFFSET;
    if (localH < 0)
      localH += 24;
    if (localH >= 48)
      localH -= 24;
    if (localH >= 24)
      localH -= 24;

    return formatHours(localH);
  }

  /**
   * Get astronomically accurate moonset time.
   * Returns formatted string in Arabic locale with ص/م.
   */
  public static String getMoonsetTime(long timeMillis)
  {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
    cal.setTimeInMillis(timeMillis);

    // Check today
    double setH = findAltitudeCrossing(cal, -0.833, false);

    // If not found for today, check next day (moon may set after midnight)
    if (Double.isNaN(setH))
    {
      Calendar next = (Calendar) cal.clone();
      next.add(Calendar.DAY_OF_MONTH, 1);
      setH = findAltitudeCrossing(next, -0.833, false);
      if (!Double.isNaN(setH))
        setH += 24.0;
    }

    if (Double.isNaN(setH))
      return "--:--";

    double localH = setH + TIMEZONE_OFFSET;
    if (localH < 0)
      localH += 24;
    if (localH >= 48)
      localH -= 24;
    if (localH >= 24)
      localH -= 24;

    return formatHours(localH);
  }

  /**
   * Format decimal hours as "h:mm ص" or "h:mm م"
   */
  private static String formatHours(double hours)
  {
    if (Double.isNaN(hours) || hours < 0 || hours >= 24)
      return "--:--";
    int h12 = (int) hours;
    if (h12 == 0)
      h12 = 12;
    else if (h12 > 12)
      h12 -= 12;
    int minute = (int) Math.round((hours - (int) hours) * 60.0);
    if (minute >= 60)
    {
      minute = 0;
      h12++;
      if (h12 > 12)
        h12 = 1;
    }
    String ampm = hours < 12.0 ? "ص" : "م";
    return String.format("%d:%02d %s", h12 == 0 ? 12 : h12, minute, ampm);
  }

  /**
   * Format time as hh:mm AM/PM using a Calendar
   */
  public static String formatTimeAmPm(Calendar cal)
  {
    int hour = cal.get(Calendar.HOUR);
    if (hour == 0)
      hour = 12;
    int minute = cal.get(Calendar.MINUTE);
    String ampm = cal.get(Calendar.AM_PM) == Calendar.AM ? "ص" : "م";
    return String.format("%d:%02d %s", hour, minute, ampm);
  }

  /**
   * Get 7-day moon predictions
   */
  public static List<MoonDayInfo> getWeeklyMoonInfo()
  {
    List<MoonDayInfo> info = new ArrayList<>();
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
    cal.set(Calendar.HOUR_OF_DAY, 12);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);

    for (int d = 0; d < 7; d++)
    {
      MoonDayInfo day = new MoonDayInfo();
      day.date = (Calendar) cal.clone();
      day.age = getLunarAge(cal.getTimeInMillis());
      day.illumination = getIllumination(cal.getTimeInMillis());
      day.phaseName = getPhaseNameArabic(day.age);
      day.phaseIndex = getPhaseIndex(day.age);
      day.moonrise = getMoonriseTime(cal.getTimeInMillis());
      day.moonset = getMoonsetTime(cal.getTimeInMillis());
      info.add(day);

      cal.add(Calendar.DAY_OF_MONTH, 1);
    }
    return info;
  }

  public static class MoonDayInfo
  {
    public Calendar date;
    public double age;
    public double illumination;
    public String phaseName;
    public int phaseIndex;
    public String moonrise;
    public String moonset;
  }
}
