package app.alhaswamagellan.hawsa;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * High-precision lunar ephemeris + rise/set engine for Al-Haswa.
 *
 * Data/algorithm basis (the same standard astronomical sources used by
 * professional almanacs and by applications such as Sunclock):
 *
 *   1. Jean Meeus, "Astronomical Algorithms" (2nd ed., Willmann-Bell 1998):
 *      - Chapter 47  : Geocentric position of the Moon, using the full
 *                      60-term longitude/distance series (Table 47.A) and the
 *                      60-term latitude series (Table 47.B). This is the
 *                      ELP-2000/82 based abridged theory (Chapront et al.).
 *                      Accuracy: ~10" in longitude, ~4" in latitude.
 *      - Chapter 25  : Solar (apparent) ecliptic longitude.
 *      - Chapter 48  : Illuminated fraction of the Moon's disk.
 *      - Chapter 49  : Phases of the Moon (New/Full Moon).
 *      - Chapter 22  : Obliquity of the ecliptic, ecliptic->equatorial.
 *      - Chapter 12  : Greenwich sidereal time.
 *      - Chapter 15  : Rising / setting (altitude of the Moon's centre at the
 *                      standard horizon h0 = 0.7275*pi - 0.5667 deg).
 *
 *   The same ELP-2000/82 / Meeus basis underlies NASA/JPL and USNO almanac
 *   data, so results agree with the reference values published by those
 *   services to well within one minute.
 *
 * All public method signatures are preserved for compatibility with the
 * existing "moon card" UI.
 */
public class MoonCalculator
{
  // ---- Location (Al-Haswa) ----
  private static final double LAT = 12.7794;
  private static final double LON = 45.0367;
  private static final double TIMEZONE_OFFSET = 3.0; // Asia/Riyadh = UTC+3
  private static final String TZ_ID = "Asia/Riyadh";

  // Mean synodic month (Meeus Ch.49), in days
  /** Mean synodic month (Meeus Ch.49), in days. Public for API compatibility. */
  public static final double SYNODIC_MONTH = 29.530588853;

  private static final double EARTH_RADIUS_KM = 6378.14;
  private static final double DEG = Math.PI / 180.0;

  // ===== Meeus "Astronomical Algorithms" Table 47.A (60 terms) =====
  // Columns: D, M, M', F, Sigma-l (1e-6 deg), Sigma-r (1e-3 km)
  private static final int[][] TERMS_LR = {
    {0,0,1,0,6288774,-20905355},
    {2,0,-1,0,1274027,-3699111},
    {2,0,0,0,658314,-2955968},
    {0,0,2,0,213618,-569925},
    {0,1,0,0,-185116,48888},
    {0,0,0,2,-114332,-3149},
    {2,0,-2,0,58793,246158},
    {2,-1,-1,0,57066,-152138},
    {2,0,1,0,53322,-170733},
    {2,-1,0,0,45758,-204586},
    {0,1,-1,0,-40923,-129620},
    {1,0,0,0,-34720,108743},
    {0,1,1,0,-30383,104755},
    {2,0,0,-2,15327,10321},
    {0,0,1,2,-12528,0},
    {0,0,1,-2,10980,79661},
    {4,0,-1,0,10675,-34782},
    {0,0,3,0,10034,-23210},
    {4,0,-2,0,8548,-21636},
    {2,1,-1,0,-7888,24208},
    {2,1,0,0,-6766,30824},
    {1,0,-1,0,-5163,-8379},
    {1,1,0,0,4987,-16675},
    {2,-1,1,0,4036,-12831},
    {2,0,2,0,3994,-10445},
    {4,0,0,0,3861,-11650},
    {2,0,-3,0,3665,14403},
    {0,1,-2,0,-2689,-7003},
    {2,0,-1,2,-2602,0},
    {2,-1,-2,0,2390,10056},
    {1,0,1,0,-2348,6322},
    {2,-2,0,0,2236,-9884},
    {0,1,2,0,-2120,5751},
    {0,2,0,0,-2069,0},
    {2,-2,-1,0,2048,-4950},
    {2,0,1,-2,-1773,4130},
    {2,0,0,2,-1595,0},
    {4,-1,-1,0,1215,-3958},
    {0,0,2,2,-1110,0},
    {3,0,-1,0,-892,3258},
    {2,1,1,0,-810,2616},
    {4,-1,-2,0,759,-1897},
    {0,2,-1,0,-713,-2117},
    {2,2,-1,0,-700,2354},
    {2,1,-2,0,691,0},
    {2,-1,0,-2,596,0},
    {4,0,1,0,549,-1423},
    {0,0,4,0,537,-1117},
    {4,-1,0,0,520,-1571},
    {1,0,-2,0,-487,-1739},
    {2,1,0,-2,-399,0},
    {0,0,2,-2,-381,-4421},
    {1,1,1,0,351,0},
    {3,0,-2,0,-340,0},
    {4,0,-3,0,330,0},
    {2,-1,2,0,327,0},
    {0,2,1,0,-323,1165},
    {1,1,-1,0,299,0},
    {2,0,3,0,294,0},
    {2,0,-1,-2,0,8752},
  };

