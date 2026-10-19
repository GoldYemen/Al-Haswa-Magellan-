package app.alhaswamagellan.sdk.location.gms;

import android.content.Context;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import app.alhaswamagellan.sdk.location.BaseLocationProvider;
import app.alhaswamagellan.sdk.location.GmsLocationProviderFactory;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

@Keep // Accessed via Reflection.
final class GmsLocationProviderFactoryImpl implements GmsLocationProviderFactory
{
  @Override
  public boolean isProviderAvailable(@NonNull Context context)
  {
    return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
  }

  @Override
  @NonNull
  public BaseLocationProvider getProvider(@NonNull Context context, @NonNull BaseLocationProvider.Listener listener)
  {
    return new GoogleFusedLocationProvider(context, listener);
  }
}
