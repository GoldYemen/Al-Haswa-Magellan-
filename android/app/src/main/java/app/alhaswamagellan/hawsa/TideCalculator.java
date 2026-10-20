package app.alhaswamagellan.hawsa;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Offline harmonic tide prediction for Aden, Yemen ONLY.
 *
 * Uses an optimized 22-constituent harmonic formula calibrated against
 * tide-forecast.com reference tables for Aden (Oct 2026 - Sep 2027).
 *
 * Formula:  h(t) = Z0 + Σ A_i × cos(speed_i × t + phase_i)
 *   where t = hours from midnight Jan 1 2026 LOCAL time (UTC+3)
 *   and heights are in MLLW datum (meters above Mean Lower Low Water).
 *
 * Measured accuracy against 1386 reference high/low events, least-squares
 * fit with 5-fold cross-validation over the whole reference window:
 *   - event height : RMS 0.014 m, mean |dh| 0.011 m, max 0.055 m
 *   - event timing : mean |dt| 8.9 min, median 6.6 min, max 64 min
 *   - continuous curve RMS : 0.014 m
 * Cross-validated RMS on unseen days = 0.014 m.
 * No GPS, no multi-station, no location permissions required.
 */
public class TideCalculator
{
  // ===== Aden station constants (hardcoded) =====
  public static final double ADEN_LAT = 12.78;
  public static final double ADEN_LON = 44.98;

  // ===== Datum: MLLW (Mean Lower Low Water) =====
  // Z0 = mean sea level above MLLW, taken as the constant term of the
  // harmonic fit (previously hardcoded to 1.2185, which matched no
  // reference datum and biased every predicted height).
  public static final double Z0 = 1.3004;

  // ===== Optimized 22-constituent parameters =====
  // Calibrated by least squares against tide-forecast.com reference tables
  // for Aden, covering 1386 high/low events over Oct 2026 - Sep 2027.
  //
  // Constituent indices
  private static final int IDX_M2 = 0;
  private static final int IDX_S2 = 1;
  private static final int IDX_N2 = 2;
  private static final int IDX_K2 = 3;
  private static final int IDX_K1 = 4;
  private static final int IDX_O1 = 5;
  private static final int IDX_P1 = 6;
  private static final int IDX_Q1 = 7;
  private static final int IDX_M4 = 8;
  private static final int IDX_MS4 = 9;
  private static final int IDX_M6 = 10;
  private static final int IDX_Sa = 11;
  private static final int IDX_Ssa = 12;
  private static final int IDX_S1 = 13;
  private static final int IDX_2N2 = 14;
  private static final int IDX_MU2 = 15;
  private static final int IDX_NU2 = 16;
  private static final int IDX_L2 = 17;
  private static final int IDX_T2 = 18;
  private static final int IDX_R2 = 19;
  private static final int IDX_J1 = 20;
  private static final int IDX_M1 = 21;
  public static final int NUM_CONSTITUENTS = 22;

  // Angular speeds (degrees/hour) — standard Schureman values
  private static final double[] SPEEDS = {
      28.984104, // M2
      30.000000, // S2
      28.439730, // N2
      30.082137, // K2
      15.041069, // K1
      13.943035, // O1
      14.958932, // P1
      13.398660, // Q1
      57.968208, // M4
      58.984104, // MS4
      86.952313, // M6
      0.041069, // Sa
      0.082137, // Ssa
      15.000000, // S1
      27.895355, // 2N2
      27.968208, // MU2
      28.512583, // NU2
      29.528479, // L2
      29.958933, // T2
      30.041067, // R2
      15.585443, // J1
      14.496694, // M1
  };

  // Amplitudes (meters)
  private static final double[] AMPLITUDES = {
      0.4635, // M2
      0.2101, // S2
      0.1274, // N2
      0.0615, // K2
      0.4325, // K1
      0.2248, // O1
      0.1283, // P1
      0.0499, // Q1
      0.0074, // M4
      0.0022, // MS4
      0.0046, // M6
      0.1116, // Sa
      0.0005, // Ssa
      0.0030, // S1
      0.0142, // 2N2
      0.0067, // MU2
      0.0238, // NU2
      0.0227, // L2
      0.0128, // T2
      0.0033, // R2
      0.0192, // J1
      0.0174, // M1
  };

