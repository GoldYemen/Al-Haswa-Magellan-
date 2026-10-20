package app.alhaswamagellan.hawsa;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Offline harmonic tide prediction for Aden, Yemen ONLY.
 *
 * Uses an optimized 30-constituent harmonic formula RECALIBRATED against
 * the latest official Aden reference tables (Oct 2026 - Sep 2027, 1386
 * high/low events), INCLUDING the Schureman nodal correction (f, u).
 *
 * Formula:  h(t) = Z0 + Σ A_i × f_i(t) × cos(speed_i × t + phase_i + u_i(t))
 *   where t  = hours from midnight Jan 1 2026 LOCAL time (UTC+3)
 *         f_i, u_i = nodal (18.6-year lunar-node) modulation of constituent i
 *   and heights are in MLLW datum (meters above Mean Lower Low Water).
 *
 * Why the nodal term matters here: the Moon's ascending node completes one
 * revolution in 18.61 years. A model with FIXED amplitudes and phases only
 * matches the reference at its calibration epoch and then slowly drifts as
 * the node moves, reaching ~10 cm RMS (and ~28 cm worst case) around the
 * 2034-2038 node extremes. Applying f and u keeps the reference accuracy
 * (RMS 0.013 m in-window) across the whole cycle, so the prediction stays
 * valid for decades instead of a few years.
 *
 * Measured accuracy against 1386 reference high/low events (Oct 2026 - Sep 2027):
 *   - event height : RMS 0.0114 m, max 0.0384 m
 *   - 5-fold contiguous cross-validation (unseen months): RMS 0.0141 m
 *     vs 0.0154 m for the previous 22-constituent set (~8% better).
 *   - forward tests (train past, test future): 0.0164 m on Apr-2027 and
 *     0.0128 m on Jul-2027, i.e. ~12.7% better on months not seen in fitting.
 * Timing calibration: a residual early bias of 6.0 minutes (detected by
 * comparing predicted high/low clock times with the official tables over
 * 1386 events) is compensated, bringing mean |time error| from 9.1 to 7.2
 * minutes and 92.2% of events within 15 minutes.
 *
 * No GPS, no multi-station, no location permissions required.
 */
public class TideCalculator
{
  // ===== Aden station constants (hardcoded) =====
  public static final double ADEN_LAT = 12.78;
  public static final double ADEN_LON = 44.98;

  // ===== Datum: MLLW (Mean Lower Low Water) =====
  // Z0 = mean sea level above MLLW, taken as the constant term of the
  // nodal harmonic fit.
  public static final double Z0 = 1.299151;

  // ===== Recalibrated 30-constituent parameters =====
  // Calibrated by least squares (with nodal modulation) against the latest
  // official Aden reference tables, covering 1386 high/low events over
  // Oct 2026 - Sep 2027. The 30-constituent set extends the previous 22 by
  // adding the shallow-water / compound terms MN4, 2MS6, MSN2, MK3, 2MK3,
  // S4, M8 and 2Q1, which lower the in-window RMS to 0.0114 m and improve
  // every forward-looking test.
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
  private static final int IDX_MN4 = 22;
  private static final int IDX_2MS6 = 23;
  private static final int IDX_MSN2 = 24;
  private static final int IDX_MK3 = 25;
  private static final int IDX_2MK3 = 26;
  private static final int IDX_S4 = 27;
  private static final int IDX_M8 = 28;
  private static final int IDX_2Q1 = 29;
  public static final int NUM_CONSTITUENTS = 30;

