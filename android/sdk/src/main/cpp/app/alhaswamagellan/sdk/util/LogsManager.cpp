#include <jni.h>
#include "app/alhaswamagellan/sdk/core/logging.hpp"

extern "C"
{
JNIEXPORT void Java_app_alhaswamagellan_sdk_util_log_LogsManager_nativeToggleCoreDebugLogs(JNIEnv * /*env*/,
                                                                                       jclass /*clazz*/,
                                                                                       jboolean enabled)
{
  jni::ToggleDebugLogs(enabled);
}
}  // extern "C"