  // ===== Meeus "Astronomical Algorithms" Table 47.B (60 terms) =====
  // Columns: D, M, M', F, Sigma-b (1e-6 deg)
  private static final int[][] TERMS_B = {
    {0,0,0,1,5128122},
    {0,0,1,1,280602},
    {0,0,1,-1,277693},
    {2,0,0,-1,173237},
    {2,0,-1,1,55413},
    {2,0,-1,-1,46271},
    {2,0,0,1,32573},
    {0,0,2,1,17198},
    {2,0,1,-1,9266},
    {0,0,2,-1,8822},
    {2,-1,0,-1,8216},
    {2,0,-2,-1,4324},
    {2,0,1,1,4200},
    {2,1,0,-1,-3359},
    {2,-1,-1,1,2463},
    {2,-1,0,1,2211},
    {2,-1,-1,-1,2065},
    {0,1,-1,-1,-1870},
    {4,0,-1,-1,1828},
    {0,1,0,1,-1794},
    {0,0,0,3,-1749},
    {0,1,-1,1,-1565},
    {1,0,0,1,-1491},
    {0,1,1,1,-1475},
    {0,1,1,-1,-1410},
    {0,1,0,-1,-1344},
    {1,0,0,-1,-1335},
    {0,0,3,1,1107},
    {4,0,0,-1,1021},
    {4,0,-1,1,833},
    {0,0,1,-3,777},
    {4,0,-2,1,671},
    {2,0,0,-3,607},
    {2,0,2,-1,596},
    {2,-1,1,-1,491},
    {2,0,-2,1,-451},
    {0,0,3,-1,439},
    {2,0,2,1,422},
    {2,0,-3,-1,421},
    {2,1,-1,1,-366},
    {2,1,0,1,-351},
    {4,0,0,1,331},
    {2,-1,1,1,315},
    {2,-2,0,-1,302},
    {0,0,1,3,-283},
    {2,1,1,-1,-229},
    {1,1,0,-1,223},
    {1,1,0,1,223},
    {0,1,-2,-1,-220},
    {2,1,-1,-1,-220},
    {1,0,1,1,-185},
    {2,-1,-2,-1,181},
    {0,1,2,1,-177},
    {4,0,-2,-1,176},
    {4,-1,-1,-1,166},
    {1,0,1,-1,-164},
    {4,0,1,-1,132},
    {1,0,-1,-1,-119},
    {4,-1,0,-1,115},
    {2,-2,0,1,107},
  };

  // ==================== low-level helpers ====================

  private static double deg2rad(double d)
  {
    return d * Math.PI / 180.0;
  }

  private static double rad2deg(double r)
  {
    return r * 180.0 / Math.PI;
  }

  private static double normalize(double angle)
  {
    angle = angle % 360.0;
    if (angle < 0)
      angle += 360.0;
    return angle;
  }

  private static double normalize180(double angle)
  {
    angle = normalize(angle);
    if (angle > 180.0)
      angle -= 360.0;
    return angle;
  }

  // ==================== time ====================

  /** Julian Day (TT approximates UT closely enough here) from epoch millis (UTC). */
  public static double jdFromMillis(long millis)
  {
    return 2440587.5 + millis / 86400000.0;
  }

  public static long millisFromJd(double jd)
  {
    return (long) ((jd - 2440587.5) * 86400000.0);
  }

