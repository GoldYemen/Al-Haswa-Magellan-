package app.alhaswamagellan.hawsa;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import app.alhaswamagellan.R;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Tides activity for Aden, Yemen — fully offline, no GPS required.
 * Displays: daily high/low table, current status indicator,
 * 24-hour line chart, 7-day navigation pills, weekly summary.
 * All time formatting uses English AM/PM (not Arabic ص/م).
 */
public class HawsaTidesActivity extends AppCompatActivity
{
  private TideChartView tideChartView;
  private LinearLayout dayButtonContainer;
  private LinearLayout weeklyTidesContainer;
  private TextView tvTidesTitle;
  private TextView tvTidesLocation;
  private TextView tvCurrentStatus;
  private ImageView btnBack;

  private int selectedDayOffset = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    // Dark status bar matching app theme
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
    {
      getWindow().setStatusBarColor(0xFF0D1B2A);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
      {
        View decor = getWindow().getDecorView();
        int flags = decor.getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        decor.setSystemUiVisibility(flags);
      }
    }

    setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    setContentView(R.layout.activity_hawsa_tides);

    btnBack = findViewById(R.id.btn_tides_back);
    tvTidesTitle = findViewById(R.id.tv_tides_title);
    tvTidesLocation = findViewById(R.id.tv_tides_location);
    tvCurrentStatus = findViewById(R.id.tv_current_status);
    tideChartView = findViewById(R.id.tide_chart);
    dayButtonContainer = findViewById(R.id.day_button_container);
    weeklyTidesContainer = findViewById(R.id.weekly_tides_container);

    if (btnBack != null)
    {
      btnBack.setOnClickListener(v -> finish());
    }

    if (tvTidesTitle != null)
      tvTidesTitle.setText("\u0627\u0644\u0645\u062F \u0648\u0627\u0644\u062C\u0632\u0631"); // المد والجزر

    // Hardcoded Aden station — no GPS, no location permissions
    if (tvTidesLocation != null)
    {
      tvTidesLocation.setText("\u0645\u062D\u0637\u0629 \u0639\u062F\u0646"); // محطة عدن
    }

