package app.alhaswamagellan.hawsa;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Hijri (Islamic) date calculator for Al-Haswa
 * Uses astronomical approximation based on synodic month
 * All strings in Arabic
 */
public class HijriDateCalculator
{
  // Reference: 1 Muharram 1442 AH = Aug 20 2020 CE
  private static final long REF_HIJRI_MILLIS;
  private static final int REF_HIJRI_YEAR = 1442;
  private static final int REF_HIJRI_MONTH = 1; // Muharram
  private static final int REF_HIJRI_DAY = 1;

  static
  {
    Calendar ref = new GregorianCalendar(2020, Calendar.AUGUST, 20, 0, 0, 0);
    ref.setTimeZone(TimeZone.getTimeZone("Asia/Riyadh"));
    REF_HIJRI_MILLIS = ref.getTimeInMillis();
  }

  // Synodic month used for Hijri calculation
  private static final double SYNODIC_MONTH = 29.53058867;

  // Arabic month names
  private static final String[] HIJRI_MONTH_NAMES = {"محرم",         "صفر",          "ربيع الأول", "ربيع الآخر",
                                                     "جمادى الأولى", "جمادى الآخرة", "رجب",        "شعبان",
                                                     "رمضان",        "شوال",         "ذو القعدة",  "ذو الحجة"};

  /**
   * Hijri date container
   */
  public static class HijriDate
  {
    public int day;
    public int month; // 1-12
    public int year;
    public String monthName;

    public HijriDate(int day, int month, int year)
    {
      this.day = day;
      this.month = month;
      this.year = year;
      this.monthName = HIJRI_MONTH_NAMES[month - 1];
    }
  }

  /**
   * Calculate Hijri date from a Gregorian Calendar
   */
  public static HijriDate calculateHijri(Calendar gregorian)
  {
    long timeMillis = gregorian.getTimeInMillis();
    double daysSinceRef = (timeMillis - REF_HIJRI_MILLIS) / 86400000.0;

    // Total lunar months since reference
    double totalMonths = daysSinceRef / SYNODIC_MONTH;

    // Starting point: 1 Muharram 1442 = month index (1442-1)*12 + (1-1) = 17292
    double totalHijriMonths = (REF_HIJRI_YEAR - 1) * 12 + (REF_HIJRI_MONTH - 1) + totalMonths;

    int hijriYear = (int) (totalHijriMonths / 12) + 1;
    int hijriMonth = (int) (totalHijriMonths % 12) + 1;
    if (hijriMonth > 12)
    {
      hijriMonth = 12;
    }

    // Day within the month
    double monthFraction = totalHijriMonths - (int) totalHijriMonths;
    int hijriDay = (int) (monthFraction * SYNODIC_MONTH) + 1;
    if (hijriDay > 30)
      hijriDay = 30;
    if (hijriDay < 1)
      hijriDay = 1;

    // Adjust for month overflow
    if (hijriMonth > 12)
    {
      hijriMonth = 1;
      hijriYear++;
    }

    return new HijriDate(hijriDay, hijriMonth, hijriYear);
  }

  /**
   * Format Hijri date in Arabic: "DD month YYYY هـ"
   */
  public static String formatHijriArabic(HijriDate hijriDate)
  {
    return String.format("%d %s %d هـ", hijriDate.day, hijriDate.monthName, hijriDate.year);
  }
}