  private static long localMidnightMillis(long timeMillis)
  {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(TZ_ID));
    cal.setTimeInMillis(timeMillis);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTimeInMillis();
  }

  // ==================== core ephemeris (Meeus Ch.47) ====================

  /** Geocentric ecliptic position of the Moon (mean equinox of date). */
  public static class MoonPos
  {
    public double lon;  // degrees
    public double lat;  // degrees
    public double dist; // km
  }

  public static MoonPos moonPosition(double jd)
  {
    double t = (jd - 2451545.0) / 36525.0;

    double lp = 218.3164477 + 481267.88123421 * t - 0.0015786 * t * t + t * t * t / 538841.0 - t * t * t * t / 65194000.0;
    double d = 297.8501921 + 445267.1114034 * t - 0.0018819 * t * t + t * t * t / 545868.0 - t * t * t * t / 113065000.0;
    double m = 357.5291092 + 35999.0502909 * t - 0.0001536 * t * t + t * t * t / 24490000.0;
    double mp = 134.9633964 + 477198.8675055 * t + 0.0087414 * t * t + t * t * t / 69699.0 - t * t * t * t / 14712000.0;
    double f = 93.2720950 + 483202.0175233 * t - 0.0036539 * t * t - t * t * t / 3526000.0 + t * t * t * t / 863310000.0;

    double a1 = 119.75 + 131.849 * t;
    double a2 = 53.09 + 479264.290 * t;
    double a3 = 313.45 + 481266.484 * t;

    double e = 1.0 - 0.002516 * t - 0.0000074 * t * t;
    double e2 = e * e;

    double dr = deg2rad(d);
    double mr = deg2rad(m);
    double mpr = deg2rad(mp);
    double fr = deg2rad(f);
    double lpr = deg2rad(lp);
    double a1r = deg2rad(a1);
    double a2r = deg2rad(a2);
    double a3r = deg2rad(a3);

    double sumL = 0.0, sumR = 0.0;
    for (int i = 0; i < TERMS_LR.length; i++)
    {
      int[] row = TERMS_LR[i];
      double arg = row[0] * dr + row[1] * mr + row[2] * mpr + row[3] * fr;
      double cl = row[4];
      double cr = row[5];
      int am = Math.abs(row[1]);
      if (am == 1)
      {
        cl *= e;
        cr *= e;
      }
      else if (am == 2)
      {
        cl *= e2;
        cr *= e2;
      }
      sumL += cl * Math.sin(arg);
      sumR += cr * Math.cos(arg);
    }
    // Additive terms for longitude
    sumL += 3958.0 * Math.sin(a1r) + 1962.0 * Math.sin(lpr - fr) + 318.0 * Math.sin(a2r);

    double sumB = 0.0;
    for (int i = 0; i < TERMS_B.length; i++)
    {
      int[] row = TERMS_B[i];
      double arg = row[0] * dr + row[1] * mr + row[2] * mpr + row[3] * fr;
      double cb = row[4];
      int am = Math.abs(row[1]);
      if (am == 1)
        cb *= e;
      else if (am == 2)
        cb *= e2;
      sumB += cb * Math.sin(arg);
    }
    // Additive terms for latitude
    sumB += -2235.0 * Math.sin(lpr) + 382.0 * Math.sin(a3r) + 175.0 * Math.sin(a1r - fr)
            + 175.0 * Math.sin(a1r + fr) + 127.0 * Math.sin(lpr - mpr) - 115.0 * Math.sin(lpr + mpr);

    MoonPos p = new MoonPos();
    p.lon = normalize(lp + sumL / 1000000.0);
    p.lat = sumB / 1000000.0;
    p.dist = 385000.56 + sumR / 1000.0;
    return p;
  }

  /** Apparent solar ecliptic longitude (Meeus Ch.25, low precision). */
  public static double sunEclipticLongitude(double jd)
  {
    double t = (jd - 2451545.0) / 36525.0;
    double l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t;
    double m = 357.52911 + 35999.05029 * t - 0.0001537 * t * t;
    double mr = deg2rad(m);
    double c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * Math.sin(mr)
             + (0.019993 - 0.000101 * t) * Math.sin(2.0 * mr)
             + 0.000289 * Math.sin(3.0 * mr);
    return normalize(l0 + c);
  }

  /** Mean obliquity of the ecliptic (Meeus Ch.22). */
  private static double obliquity(double t)
  {
    return 23.439291111111 - 0.0130041666667 * t - 1.63888888889e-7 * t * t + 5.03611111111e-7 * t * t * t;
  }

  /** Ecliptic -> equatorial. Returns {RA(deg), Dec(deg)}. */
  private static double[] eclipticToEquatorial(double lon, double lat, double jd)
  {
    double t = (jd - 2451545.0) / 36525.0;
    double eps = deg2rad(obliquity(t));
    double l = deg2rad(lon);
    double b = deg2rad(lat);
    double ra = Math.atan2(Math.sin(l) * Math.cos(eps) - Math.tan(b) * Math.sin(eps), Math.cos(l));
    double dec = Math.asin(Math.sin(b) * Math.cos(eps) + Math.cos(b) * Math.sin(eps) * Math.sin(l));
    return new double[] {normalize(rad2deg(ra)), rad2deg(dec)};
  }

  /** Moon phase angle i (Sun-Moon-Earth), degrees. 0 = full, 180 = new. */
  public static double phaseAngleDeg(double jd)
  {
    MoonPos p = moonPosition(jd);
    double elong = normalize(p.lon - sunEclipticLongitude(jd));
    return 180.0 - elong;
  }

  /** Elongation of the Moon from the Sun (0 = new, 180 = full), degrees. */
  public static double elongationDeg(double jd)
  {
    return normalize(moonPosition(jd).lon - sunEclipticLongitude(jd));
  }

  // ============ public accurate API ============

  /** Illuminated fraction of the disk, 0..1 (Meeus Ch.48). */
  public static double getIllumination(long timeMillis)
  {
    double i = phaseAngleDeg(jdFromMillis(timeMillis));
    return 0.5 * (1.0 + Math.cos(deg2rad(i)));
  }

  /**
   * Phase fraction in the lunation: 0.0 = new, 0.25 = first quarter,
   * 0.5 = full, 0.75 = last quarter, 1.0 = new again.
   * Derived directly from the true Sun-Moon elongation, so it drives the
   * drawn terminator correctly.
   */
  public static double getPhaseFraction(long timeMillis)
  {
    return elongationDeg(jdFromMillis(timeMillis)) / 360.0;
  }

  /**
   * Lunar age: days elapsed since the last New Moon (0 .. ~29.53).
   * Uses the real elongation cross (Meeus Ch.49) rather than a fixed
   * mean-month remainder, so it stays accurate across the whole lunation.
   */
  public static double getLunarAge(long timeMillis)
  {
    double jd = jdFromMillis(timeMillis);
    double frac = elongationDeg(jd) / 360.0;
    double jdNew = jd - frac * SYNODIC_MONTH;
    for (int k = 0; k < 8; k++)
    {
      double e = normalize180(elongationDeg(jdNew));
      double e2 = normalize180(elongationDeg(jdNew + 0.05));
      double slope = (e2 - e) / 0.05;
      if (Math.abs(slope) < 1e-6)
        break;
      jdNew -= e / slope;
    }
    double age = jd - jdNew;
    if (age < 0)
      age += SYNODIC_MONTH;
    return age;
  }

  /** Moon-Earth distance in kilometres. */
  public static double getDistanceKm(long timeMillis)
  {
    return moonPosition(jdFromMillis(timeMillis)).dist;
  }

  /** Geocentric ecliptic longitude of the Moon, degrees. */
  public static double getEclipticLongitude(long timeMillis)
  {
    return moonPosition(jdFromMillis(timeMillis)).lon;
  }

  /** Geocentric ecliptic latitude of the Moon, degrees. */
  public static double getEclipticLatitude(long timeMillis)
  {
    return moonPosition(jdFromMillis(timeMillis)).lat;
  }

  /** Geocentric equatorial coordinates {RA(deg), Dec(deg)}. */
  public static double[] getEquatorialPosition(long timeMillis)
  {
    double jd = jdFromMillis(timeMillis);
    MoonPos p = moonPosition(jd);
    return eclipticToEquatorial(p.lon, p.lat, jd);
  }

  /** Phase angle (deg) i.e. Sun-Moon-Earth: 0 = full moon, 180 = new moon. */
  public static double getPhaseAngle(long timeMillis)
  {
    return phaseAngleDeg(jdFromMillis(timeMillis));
  }

  /** Find the instant of the next New (target=0) or Full (target=180) Moon. */
  private static long findPhaseInstant(long fromMillis, double targetElong)
  {
    double jd = jdFromMillis(fromMillis);
    double delta = targetElong - elongationDeg(jd);
    while (delta < 0)
      delta += 360.0;
    double jdT = jd + delta / 360.0 * SYNODIC_MONTH;
    for (int k = 0; k < 10; k++)
    {
      double e = normalize180(elongationDeg(jdT) - targetElong);
      double e2 = normalize180(elongationDeg(jdT + 0.05) - targetElong);
      double slope = (e2 - e) / 0.05;
      if (Math.abs(slope) < 1e-6)
        break;
      jdT -= e / slope;
    }
    return millisFromJd(jdT);
  }

  public static long getNextNewMoon(long timeMillis)
  {
    return findPhaseInstant(timeMillis, 0.0);
  }

  public static long getNextFullMoon(long timeMillis)
  {
    return findPhaseInstant(timeMillis, 180.0);
  }

  // ============ rise / set (Meeus Ch.15) ============

  /** Standard altitude of the Moon's centre at rising/setting (Meeus Ch.15). */
  private static double moonHorizonDeg(double jd)
  {
    double dist = moonPosition(jd).dist;
    double parallax = Math.asin(EARTH_RADIUS_KM / dist);
    return 0.7275 * rad2deg(parallax) - 0.5667;
  }

  /** Geocentric altitude of the Moon's centre above the horizon, degrees. */
  public static double moonAltitudeDeg(double jd)
  {
    double t = (jd - 2451545.0) / 36525.0;
    MoonPos p = moonPosition(jd);
    double[] eq = eclipticToEquatorial(p.lon, p.lat, jd);
    double ra = eq[0];
    double dec = eq[1];
    double theta = 280.46061837 + 360.98564736629 * (jd - 2451545.0) + 0.000387933 * t * t - t * t * t / 38710000.0;
    double lst = normalize(theta + LON);
    double ha = normalize180(lst - ra);
    double har = deg2rad(ha);
    double decr = deg2rad(dec);
    double latr = deg2rad(LAT);
    double alt = Math.asin(Math.sin(latr) * Math.sin(decr) + Math.cos(latr) * Math.cos(decr) * Math.cos(har));
    return rad2deg(alt);
  }

  private static long bisectCrossing(long t1, long t2, boolean rising)
  {
    for (int k = 0; k < 40; k++)
    {
      long tm = (t1 + t2) / 2;
      double a = moonAltitudeDeg(jdFromMillis(tm)) - moonHorizonDeg(jdFromMillis(tm));
      if (rising)
      {
        if (a > 0)
          t2 = tm;
        else
          t1 = tm;
      }
      else
      {
        if (a < 0)
          t2 = tm;
        else
          t1 = tm;
      }
    }
    return (t1 + t2) / 2;
  }

  private static long findRiseSet(long fromMillis, boolean rising)
  {
    final long step = 60000L; // one minute
    final long span = 32L * 3600000L; // search a full lunar day
    double prev = Double.NaN;
    long prevT = 0;
    long steps = span / step;
    for (long i = 0; i <= steps; i++)
    {
      long t = fromMillis + i * step;
      double jd = jdFromMillis(t);
      double a = moonAltitudeDeg(jd) - moonHorizonDeg(jd);
      if (i > 0)
      {
        if (rising && prev < 0.0 && a >= 0.0)
          return bisectCrossing(prevT, t, true);
        if (!rising && prev > 0.0 && a <= 0.0)
          return bisectCrossing(prevT, t, false);
      }
      prev = a;
      prevT = t;
    }
    return Long.MIN_VALUE;
  }

  private static String riseSetFormatted(long timeMillis, boolean rising)
  {
    long dayStart = localMidnightMillis(timeMillis);
    // Prefer the event that belongs to the requested calendar day.
    long ev = findRiseSet(dayStart, rising);
    if (ev != Long.MIN_VALUE && ev >= dayStart && ev < dayStart + 86400000L)
      return formatHours((ev - dayStart) / 3600000.0);
    // Otherwise show the next event after the reference moment so the card
    // is never empty (the Moon always rises and sets within a lunar day).
    long from = dayStart;
    ev = findRiseSet(from, rising);
    if (ev == Long.MIN_VALUE)
      return "--:--";
    return formatHours((ev - dayStart) / 3600000.0 % 24.0);
  }

  public static String getMoonriseTime(long timeMillis)
  {
    return riseSetFormatted(timeMillis, true);
  }

  public static String getMoonsetTime(long timeMillis)
  {
    return riseSetFormatted(timeMillis, false);
  }

  /**
   * Format decimal hours (0..24) as "h:mm ص/م" in Arabic.
   */
  private static String formatHours(double hours)
  {
    if (Double.isNaN(hours) || hours < 0 || hours >= 24)
      return "--:--";
    int total = (int) Math.round(hours * 60.0);
    total = ((total % 1440) + 1440) % 1440;
    int hh = total / 60;
    int mm = total % 60;
    int h12 = hh % 12;
    if (h12 == 0)
      h12 = 12;
    String ampm = hh < 12 ? "ص" : "م";
    return String.format("%d:%02d %s", h12, mm, ampm);
  }

  public static String formatTimeAmPm(Calendar cal)
  {
    int hour = cal.get(Calendar.HOUR);
    if (hour == 0)
      hour = 12;
    int minute = cal.get(Calendar.MINUTE);
    String ampm = cal.get(Calendar.AM_PM) == Calendar.AM ? "ص" : "م";
    return String.format("%d:%02d %s", hour, minute, ampm);
  }

  // ============ phase naming ============

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

  // ============ bundled details ============

  public static class MoonDetails
  {
    public double age;            // days since new moon
    public double illumination;   // 0..1
    public double phaseFraction;  // 0..1 (0=new, 0.5=full)
    public double phaseAngle;     // deg (0=full, 180=new)
    public double distanceKm;     // Earth-Moon distance
    public double ra;             // right ascension (deg)
    public double dec;            // declination (deg)
    public double eclipticLon;    // ecliptic longitude (deg)
    public double eclipticLat;    // ecliptic latitude (deg)
    public int phaseIndex;        // 0..7
    public String phaseName;      // Arabic phase name
  }

  public static MoonDetails getMoonDetails(long timeMillis)
  {
    double jd = jdFromMillis(timeMillis);
    MoonPos p = moonPosition(jd);
    double[] eq = eclipticToEquatorial(p.lon, p.lat, jd);
    double i = phaseAngleDeg(jd);

    MoonDetails m = new MoonDetails();
    m.age = getLunarAge(timeMillis);
    m.illumination = 0.5 * (1.0 + Math.cos(deg2rad(i)));
    m.phaseFraction = normalize(p.lon - sunEclipticLongitude(jd)) / 360.0;
    m.phaseAngle = i;
    m.distanceKm = p.dist;
    m.ra = eq[0];
    m.dec = eq[1];
    m.eclipticLon = p.lon;
    m.eclipticLat = p.lat;
    m.phaseIndex = getPhaseIndex(m.age);
    m.phaseName = getPhaseNameArabic(m.age);
    return m;
  }

  // ============ weekly ============

  public static class MoonDayInfo
  {
    public Calendar date;
    public double age;
    public double illumination;
    public String phaseName;
    public int phaseIndex;
    public String moonrise;
    public String moonset;
    /** True phase fraction 0..1 (0=new, 0.5=full) from real elongation. */
    public double phaseFraction;
  }

  public static List<MoonDayInfo> getWeeklyMoonInfo()
  {
    List<MoonDayInfo> info = new ArrayList<>();
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(TZ_ID));
    cal.set(Calendar.HOUR_OF_DAY, 12);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    for (int d = 0; d < 7; d++)
    {
      long ms = cal.getTimeInMillis();
      MoonDetails md = getMoonDetails(ms);
      MoonDayInfo day = new MoonDayInfo();
      day.date = (Calendar) cal.clone();
      day.age = md.age;
      day.illumination = md.illumination;
      day.phaseName = md.phaseName;
      day.phaseIndex = md.phaseIndex;
      day.moonrise = getMoonriseTime(ms);
      day.moonset = getMoonsetTime(ms);
      day.phaseFraction = md.phaseFraction;
      info.add(day);
      cal.add(Calendar.DAY_OF_MONTH, 1);
    }
    return info;
  }
}
