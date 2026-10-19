package app.alhaswamagellan.hawsa;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import app.alhaswamagellan.R;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Moon phase screen with beautiful animated phases, current phase info,
 * accurate rise/set times (Meeus/NOAA astronomical algorithms),
 * Hijri date, and realistic weekly moon icons.
 * Status bar is visible (not immersive) matching the dark navy background.
 */
public class HawsaMoonActivity extends AppCompatActivity
{
  private MoonPhaseView moonPhaseView;
  private ImageView btnBack;

  // Theme colors
  private static final int COLOR_BG = 0xFF0D1B2A;
  private static final int COLOR_CARD = 0xFF111827;
  private static final int COLOR_GOLD = 0xFFD4AF37;
  private static final int COLOR_TEXT = 0xFFE0E1DD;
  private static final int COLOR_TEXT_DIM = 0xFF778DA9;
  private static final int COLOR_SEPARATOR = 0xFF1E293B;

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

    // Build the entire layout programmatically for full control
    ScrollView scrollView = new ScrollView(this);
    scrollView.setBackgroundColor(COLOR_BG);
    scrollView.setFillViewport(true);
    scrollView.setPadding(0, 0, 0, 0);

    LinearLayout rootLayout = new LinearLayout(this);
    rootLayout.setOrientation(LinearLayout.VERTICAL);
    rootLayout.setBackgroundColor(COLOR_BG);
    rootLayout.setPadding(dp(20), dp(16), dp(20), dp(30));
    rootLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    // Add top padding for status bar space
    rootLayout.setPaddingRelative(dp(20), dp(12), dp(20), dp(30));
    rootLayout.setClipToPadding(false);

    // === Back button + Title ===
    LinearLayout topBar = new LinearLayout(this);
    topBar.setOrientation(LinearLayout.HORIZONTAL);
    topBar.setGravity(Gravity.CENTER_VERTICAL);
    topBar.setPadding(0, 0, 0, dp(16));

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
    tvTitle.setText("القمر");
    tvTitle.setTextSize(22);
    tvTitle.setTextColor(COLOR_GOLD);
    tvTitle.setTypeface(null, Typeface.BOLD);
    LinearLayout.LayoutParams titleParams =
        new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    tvTitle.setLayoutParams(titleParams);
    tvTitle.setGravity(Gravity.CENTER);

