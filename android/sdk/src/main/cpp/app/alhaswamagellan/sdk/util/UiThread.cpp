#include "app/alhaswamagellan/sdk/core/jni_helper.hpp"
#include "app/alhaswamagellan/sdk/platform/GuiThread.hpp"

extern "C"
{
// static void nativeProcessTask(long taskPointer);
JNIEXPORT void Java_app_alhaswamagellan_sdk_util_concurrency_UiThread_nativeProcessTask(JNIEnv * env, jclass clazz,
                                                                                    jlong taskPointer)
{
  android::GuiThread::ProcessTask(taskPointer);
}
}