    // Build day pills and refresh data
    buildDayPills();
    updateCurrentStatus();
    refreshTideData();
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    updateCurrentStatus();
  }

  /**
   * Update the current tide status indicator (rising/high/falling/low).
   */
  private void updateCurrentStatus()
  {
    if (tvCurrentStatus == null)
      return;

    String status = TideCalculator.getCurrentStatus();
    String statusText;
    int textColor;

    switch (status)
    {
    case "RISING":
      statusText = "\u0645\u062F \u0635\u0627\u0639\u062F \u2191"; // مد مملي ↑
      textColor = 0xFF10B981; // green
      break;
    case "HIGH":
      statusText = "\u0645\u062F \u0645\u0645\u0644\u064A \u25CF"; // مد مملي ●
      textColor = 0xFF10B981; // green
      break;
    case "FALLING":
      statusText = "\u062C\u0632\u0631 \u0646\u0627\u0632\u0644 \u2193"; // جزر نازل ↓
      textColor = 0xFFEF4444; // red
      break;
    case "LOW":
      statusText = "\u062C\u0632\u0631 \u0645\u0646\u062E\u0641\u0636 \u25CF"; // جزر نازل ●
      textColor = 0xFFEF4444; // red
      break;
    default: statusText = ""; textColor = 0xFF64748B;
    }

    tvCurrentStatus.setText(statusText);
    tvCurrentStatus.setTextColor(textColor);
  }

  /**
   * Modern pill-style day selector buttons (7 days).
   */
  private void buildDayPills()
  {
    if (dayButtonContainer == null)
      return;
    dayButtonContainer.removeAllViews();

    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+3"));

    for (int i = 0; i < 7; i++)
    {
      TextView pill = new TextView(this);

      if (i == 0)
      {
        pill.setText("\u0627\u0644\u064A\u0648\u0645"); // اليوم
      }
      else
      {
        Calendar offset = (Calendar) cal.clone();
        offset.add(Calendar.DAY_OF_MONTH, i);
        String[] arabicDays = {
            "\u0627\u0644\u0623\u062D\u062F", // الأحد
            "\u0627\u0644\u0627\u062B\u0646\u064A\u0646", // الاثنين
            "\u0627\u0644\u062B\u0644\u0627\u062B\u0627\u0621", // الثلاثاء
            "\u0627\u0644\u0623\u0631\u0628\u0639\u0627\u0621", // الأربعاء
            "\u0627\u0644\u062E\u0645\u064A\u0633", // الخميس
            "\u0627\u0644\u062C\u0645\u0639\u0629", // الجمعة
            "\u0627\u0644\u0633\u0628\u062A" // السبت
        };
        int dayOfWeek = offset.get(Calendar.DAY_OF_WEEK) - 1;
        pill.setText(arabicDays[dayOfWeek]);
      }

      pill.setTextSize(13);
      pill.setGravity(Gravity.CENTER);
      pill.setAllCaps(false);
      pill.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

      boolean selected = (i == selectedDayOffset);
      pill.setTextColor(selected ? 0xFF0A0E1A : 0xFF64748B);

      int pillPaddingH = 20;
      int pillPaddingV = 10;
      pill.setPadding(pillPaddingH, pillPaddingV, pillPaddingH, pillPaddingV);

      android.graphics.drawable.GradientDrawable pillBg = new android.graphics.drawable.GradientDrawable();
      pillBg.setCornerRadius(20f);
      pillBg.setColor(selected ? 0xFFD4AF37 : 0xFF1E293B);
      pillBg.setStroke(selected ? 0 : 1, selected ? 0 : 0xFF334155);
      pill.setBackground(pillBg);

      final int dayOffset = i;
      pill.setOnClickListener(v -> {
        selectedDayOffset = dayOffset;
        buildDayPills();
        refreshTideData();
      });

      LinearLayout.LayoutParams params =
          new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
      params.setMargins(6, 0, 6, 0);
      pill.setLayoutParams(params);
      dayButtonContainer.addView(pill);
    }
  }

  /**
   * Refresh the tide chart and data for the selected day.
   */
  private void refreshTideData()
  {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+3"));
    cal.add(Calendar.DAY_OF_MONTH, selectedDayOffset);

    List<TideCalculator.TidePoint> points = TideCalculator.getDayTidePoints(cal);
    List<TideCalculator.TideEvent> events = TideCalculator.getDayTideEvents(cal);

    if (tideChartView != null)
    {
      tideChartView.setDayData(cal, points, events);
    }

    updateWeeklySummaryText();
  }

  /**
   * Build the weekly summary text with Arabic labels and English AM/PM times.
   */
  @SuppressLint("DefaultLocale")
  private void updateWeeklySummaryText()
  {
    if (weeklyTidesContainer == null)
      return;

    List<TideCalculator.DayTideSummary> summary = TideCalculator.getWeeklySummary();

    String[] arabicDays = {
        "\u0627\u0644\u0623\u062D\u062F", // الأحد
        "\u0627\u0644\u0627\u062B\u0646\u064A\u0646", // الاثنين
        "\u0627\u0644\u062B\u0644\u0627\u062B\u0627\u0621", // الثلاثاء
        "\u0627\u0644\u0623\u0631\u0628\u0639\u0627\u0621", // الأربعاء
        "\u0627\u0644\u062E\u0645\u064A\u0633", // الخميس
        "\u0627\u0644\u062C\u0645\u0639\u0629", // الجمعة
        "\u0627\u0644\u0633\u0628\u062A" // السبت
    };
    SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM", Locale.US);

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < summary.size(); i++)
    {
      TideCalculator.DayTideSummary day = summary.get(i);
      int dayOfWeek = day.date.get(Calendar.DAY_OF_WEEK) - 1;

      sb.append(arabicDays[dayOfWeek]).append(" ").append(dateFmt.format(day.date.getTime()));
      sb.append("\n");

      if (day.firstHigh != null)
      {
        sb.append("  \u0645\u062F \u0645\u0645\u0644\u064A: ").append(formatEventTime(day.firstHigh)); // مد مملي:
        sb.append(" (").append(String.format(Locale.US, "%.1fm", day.firstHigh.height)).append(")");
        if (day.secondHigh != null)
        {
          sb.append(" | ").append(formatEventTime(day.secondHigh));
          sb.append(" (").append(String.format(Locale.US, "%.1fm", day.secondHigh.height)).append(")");
        }
        sb.append("\n");
      }

      if (day.firstLow != null)
      {
        sb.append("  \u062C\u0632\u0631 \u0645\u0646\u062E\u0641\u0636: ")
            .append(formatEventTime(day.firstLow)); // الجزر المنخفض:
        sb.append(" (").append(String.format(Locale.US, "%.1fm", day.firstLow.height)).append(")");
        if (day.secondLow != null)
        {
          sb.append(" | ").append(formatEventTime(day.secondLow));
          sb.append(" (").append(String.format(Locale.US, "%.1fm", day.secondLow.height)).append(")");
        }
        sb.append("\n");
      }

      if (day.maxHeight > Double.MIN_VALUE)
      {
        sb.append("  \u2191").append(String.format(Locale.US, "%.1f", day.maxHeight)).append("m ");
      }
      if (day.minHeight < Double.MAX_VALUE)
      {
        sb.append("\u2193").append(String.format(Locale.US, "%.1f", day.minHeight)).append("m");
      }
      sb.append("\n\n");
    }

    // Designer credit — MUST NOT be modified
    sb.append(
        "\u062A\u0645 \u062A\u0635\u0645\u064A\u0645 \u0647\u0630\u0627 \u0627\u0644\u062A\u0637\u0628\u064A\u0642 "
        + "\u0645\u0646 \u0642\u0628\u0644 \u0623\u0635\u064A\u0644 \u0635\u0627\u062F\u0642");
    // تم تصميم هذا التطبيق من قبل أصيل صادق

    TextView summaryText = null;
    if (weeklyTidesContainer.getChildCount() > 0)
    {
      summaryText = (TextView) weeklyTidesContainer.getChildAt(0);
    }
    if (summaryText == null)
    {
      summaryText = new TextView(this);
      summaryText.setTextSize(12);
      summaryText.setTextColor(0xFFE0E1DD);
      weeklyTidesContainer.addView(summaryText);
    }
    summaryText.setText(sb.toString().trim());
  }

  /**
   * Format event time using English AM/PM (NOT Arabic ص/م).
   * Uses 12-hour format with English AM/PM suffix.
   */
  private String formatEventTime(TideCalculator.TideEvent event)
  {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+3"));
    cal.setTimeInMillis(event.timeMillis);
    int h = cal.get(Calendar.HOUR);
    if (h == 0)
      h = 12;
    int m = cal.get(Calendar.MINUTE);
    String ap = cal.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
    return String.format(Locale.US, "%d:%02d%s", h, m, ap);
  }
}
