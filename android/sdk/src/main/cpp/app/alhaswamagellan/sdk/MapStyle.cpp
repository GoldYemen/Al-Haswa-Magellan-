#include "app/alhaswamagellan/sdk/core/jni_helper.hpp"

#include "app/alhaswamagellan/sdk/Framework.hpp"

#include "indexer/map_style.hpp"

extern "C"
{
JNIEXPORT void Java_app_alhaswamagellan_sdk_MapStyle_nativeSet(JNIEnv *, jclass, jint mapStyle)
{
  auto const val = static_cast<MapStyle>(mapStyle);
  if (val != g_framework->GetMapStyle())
    g_framework->SetMapStyle(val);
}

JNIEXPORT jint Java_app_alhaswamagellan_sdk_MapStyle_nativeGet(JNIEnv *, jclass)
{
  return g_framework->GetMapStyle();
}

JNIEXPORT void Java_app_alhaswamagellan_sdk_MapStyle_nativeMark(JNIEnv *, jclass, jint mapStyle)
{
  auto const val = static_cast<MapStyle>(mapStyle);
  if (val != g_framework->GetMapStyle())
    g_framework->MarkMapStyle(val);
}
}