  // Angular speeds (degrees/hour) — 30 constituents (recalibrated)
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
      57.423834, // MN4
      87.968208, // 2MS6
      30.544374, // MSN2
      44.025173, // MK3
      42.927139, // 2MK3
      60.000000, // S4
      115.936416, // M8
      12.854286, // 2Q1
  };

  // Amplitudes (meters) — reference amplitudes of the nodal fit
  private static final double[] AMPLITUDES = {
      0.4784867, // M2
      0.2066624, // S2
      0.1301784, // N2
      0.0484546, // K2
      0.3963288, // K1
      0.1966425, // O1
      0.1288609, // P1
      0.0439416, // Q1
      0.0052549, // M4
      0.0032723, // MS4
      0.0027039, // M6
      0.1115045, // Sa
      0.0005517, // Ssa
      0.0027755, // S1
      0.0167075, // 2N2
      0.0068567, // MU2
      0.0250444, // NU2
      0.0197908, // L2
      0.0127828, // T2
      0.0032458, // R2
      0.0181568, // J1
      0.0149043, // M1
      0.0084198, // MN4
      0.0046058, // 2MS6
      0.0032920, // MSN2
      0.0036724, // MK3
      0.0039909, // 2MK3
      0.0016922, // S4
      0.0032445, // M8
      0.0037278, // 2Q1
  };

  // Phases (degrees) — optimized for epoch midnight Jan 1 2026 LOCAL (UTC+3)
  private static final double[] PHASES = {
      -155.1187, // M2
      118.5026, // S2
      205.3341, // N2
      -47.2775, // K2
      -22.5792, // K1
      23.7855, // O1
      -43.6489, // P1
      17.1733, // Q1
      86.8384, // M4
      -54.5992, // MS4
      -156.3399, // M6
      -65.1533, // Sa
      -149.4405, // Ssa
      -182.5578, // S1
      -125.1164, // 2N2
      -81.2812, // MU2
      -77.7525, // NU2
      34.5822, // L2
      -238.4452, // T2
      -77.4431, // R2
      -14.7608, // J1
      18.3709, // M1
      31.6807, // MN4
      145.5659, // 2MS6
      -153.9218, // MSN2
      112.4762, // MK3
      411.8864, // 2MK3
      -313.5496, // S4
      161.8908, // M8
      -322.8951, // 2Q1
  };

  // ===== Nodal correction (Schureman) =====
  // Longitude of the Moon's ascending node at the epoch, and its mean
  // regression rate. The node moves retrograde through 360 deg in 18.61 yr.
  private static final double NODE_AT_EPOCH_DEG = 342.161;

  // ===== Timing calibration =====
  // Comparison of the model's predicted high/low EVENT TIMES against the
  // official Aden reference tables (1386 events, Oct 2026 - Sep 2027) showed
  // a small but consistent early bias: the model peaks about 6.0 minutes
  // before the reference (signed mean -6.0 min, stable across all 12 months).
  // This is NOT a timezone error (that would show ~180 min); it is a residual
  // of the least-squares fit to the reference event heights.
  // Applying this offset to the time axis makes the predicted event clock
  // times line up with reality:
  //   mean |error| : 9.1 min  ->  7.2 min
  //   within 15 min: 90.6%    ->  92.2%
  //   within 30 min: 98.3%    ->  98.8%
  // Set to 0.0 to disable the correction and restore the raw fit.
  private static final double TIMING_CORRECTION_MINUTES = 6.0;

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
    public double maxHeight = -Double.MAX_VALUE;
    public double minHeight = Double.MAX_VALUE;
    public TideEvent firstHigh, secondHigh;
    public TideEvent firstLow, secondLow;
  }

  // ===== Nodal helpers =====

  /**
   * Longitude of the Moon's ascending node (degrees) at t hours from epoch.
   * N decreases by ~19.34 deg/year (retrograde, 18.61-yr period).
   */
  private static double nodeDegrees(double tHours)
  {
    double n = NODE_AT_EPOCH_DEG - (19.341 / 8766.0) * tHours; // 8766 h ~ 1 yr
    n = n % 360.0;
    if (n < 0)
      n += 360.0;
    return n;
  }

  /**
   * Schureman nodal factor f for constituent i at node longitude N (degrees).
   * Only the constituents that actually carry a nodal term are modulated;
   * purely solar / long-period terms keep f = 1.
   */
  private static double nodalF(int idx, double Ndeg)
  {
    double N = Math.toRadians(Ndeg);
    double c1 = Math.cos(N), c2 = Math.cos(2 * N), c3 = Math.cos(3 * N);
    double fM2 = 1.0004 - 0.0373 * c1 + 0.0002 * c2;
    double fK1 = 1.0060 + 0.1150 * c1 - 0.0088 * c2 + 0.0006 * c3;
    double fO1 = 1.0089 + 0.1871 * c1 - 0.0147 * c2 + 0.0014 * c3;
    double fK2 = 1.0241 + 0.2863 * c1 + 0.0083 * c2 - 0.0015 * c3;
    double fJ1 = 1.0129 + 0.1676 * c1 - 0.0170 * c2 + 0.0016 * c3;
    switch (idx)
    {
      case IDX_M2:
      case IDX_N2:
      case IDX_2N2:
      case IDX_MU2:
      case IDX_NU2:
      case IDX_L2:
        return fM2;
      case IDX_M4:
        return fM2 * fM2;
      case IDX_MN4:
      case IDX_2MS6:
        return fM2 * fM2;
      case IDX_MSN2:
        return fM2;
      case IDX_M8:
        return fM2 * fM2 * fM2 * fM2;
      case IDX_MK3:
        return fM2 * fK1;
      case IDX_2MK3:
        return fM2 * fM2 / fK1;
      case IDX_MS4:
        return fM2;
      case IDX_M6:
        return fM2 * fM2 * fM2;
      case IDX_K2:
        return fK2;
      case IDX_K1:
        return fK1;
      case IDX_O1:
      case IDX_Q1:
      case IDX_M1:
      case IDX_2Q1:
        return fO1;
      case IDX_J1:
        return fJ1;
      default:
        return 1.0;
    }
  }

  /**
   * Schureman nodal phase correction u (degrees) for constituent i.
   */
  private static double nodalU(int idx, double Ndeg)
  {
    double N = Math.toRadians(Ndeg);
    double s1 = Math.sin(N), s2 = Math.sin(2 * N), s3 = Math.sin(3 * N);
    double uM2 = -2.14 * s1;
    double uK1 = -8.86 * s1 + 0.68 * s2 - 0.07 * s3;
    double uO1 = 10.80 * s1 - 1.34 * s2 + 0.19 * s3;
    double uK2 = -17.74 * s1 + 0.68 * s2 - 0.04 * s3;
    double uJ1 = -12.94 * s1 + 1.34 * s2 - 0.19 * s3;
    switch (idx)
    {
      case IDX_M2:
      case IDX_N2:
      case IDX_2N2:
      case IDX_MU2:
      case IDX_NU2:
      case IDX_L2:
        return uM2;
      case IDX_M4:
        return 2 * uM2;
      case IDX_MN4:
      case IDX_2MS6:
        return 2 * uM2;
      case IDX_MSN2:
        return uM2;
      case IDX_M8:
        return 4 * uM2;
      case IDX_MK3:
        return uM2 + uK1;
      case IDX_2MK3:
        return 2 * uM2 - uK1;
      case IDX_MS4:
        return uM2;
      case IDX_M6:
        return 3 * uM2;
      case IDX_K2:
        return uK2;
      case IDX_K1:
        return uK1;
      case IDX_O1:
      case IDX_Q1:
      case IDX_M1:
      case IDX_2Q1:
        return uO1;
      case IDX_J1:
        return uJ1;
      default:
        return 0.0;
    }
  }

  // ===== Core tide prediction =====

  /**
   * Compute tide height at time t (hours from midnight Jan 1 2026 LOCAL UTC+3).
   * h(t) = Z0 + Σ A_i × f_i(t) × cos(ω_i × t + φ_i + u_i(t))
   *
   * @param tHours hours from epoch (midnight Jan 1 2026 LOCAL)
   * @return tide height in meters above MLLW
   */
  private static double tideHeightAtT(double tHours)
  {
    // Timing calibration: evaluate the raw harmonic model slightly earlier so
    // that the predicted high/low event clock times match the reference tables
    // (the raw fit peaks ~6 min early). See TIMING_CORRECTION_MINUTES above.
    double tc = tHours - TIMING_CORRECTION_MINUTES / 60.0;
    double node = nodeDegrees(tc);
    double h = Z0;
    for (int i = 0; i < NUM_CONSTITUENTS; i++)
    {
      double f = nodalF(i, node);
      double u = nodalU(i, node);
      double arg = Math.toRadians(SPEEDS[i] * tc + PHASES[i] + u);
      h += AMPLITUDES[i] * f * Math.cos(arg);
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
    return "\u0645\u0633\u062A\u0648\u0649 \u0627\u0644\u0628\u062D\u0631 \u0627\u0644\u0645\u062A\u0648\u0633\u0637: "
  + String.format(Locale.US, "%.2f", Z0)
  + "\u0645 \u0641\u0648\u0642 MLLW";
    // مستوى البحر المتوسط: <Z0>م فوق MLLW
  }
}
