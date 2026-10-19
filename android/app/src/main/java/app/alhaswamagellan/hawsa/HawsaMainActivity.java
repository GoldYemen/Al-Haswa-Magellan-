package app.alhaswamagellan.hawsa;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import app.alhaswamagellan.MwmActivity;
import app.alhaswamagellan.R;

public class HawsaMainActivity extends AppCompatActivity
{
  private static final double DEFAULT_LAT = 15.3694;
  private static final double DEFAULT_LON = 44.1910;
  private static final int DEFAULT_ZOOM = 4;  // ~2000 km view radius

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    // Window setup - dark status bar matching main background
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
    {
      getWindow().setDecorFitsSystemWindows(true);
    }
    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
    {
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
    {
      getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.hawsa_bg_main));
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
      {
        View decor = getWindow().getDecorView();
        int flags = decor.getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        decor.setSystemUiVisibility(flags);
      }
    }

    // Force RTL layout direction for Arabic
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
    {
      getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    }

    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    setContentView(R.layout.activity_hawsa_main);

    // Card click listeners
    findViewById(R.id.card_map).setOnClickListener(v -> openMap());

    findViewById(R.id.card_tides).setOnClickListener(v -> {
      try
      {
        startActivity(new Intent(this, HawsaTidesActivity.class));
      }
      catch (Exception e)
      { /* TODO */
      }
    });

    findViewById(R.id.card_moon).setOnClickListener(v -> {
      try
      {
        startActivity(new Intent(this, HawsaMoonActivity.class));
      }
      catch (Exception e)
      { /* TODO */
      }
    });

    findViewById(R.id.card_compass).setOnClickListener(v -> {
      try
      {
        startActivity(new Intent(this, HawsaCompassActivity.class));
      }
      catch (Exception e)
      { /* TODO */
      }
    });
  }

  private void openMap()
  {
    Intent intent = new Intent(this, MwmActivity.class);
    intent.putExtra(MwmActivity.EXTRA_HAWSA_LAT, DEFAULT_LAT);
    intent.putExtra(MwmActivity.EXTRA_HAWSA_LON, DEFAULT_LON);
    intent.putExtra(MwmActivity.EXTRA_HAWSA_ZOOM, DEFAULT_ZOOM);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivity(intent);
  }
}
