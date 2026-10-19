package app.alhaswamagellan.car;

import androidx.annotation.NonNull;
import androidx.car.app.Screen;
import androidx.car.app.SessionInfo;
import androidx.lifecycle.LifecycleOwner;
import app.alhaswamagellan.R;
import app.alhaswamagellan.car.screens.ErrorScreen;
import app.alhaswamagellan.car.screens.MapPlaceholderScreen;
import app.alhaswamagellan.car.screens.MapScreen;
import app.alhaswamagellan.car.screens.download.DownloadMapsScreenBuilder;
import app.alhaswamagellan.car.screens.download.DownloaderHelpers;
import app.alhaswamagellan.car.screens.permissions.RequestPermissionsScreenBuilder;
import app.alhaswamagellan.car.util.UserActionRequired;
import app.alhaswamagellan.sdk.OrganicMaps;
import app.alhaswamagellan.sdk.display.DisplayChangedListener;
import app.alhaswamagellan.sdk.display.DisplayManager;
import app.alhaswamagellan.sdk.display.DisplayType;
import app.alhaswamagellan.sdk.location.LocationUtils;
import app.alhaswamagellan.sdk.util.log.Logger;
import java.util.ArrayList;
import java.util.List;

public final class AndroidAutoSession extends CarAppSessionBase implements DisplayChangedListener
{
  private static final String TAG = AndroidAutoSession.class.getSimpleName();

  private final boolean mInitFailed;

  public AndroidAutoSession(@NonNull OrganicMaps organicMapsContext, @NonNull DisplayManager displayManager,
                            @NonNull SessionInfo sessionInfo, boolean isDebug, boolean initFailed)
  {
    super(organicMapsContext, displayManager, sessionInfo, isDebug);
    mInitFailed = initFailed;
  }

  @Override
  public void onCreate(@NonNull LifecycleOwner owner)
  {
    super.onCreate(owner);
    mDisplayManager.addListener(DisplayType.Car, this);
  }

  @Override
  public void onDestroy(@NonNull LifecycleOwner owner)
  {
    super.onDestroy(owner);
    mDisplayManager.removeListener(DisplayType.Car);
  }

  @NonNull
  protected Screen prepareScreens()
  {
    if (mInitFailed)
      return new ErrorScreen.Builder(getCarContext(), mOrganicMapsContext)
          .setErrorMessage(R.string.dialog_error_storage_message)
          .build();

    final List<Screen> screensStack = new ArrayList<>();
    screensStack.add(new MapScreen(getCarContext(), mOrganicMapsContext, mSurfaceRenderer));

    if (DownloaderHelpers.isWorldMapsDownloadNeeded(mOrganicMapsContext.getFlavor()))
    {
      mScreenManager.push(new DownloadMapsScreenBuilder(getCarContext(), mOrganicMapsContext)
                              .setDownloaderType(DownloadMapsScreenBuilder.DownloaderType.FirstLaunch)
                              .build());
    }

    if (!LocationUtils.checkFineLocationPermission(getCarContext()))
      screensStack.add(
          RequestPermissionsScreenBuilder.build(getCarContext(), mOrganicMapsContext, mSensorsManager::onStart));

    if (mDisplayManager.isDeviceDisplayUsed())
    {
      mSurfaceRenderer.disable();
      onStop(this);
      screensStack.add(new MapPlaceholderScreen(getCarContext(), mOrganicMapsContext));
    }

    for (int i = 0; i < screensStack.size() - 1; i++)
      mScreenManager.push(screensStack.get(i));

    return screensStack.get(screensStack.size() - 1);
  }

  @Override
  public void onDisplayChangedToDevice(@NonNull Runnable onTaskFinishedCallback)
  {
    Logger.d(TAG);
    final Screen topScreen = mScreenManager.getTop();
    onStop(this);
    mSurfaceRenderer.disable();

    final MapPlaceholderScreen mapPlaceholderScreen = new MapPlaceholderScreen(getCarContext(), mOrganicMapsContext);
    if (topScreen instanceof UserActionRequired)
      mScreenManager.popToRoot();

    mScreenManager.push(mapPlaceholderScreen);

    onTaskFinishedCallback.run();
  }

  @Override
  public void onDisplayChangedToCar(@NonNull Runnable onTaskFinishedCallback)
  {
    Logger.d(TAG);
    onStart(this);
    mSurfaceRenderer.enable();

    if (mScreenManager.getTop() instanceof MapPlaceholderScreen)
      mScreenManager.pop();

    onTaskFinishedCallback.run();
  }
}