    // Invisible spacer to balance the back button
    View spacer = new View(this);
    spacer.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));

    topBar.addView(btnBack);
    topBar.addView(tvTitle);
    topBar.addView(spacer);
    rootLayout.addView(topBar);

    // === Animated Moon Phase View ===
    moonPhaseView = new MoonPhaseView(this);
    LinearLayout.LayoutParams moonParams =
        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(280));
    moonPhaseView.setLayoutParams(moonParams);
    rootLayout.addView(moonPhaseView);

    // === Current Phase Info Card ===
    Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
    long timeMillis = now.getTimeInMillis();

    // Use correct MoonCalculator API
    double age = MoonCalculator.getLunarAge(timeMillis);
    double illum = MoonCalculator.getIllumination(timeMillis);
    // Phase fraction: 0=new, 0.5=full, 1.0=new again
    float phaseFraction = (float) (age / MoonCalculator.SYNODIC_MONTH);

    // Set phase into animated view
    if (moonPhaseView != null)
    {
      moonPhaseView.setPhase(phaseFraction, (float) illum);
    }

    LinearLayout infoCard = createCard();

    // Phase name
    TextView tvPhaseName = new TextView(this);
    tvPhaseName.setText(MoonCalculator.getPhaseNameArabic(age));
    tvPhaseName.setTextSize(20);
    tvPhaseName.setTextColor(COLOR_GOLD);
    tvPhaseName.setTypeface(null, Typeface.BOLD);
    tvPhaseName.setGravity(Gravity.CENTER);
    tvPhaseName.setPadding(0, dp(16), 0, dp(4));
    infoCard.addView(tvPhaseName);

    // Illumination
    TextView tvIllumination = new TextView(this);
    tvIllumination.setText(String.format(Locale.US, "الإضاءة: %.0f%%", illum * 100));
    tvIllumination.setTextSize(16);
    tvIllumination.setTextColor(COLOR_TEXT);
    tvIllumination.setGravity(Gravity.CENTER);
    tvIllumination.setPadding(0, dp(4), 0, dp(8));
    infoCard.addView(tvIllumination);

    // Moon age
    TextView tvAge = new TextView(this);
    tvAge.setText(String.format(Locale.US, "عمر القمر: %.1f يوم", age));
    tvAge.setTextSize(14);
    tvAge.setTextColor(COLOR_TEXT_DIM);
    tvAge.setGravity(Gravity.CENTER);
    tvAge.setPadding(0, 0, 0, dp(12));
    infoCard.addView(tvAge);

    // Separator
    infoCard.addView(createSeparator());

    // Rise / Set times
    LinearLayout timesGrid = new LinearLayout(this);
    timesGrid.setOrientation(LinearLayout.HORIZONTAL);
    timesGrid.setGravity(Gravity.CENTER);
    timesGrid.setPadding(0, dp(12), 0, dp(8));

    String riseTime = MoonCalculator.getMoonriseTime(timeMillis);
    String setTime = MoonCalculator.getMoonsetTime(timeMillis);

    timesGrid.addView(createTimeColumn("الطلوع", riseTime));
    timesGrid.addView(createTimeColumn("الغروب", setTime));

    infoCard.addView(timesGrid);

    // Separator
    infoCard.addView(createSeparator());

    // Hijri date
    HijriDateCalculator.HijriDate hijriDate = HijriDateCalculator.calculateHijri(now);
    String hijriStr = HijriDateCalculator.formatHijriArabic(hijriDate);

    TextView tvHijri = new TextView(this);
    tvHijri.setText("التاريخ الهجري: " + hijriStr);
    tvHijri.setTextSize(16);
    tvHijri.setTextColor(COLOR_TEXT);
    tvHijri.setGravity(Gravity.CENTER);
    tvHijri.setTypeface(null, Typeface.BOLD);
    tvHijri.setPadding(0, dp(12), 0, dp(8));
    infoCard.addView(tvHijri);

    // Gregorian date
    SimpleDateFormat gregFmt = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("ar"));
    TextView tvGregorian = new TextView(this);
    tvGregorian.setText("التاريخ الميلادي: " + gregFmt.format(now.getTime()));
    tvGregorian.setTextSize(13);
    tvGregorian.setTextColor(COLOR_TEXT_DIM);
    tvGregorian.setGravity(Gravity.CENTER);
    tvGregorian.setPadding(0, dp(2), 0, dp(12));
    infoCard.addView(tvGregorian);

    rootLayout.addView(infoCard);

    // === Weekly Phase Strip ===
    LinearLayout weeklyCard = createCard();
    weeklyCard.setPadding(dp(16), dp(16), dp(16), dp(16));

    TextView tvWeeklyTitle = new TextView(this);
    tvWeeklyTitle.setText("أطوار القمر هذا الأسبوع");
    tvWeeklyTitle.setTextSize(15);
    tvWeeklyTitle.setTextColor(COLOR_GOLD);
    tvWeeklyTitle.setTypeface(null, Typeface.BOLD);
    tvWeeklyTitle.setPadding(0, 0, 0, dp(12));
    weeklyCard.addView(tvWeeklyTitle);

    List<MoonCalculator.MoonDayInfo> weekly = MoonCalculator.getWeeklyMoonInfo();

    LinearLayout weeklyStrip = new LinearLayout(this);
    weeklyStrip.setOrientation(LinearLayout.HORIZONTAL);
    weeklyStrip.setGravity(Gravity.CENTER);
    weeklyStrip.setPadding(0, 0, 0, dp(4));

    for (int i = 0; i < weekly.size() && i < 7; i++)
    {
      MoonCalculator.MoonDayInfo dayInfo = weekly.get(i);
      float dayPhase = (float) (dayInfo.age / MoonCalculator.SYNODIC_MONTH);
      float dayIllum = (float) dayInfo.illumination;
      boolean isToday = (i == 0);
      weeklyStrip.addView(createRealisticDayMoonIcon(dayPhase, dayIllum, isToday));
    }

    weeklyCard.addView(weeklyStrip);

    // Day labels under strip
    LinearLayout dayLabels = new LinearLayout(this);
    dayLabels.setOrientation(LinearLayout.HORIZONTAL);
    dayLabels.setGravity(Gravity.CENTER);

    String[] shortDayNames = {"أح", "إث", "ثل", "أر", "خم", "جم", "سب"};

    for (int i = 0; i < weekly.size() && i < 7; i++)
    {
      MoonCalculator.MoonDayInfo dayInfo = weekly.get(i);
      int dow = dayInfo.date.get(Calendar.DAY_OF_WEEK) - 1;
      TextView lbl = new TextView(this);
      lbl.setText(shortDayNames[dow]);
      lbl.setTextSize(11);
      lbl.setTextColor(i == 0 ? COLOR_GOLD : COLOR_TEXT_DIM);
      lbl.setGravity(Gravity.CENTER);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
      lbl.setLayoutParams(lp);
      dayLabels.addView(lbl);
    }

    weeklyCard.addView(dayLabels);

    // Weekly details with rise/set times
    LinearLayout weeklyDetails = new LinearLayout(this);
    weeklyDetails.setOrientation(LinearLayout.VERTICAL);
    weeklyDetails.setPadding(0, dp(12), 0, 0);

    String[] arabicDays = {"الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"};

    for (int i = 0; i < weekly.size() && i < 7; i++)
    {
      MoonCalculator.MoonDayInfo dayInfo = weekly.get(i);
      LinearLayout row = new LinearLayout(this);
      row.setOrientation(LinearLayout.HORIZONTAL);
      row.setGravity(Gravity.CENTER_VERTICAL);
      row.setPadding(0, dp(6), 0, dp(6));

      if (i == 0)
      {
        row.setBackgroundColor(0x15D4AF37);
      }

      // Day name
      int dow = dayInfo.date.get(Calendar.DAY_OF_WEEK) - 1;
      TextView tvDayName = new TextView(this);
      tvDayName.setText(arabicDays[dow]);
      tvDayName.setTextSize(14);
      tvDayName.setTextColor(i == 0 ? COLOR_GOLD : COLOR_TEXT);
      tvDayName.setTypeface(null, i == 0 ? Typeface.BOLD : Typeface.NORMAL);
      LinearLayout.LayoutParams nameParams =
          new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
      tvDayName.setLayoutParams(nameParams);
      row.addView(tvDayName);

      // Phase name + illumination
      TextView tvPhaseInfo = new TextView(this);
      tvPhaseInfo.setText(String.format(Locale.US, "%s · %.0f%%", dayInfo.phaseName, dayInfo.illumination * 100));
      tvPhaseInfo.setTextSize(11);
      tvPhaseInfo.setTextColor(COLOR_TEXT_DIM);
      LinearLayout.LayoutParams phaseParams =
          new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
      tvPhaseInfo.setLayoutParams(phaseParams);
      tvPhaseInfo.setGravity(Gravity.CENTER);
      row.addView(tvPhaseInfo);

      // Rise/Set
      TextView tvTimes = new TextView(this);
      String timesStr = String.format(Locale.US, "ط %s | غ %s", dayInfo.moonrise, dayInfo.moonset);
      tvTimes.setText(timesStr);
      tvTimes.setTextSize(11);
      tvTimes.setTextColor(i == 0 ? 0xFFD4AF37 : 0xFFCE93D8);
      LinearLayout.LayoutParams timesParams =
          new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
      tvTimes.setLayoutParams(timesParams);
      tvTimes.setGravity(Gravity.CENTER);
      row.addView(tvTimes);

      weeklyDetails.addView(row);

      if (i < 6)
      {
        View sep = new View(this);
        sep.setBackgroundColor(COLOR_SEPARATOR);
        LinearLayout.LayoutParams sepP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1);
        sep.setLayoutParams(sepP);
        weeklyDetails.addView(sep);
      }
    }

    weeklyCard.addView(weeklyDetails);
    rootLayout.addView(weeklyCard);

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
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    if (moonPhaseView != null)
    {
      moonPhaseView.startAllAnimations();
    }
  }

  @Override
  protected void onPause()
  {
    super.onPause();
    if (moonPhaseView != null)
    {
      moonPhaseView.stopAllAnimations();
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
    bg.setStroke(dp(1), 0xFF1E293B);
    card.setBackground(bg);

    LinearLayout.LayoutParams params =
        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    params.setMargins(0, dp(12), 0, 0);
    card.setLayoutParams(params);

    return card;
  }

  private View createSeparator()
  {
    View sep = new View(this);
    sep.setBackgroundColor(COLOR_SEPARATOR);
    LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
    sepParams.setMargins(dp(32), 0, dp(32), 0);
    sep.setLayoutParams(sepParams);
    return sep;
  }

  private LinearLayout createTimeColumn(String label, String timeStr)
  {
    LinearLayout col = new LinearLayout(this);
    col.setOrientation(LinearLayout.VERTICAL);
    col.setGravity(Gravity.CENTER);
    col.setPadding(dp(8), 0, dp(8), 0);
    LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    col.setLayoutParams(colParams);

    TextView tvLabel = new TextView(this);
    tvLabel.setText(label);
    tvLabel.setTextSize(12);
    tvLabel.setTextColor(COLOR_TEXT_DIM);
    tvLabel.setGravity(Gravity.CENTER);
    col.addView(tvLabel);

    TextView tvTime = new TextView(this);
    tvTime.setText(timeStr);
    tvTime.setTextSize(18);
    tvTime.setTextColor(COLOR_GOLD);
    tvTime.setTypeface(null, Typeface.BOLD);
    tvTime.setGravity(Gravity.CENTER);
    col.addView(tvTime);

    return col;
  }

  /**
   * Create a realistic moon icon for the weekly strip using the physically
   * accurate terminator model matching MoonPhaseView.
   *
   * The terminator is the projection of the great circle dividing the Moon's
   * lit and dark hemispheres. When projected onto the 2D disk, it forms an
   * ellipse whose x-offset = r * cos(2*PI*p), with the sign flipped for
   * waning phases so the visible terminator is on the correct side.
   *
   * Phase mapping (Northern Hemisphere):
   *   0.00 = new moon (all dark)
   *   0.25 = first quarter (right half lit)
   *   0.50 = full moon (all lit)
   *   0.75 = last quarter (left half lit)
   *   1.00 = new moon (all dark)
   */
  @SuppressLint("ViewConstructor")
  private View createRealisticDayMoonIcon(float phase, float illumination, boolean isToday)
  {
    View icon = new View(this) {
      @Override
      protected void onDraw(Canvas canvas)
      {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float r = Math.min(w, h) / 2f - dp(4);

        float p = phase;
        double illum = 0.5 * (1.0 - Math.cos(2.0 * Math.PI * p));

        // Moon lit base
        Paint litPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        litPaint.setColor(isToday ? 0xFFF1F5F9 : 0xFF94A3B8);
        litPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, r, litPaint);

        // Edge cases: full moon or new moon
        if (illum >= 0.99)
        {
          // Full moon — all lit, no dark overlay
          if (isToday)
          {
            Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            ringPaint.setColor(COLOR_GOLD);
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(2f);
            canvas.drawCircle(cx, cy, r + dp(1), ringPaint);
          }
          return;
        }
        else if (illum <= 0.01)
        {
          // New moon — all dark
          Paint darkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
          darkPaint.setColor(0xFF1E293B);
          darkPaint.setStyle(Paint.Style.FILL);
          canvas.drawCircle(cx, cy, r, darkPaint);
          if (isToday)
          {
            Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            ringPaint.setColor(COLOR_GOLD);
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(2f);
            canvas.drawCircle(cx, cy, r + dp(1), ringPaint);
          }
          return;
        }

        // Physically accurate terminator position
        boolean isWaxing = (p <= 0.5f);
        double signedTerminatorX = (isWaxing ? 1.0 : -1.0) * r * Math.cos(2.0 * Math.PI * p);

        // Dark overlay using terminator semi-ellipse + dark limb arc
        Paint darkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        darkPaint.setColor(0xFF1E293B);
        darkPaint.setStyle(Paint.Style.FILL);

        Path darkPath = new Path();

        // Start at top of moon disk
        darkPath.moveTo(cx, cy - r);

        // Draw the dark limb arc
        if (isWaxing)
        {
          // Waxing: right side lit, dark limb is LEFT side
          darkPath.arcTo(cx - r, cy - r, cx + r, cy + r, -90, -180, true);
        }
        else
        {
          // Waning: left side lit, dark limb is RIGHT side
          darkPath.arcTo(cx - r, cy - r, cx + r, cy + r, -90, 180, false);
        }

        // Walk along the terminator from bottom back to top
        // The terminator is a semi-ellipse: x = cx + signedTerminatorX * cos(angle), y = cy + r * sin(angle)
        // angle goes from +90 (bottom) to -90 (top)
        int steps = 36;
        for (int i = steps; i >= 0; i--)
        {
          float angle = -90f + (180f * i / steps);
          float y = cy + r * (float) Math.sin(Math.toRadians(angle));
          float xTerminator = cx + (float) (signedTerminatorX * Math.cos(Math.toRadians(angle)));
          darkPath.lineTo(xTerminator, y);
        }

        darkPath.close();
        canvas.drawPath(darkPath, darkPaint);

        // Highlight ring for today
        if (isToday)
        {
          Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
          ringPaint.setColor(COLOR_GOLD);
          ringPaint.setStyle(Paint.Style.STROKE);
          ringPaint.setStrokeWidth(2f);
          canvas.drawCircle(cx, cy, r + dp(1), ringPaint);
        }
      }
    };

    int iconSize = dp(40);
    LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(0, iconSize, 1f);
    icon.setLayoutParams(iconParams);
    return icon;
  }
}
