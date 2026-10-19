package app.alhaswamagellan.sdk;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import app.alhaswamagellan.sdk.widget.placepage.PlacePageData;

public interface PlacePageActivationListener
{
  // Called from JNI.
  @Keep
  @SuppressWarnings("unused")
  void onPlacePageActivated(@NonNull PlacePageData data);

  // Called from JNI
  @Keep
  @SuppressWarnings("unused")
  void onPlacePageDeactivated();

  // Called from JNI
  @Keep
  @SuppressWarnings("unused")
  default void onSwitchFullScreenMode()
  {}
}