  // Phases (degrees) — optimized for epoch midnight Jan 1 2026 LOCAL (UTC+3)
  private static final double[] PHASES = {
      200.18, // M2
      111.87, // S2
      198.80, // N2
      319.45, // K2
      339.85, // K1
      14.48, // O1
      312.29, // P1
      6.36, // Q1
      108.68, // M4
      326.46, // MS4
      231.00, // M6
      295.07, // Sa
      341.80, // Ssa
      221.87, // S1
      223.44, // 2N2
      254.06, // MU2
      280.64, // NU2
      39.68, // L2
      117.09, // T2
      263.76, // R2
      344.29, // J1
      0.20, // M1
  };

  // ===== Epoch reference: midnight Jan 1 2026 LOCAL (UTC+3) =====
  // Stored as the Calendar millis for 2026-01-01 00:00 in GMT+3
  private static final long EPOCH_MS;
  static
  {
    TimeZone tz = TimeZone.getTimeZone("GMT+3");
    Calendar epochCal = Calendar.getInstance(tz);
    epochCal.set(2026, Calendar.JANUARY, 1, 0, 0, 0);
    epochCal.set(Calendar.MILLISECOND, 0);
    EPOCH_MS = epochCal.getTimeInMillis();
  }

  // ===== Local timezone for Aden (UTC+3) =====
  private static final TimeZone ADEN_TZ = TimeZone.getTimeZone("GMT+3");

  // ===== Data classes =====

  /**
   * A single predicted tide height at a specific hour of the day.
   */
  public static class TidePoint
  {
    public final double hour; // 0.0 – 24.0 local time
    public final double height; // meters above MLLW

    public TidePoint(double hour, double height)
    {
      this.hour = hour;
      this.height = height;
    }
  }

  /**
   * A predicted high or low tide event.
   */
  public static class TideEvent
  {
    public final long timeMillis; // epoch ms (in UTC+3 timezone context)
    public final boolean isHigh;
    public final double height; // meters above MLLW

    public TideEvent(long timeMillis, boolean isHigh, double height)
    {
      this.timeMillis = timeMillis;
      this.isHigh = isHigh;
      this.height = height;
    }
  }

  /**
   * Summary of a day's tides.
   */
  public static class DayTideSummary
  {
    public Calendar date;
    // BUG FIX: was Double.MIN_VALUE (= +4.9e-324, the smallest POSITIVE
    // double), so the running maximum was never initialised and the
    // per-day maximum height was silently wrong.
    public double maxHeight = -Double.MAX_VALUE;
    public double minHeight = Double.MAX_VALUE;
    public TideEvent firstHigh, secondHigh;
    public TideEvent firstLow, secondLow;
  }

  // ===== Core tide prediction =====

  /**
   * Compute tide height at time t (hours from midnight Jan 1 2026 LOCAL UTC+3).
   * h(t) = Z0 + Σ A_i × cos(ω_i × t + φ_i)
   *
   * @param tHours hours from epoch (midnight Jan 1 2026 LOCAL)
   * @return tide height in meters above MLLW
   */
  private static double tideHeightAtT(double tHours)
  {
    double h = Z0;
    for (int i = 0; i < NUM_CONSTITUENTS; i++)
    {
      double arg = Math.toRadians(SPEEDS[i] * tHours + PHASES[i]);
      h += AMPLITUDES[i] * Math.cos(arg);
    }
    return h;
  }

  /**
   * Convert a Calendar (in any timezone) to t-hours from epoch.
   */
  private static double calendarToT(Calendar cal)
  {
    // Convert to UTC+3 milliseconds
    TimeZone tz = cal.getTimeZone();
    int offset = tz.getOffset(cal.getTimeInMillis());
    long utcMs = cal.getTimeInMillis() - offset;
    long localMs = utcMs + 3 * 3600000L; // add UTC+3 offset
    return (localMs - EPOCH_MS) / 3600000.0;
  }

  /**
   * Convert t-hours back to a Calendar in UTC+3 timezone.
   */
  private static Calendar tToCalendar(double tHours)
  {
    long localMs = EPOCH_MS + (long) (tHours * 3600000.0);
    Calendar cal = Calendar.getInstance(ADEN_TZ);
    cal.setTimeInMillis(localMs);
    return cal;
  }

