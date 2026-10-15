package app.organicmaps.sdk;

import android.content.Context;
import android.content.res.AssetManager;
import androidx.annotation.NonNull;
import app.organicmaps.sdk.util.log.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Copies the pre-bundled Yemen.mwm from APK assets to the writable data directory
 * so the native core can discover it as a country map.
 *
 * Country maps are NOT read from APK assets (unlike World.mwm/WorldCoasts.mwm
 * which the C++ GetReader finds inside the APK zip). They must reside under
 * {writablePath}/{dataVersion}/ for the native core to find them.
 *
 * IMPORTANT: This class does NOT call Framework.nativeGetDataVersion() because
 * that native method accesses g_framework->NativeFramework(), which is NULL
 * until nativeInitFramework() runs. Calling it before nativeInitFramework()
 * causes a fatal SIGSEGV (native crash) — not a catchable Java exception.
 * The data version is therefore hardcoded from the bundled countries.json
 * (which contains "v":260714) and must be updated when the map bundle is updated.
 */
public final class YemenMapPreloader
{
  private static final String TAG = YemenMapPreloader.class.getSimpleName();

  /** Asset path inside the APK for the Yemen map. */
  private static final String ASSET_NAME = "Yemen.mwm";

  /**
   * Map data version — MUST match the version embedded in the bundled
   * countries.json ("v": field) and the Yemen.mwm file.
   * Format: yyMMdd — 260714 = July 14, 2026.
   * Updated in sync with the CDN at:
   *   https://cdn.organicmaps.app/maps/{DATA_VERSION}/Yemen.mwm
   *
   * Do NOT call Framework.nativeGetDataVersion() here — g_framework is NULL
   * before nativeInitFramework() and will crash the process (SIGSEGV).
   */
  private static final String DATA_VERSION = "260714";

  private YemenMapPreloader() {}

  /**
   * Copies Yemen.mwm from APK assets to {writablePath}/{DATA_VERSION}/ if not already present.
   *
   * This MUST be called AFTER nativeInitPlatform() (so the platform is ready)
   * but BEFORE nativeInitFramework() (so the framework discovers the map
   * when its Storage constructor scans local files via FindAllLocalMapsAndCleanup).
   *
   * @param context     application context (for AssetManager)
   * @param writablePath the writable data directory returned by StoragePathManager.findMapsStorage()
   * @return true if the map file exists in the target directory after this call
   */
  public static boolean preloadYemenMap(@NonNull Context context, @NonNull String writablePath)
  {
    final String targetDir = writablePath + File.separator + DATA_VERSION;
    final File targetFile = new File(targetDir, ASSET_NAME);

    // Skip if already present (subsequent launches)
    if (targetFile.exists() && targetFile.length() > 0)
    {
      Logger.i(TAG, "Yemen.mwm already exists at " + targetFile.getPath()
                  + " (" + targetFile.length() + " bytes), skipping copy");
      return true;
    }

    // Ensure the data version subdirectory exists
    final File dir = new File(targetDir);
    if (!dir.exists() && !dir.mkdirs())
    {
      Logger.e(TAG, "Failed to create directory: " + targetDir);
      return false;
    }

    Logger.i(TAG, "Pre-loading Yemen.mwm from APK assets to " + targetFile.getPath());

    final AssetManager assets = context.getAssets();
    try (InputStream in = assets.open(ASSET_NAME))
    {
      try (OutputStream out = new FileOutputStream(targetFile))
      {
        byte[] buf = new byte[8192];
        int len;
        long total = 0;
        long lastLog = 0;
        while ((len = in.read(buf)) > 0)
        {
          out.write(buf, 0, len);
          total += len;
          // Log progress every ~10 MB
          if (total - lastLog >= 10 * 1024 * 1024)
          {
            Logger.d(TAG, "  copied " + (total / (1024 * 1024)) + " MB so far...");
            lastLog = total;
          }
        }
        out.flush();
        Logger.i(TAG, "Yemen.mwm copied successfully: " + total + " bytes");
        return true;
      }
    }
    catch (IOException e)
    {
      Logger.e(TAG, "Failed to copy Yemen.mwm from assets", e);
      // Clean up partial file so we don't leave a corrupt .mwm
      if (targetFile.exists())
        targetFile.delete();
      return false;
    }
  }
}