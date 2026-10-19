package app.alhaswamagellan.sdk.car.renderer;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.lifecycle.LifecycleOwner;
import app.alhaswamagellan.sdk.display.DisplayManager;
import app.alhaswamagellan.sdk.location.LocationHelper;

public final class RendererFactory
{
  @NonNull
  public static Renderer create(@NonNull CarContext carContext, @NonNull DisplayManager displayManager,
                                @NonNull LocationHelper locationHelper, @NonNull LifecycleOwner lifecycleOwner)
  {
    return new SurfaceRenderer(carContext, displayManager, locationHelper, lifecycleOwner);
  }
}