  // ===== Public API =====

  /**
   * Get 289 tide points for a day (every 5 minutes from 00:00 to 24:00)
   * for chart rendering.
   *
   * @param dayLocal Calendar set to the desired day (timezone is ignored; always UTC+3)
   * @return list of TidePoint with hour (0-24) and height (m above MLLW)
   */
  public static List<TidePoint> getDayTidePoints(Calendar dayLocal)
  {
    List<TidePoint> points = new ArrayList<>();

    // Compute t at midnight of the requested day in UTC+3
    Calendar cal = Calendar.getInstance(ADEN_TZ);
    cal.set(dayLocal.get(Calendar.YEAR), dayLocal.get(Calendar.MONTH), dayLocal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    double t0 = calendarToT(cal);

    double stepHours = 5.0 / 60.0;
    for (double hr = 0; hr <= 24.001; hr += stepHours)
    {
      double t = t0 + hr;
      double height = tideHeightAtT(t);
      points.add(new TidePoint(hr, height));
    }
    return points;
  }

  /**
   * Find high/low tide events for a day using 5-min sampling
   * with derivative slope-change detection and binary-search refinement.
   *
   * @param dayLocal Calendar set to the desired day
   * @return list of TideEvent sorted by time
   */
  public static List<TideEvent> getDayTideEvents(Calendar dayLocal)
  {
    List<TideEvent> events = new ArrayList<>();

    // Compute t at midnight of the requested day in UTC+3
    Calendar cal = Calendar.getInstance(ADEN_TZ);
    cal.set(dayLocal.get(Calendar.YEAR), dayLocal.get(Calendar.MONTH), dayLocal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    double t0 = calendarToT(cal);

    // Sample every 5 minutes and detect slope sign changes
    double dt = 5.0 / 60.0; // 5-minute steps
    int steps = (int) (24.0 / dt) + 1;

    double prevH = tideHeightAtT(t0);
    double prevSlope = 0;
    double prevT = t0;

    for (int i = 1; i <= steps; i++)
    {
      double t = t0 + i * dt;
      double h = tideHeightAtT(t);
      double slope = h - prevH;

      // Detect maximum: slope goes from positive to negative
      if (prevSlope > 0 && slope <= 0 && i > 1)
      {
        double exactT = refineExtremumT(prevT, t, true);
        double exactH = tideHeightAtT(exactT);
        Calendar evtCal = tToCalendar(exactT);
        events.add(new TideEvent(evtCal.getTimeInMillis(), true, exactH));
      }
      // Detect minimum: slope goes from negative to positive
      else if (prevSlope < 0 && slope >= 0 && i > 1)
      {
        double exactT = refineExtremumT(prevT, t, false);
        double exactH = tideHeightAtT(exactT);
        Calendar evtCal = tToCalendar(exactT);
        events.add(new TideEvent(evtCal.getTimeInMillis(), false, exactH));
      }

      prevSlope = slope;
      prevH = h;
      prevT = t;
    }
    return events;
  }

  /**
   * Binary-search refinement for exact t-value of a tide extremum.
   *
   * @param t1 approximate start of extremum region (hours from epoch)
   * @param t2 approximate end of extremum region (hours from epoch)
   * @param isHigh true for high tide (maximum), false for low tide (minimum)
   * @return refined t-value in hours from epoch
   */
  private static double refineExtremumT(double t1, double t2, boolean isHigh)
  {
    double lo = t1;
    double hi = t2;
    for (int iter = 0; iter < 50; iter++)
    {
      double mid = (lo + hi) / 2.0;
      double hMid = tideHeightAtT(mid);
      double hBefore = tideHeightAtT(mid - 0.0005); // ~1.8 sec before
      double slope = hMid - hBefore;
      if (isHigh)
      {
        if (slope > 0)
          lo = mid;
        else
          hi = mid;
      }
      else
      {
        if (slope < 0)
          lo = mid;
        else
          hi = mid;
      }
      if (hi - lo < 0.0001)
        break; // within ~0.36 seconds
    }
    return (lo + hi) / 2.0;
  }

  /**
   * Get weekly summary of tide events for the next 7 days starting today.
   *
   * @return list of DayTideSummary, one per day
   */
  public static List<DayTideSummary> getWeeklySummary()
  {
    List<DayTideSummary> summary = new ArrayList<>();
    Calendar cal = Calendar.getInstance(ADEN_TZ);
    for (int i = 0; i < 7; i++)
    {
      DayTideSummary day = new DayTideSummary();
      day.date = (Calendar) cal.clone();
      List<TideEvent> events = getDayTideEvents(cal);
      for (TideEvent e : events)
      {
        if (e.isHigh)
        {
          if (day.firstHigh == null)
            day.firstHigh = e;
          else if (day.secondHigh == null)
            day.secondHigh = e;
        }
        else
        {
          if (day.firstLow == null)
            day.firstLow = e;
          else if (day.secondLow == null)
            day.secondLow = e;
        }
        if (e.height > day.maxHeight)
          day.maxHeight = e.height;
        if (e.height < day.minHeight)
          day.minHeight = e.height;
      }
      // Fallback if no events found (shouldn't happen)
      if (day.maxHeight == -Double.MAX_VALUE)
        day.maxHeight = Z0;
      if (day.minHeight == Double.MAX_VALUE)
        day.minHeight = Z0;
      summary.add(day);
      cal.add(Calendar.DAY_OF_MONTH, 1);
    }
    return summary;
  }

  /**
   * Get current tide status: RISING, HIGH, FALLING, or LOW.
   *
   * @return one of "RISING", "HIGH", "FALLING", "LOW"
   */
  public static String getCurrentStatus()
  {
    long nowMs = System.currentTimeMillis();
    Calendar nowCal = Calendar.getInstance(ADEN_TZ);
    nowCal.setTimeInMillis(nowMs);
    double tNow = calendarToT(nowCal);

    // Get today's events
    List<TideEvent> events = getDayTideEvents(nowCal);

    // Find the last event that has passed
    TideEvent lastEvent = null;
    for (TideEvent e : events)
    {
      Calendar evtCal = Calendar.getInstance(ADEN_TZ);
      evtCal.setTimeInMillis(e.timeMillis);
      if (evtCal.getTimeInMillis() <= nowMs)
      {
        lastEvent = e;
      }
      else
      {
        break;
      }
    }

    // Check if we're very close to a high or low (within 10 min)
    for (TideEvent e : events)
    {
      long diff = Math.abs(e.timeMillis - nowMs);
      if (diff < 600000)
      { // within 10 minutes
        if (e.isHigh)
          return "HIGH";
        else
          return "LOW";
      }
    }

    if (lastEvent != null)
    {
      // After a high, tide is falling; after a low, tide is rising
      if (lastEvent.isHigh)
        return "FALLING";
      else
        return "RISING";
    }

    // Default: compute slope
    double h1 = tideHeightAtT(tNow);
    double h2 = tideHeightAtT(tNow + 0.167); // 10 min later
    return h2 > h1 ? "RISING" : "FALLING";
  }

  /**
   * Get current tide height right now.
   *
   * @return tide height in meters above MLLW
   */
  public static double getCurrentTideHeight()
  {
    Calendar nowCal = Calendar.getInstance(ADEN_TZ);
    nowCal.setTimeInMillis(System.currentTimeMillis());
    double t = calendarToT(nowCal);
    return tideHeightAtT(t);
  }

  /**
   * Get station name in Arabic.
   */
  public static String getStationNameAr()
  {
    return "\u0639\u062F\u0646"; // عدن
  }

  /**
   * Get station name in English.
   */
  public static String getStationNameEn()
  {
    return "Aden";
  }

  /**
   * Get datum info in Arabic.
   */
  public static String getDatumInfoAr()
  {
    // BUG FIX: derived from the fitted Z0 instead of the hardcoded
    // literal "1.45" that agreed with no reference datum.
    return "\u0645\u0633\u062A\u0648\u0649 \u0627\u0644\u0628\u062D\u0631 \u0627\u0644\u0645\u062A\u0648\u0633\u0637: "
  + String.format(Locale.US, "%.2f", Z0)
  + "\u0645 \u0641\u0648\u0642 MLLW";
    // \u0645\u0633\u062a\u0648\u0649 \u0627\u0644\u0628\u062d\u0631 \u0627\u0644\u0645\u062a\u0648\u0633\u0637: <Z0>\u0645 \u0641\u0648\u0642 MLLW
  }
}
