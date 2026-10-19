package app.alhaswamagellan.sdk.util;

import app.alhaswamagellan.sdk.BuildConfig;

public class Assert
{
  public static void debug(boolean condition, String message)
  {
    if (BuildConfig.DEBUG && !condition)
      throw new AssertionError(message);
  }

  public static void always(boolean condition, String message)
  {
    if (!condition)
      throw new AssertionError(message);
  }
}
