#include "app/alhaswamagellan/sdk/Framework.hpp"
#include "app/alhaswamagellan/sdk/core/jni_helper.hpp"

#include "search/displayed_categories.hpp"

extern "C"
{
JNIEXPORT jobjectArray Java_app_alhaswamagellan_sdk_search_DisplayedCategories_nativeGetKeys(JNIEnv * env, jclass)
{
  search::DisplayedCategories const & categories = frm()->GetDisplayedCategories();
  return jni::ToJavaStringArray(env, categories.GetKeys());
}

JNIEXPORT jboolean Java_app_alhaswamagellan_sdk_search_DisplayedCategories_nativeIsLangSupported(JNIEnv * env, jclass,
                                                                                             jstring langCode)
{
  return search::DisplayedCategories::IsLanguageSupported(jni::ToNativeString(env, langCode));
}
}  // extern "C"
