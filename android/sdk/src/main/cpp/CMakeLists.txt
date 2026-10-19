cmake_minimum_required(VERSION 3.22.1)

project(organicmaps C CXX)

set(SRC
  # JNI headers
  ../../../../../private.h
  app/alhaswamagellan/sdk/core/jni_helper.hpp
  app/alhaswamagellan/sdk/core/jni_java_methods.hpp
  app/alhaswamagellan/sdk/core/logging.hpp
  app/alhaswamagellan/sdk/core/ScopedEnv.hpp
  app/alhaswamagellan/sdk/core/ScopedLocalRef.hpp
  app/alhaswamagellan/sdk/Framework.hpp
  app/alhaswamagellan/sdk/FrameworkJni.hpp
  app/alhaswamagellan/sdk/opengl/android_gl_utils.hpp
  app/alhaswamagellan/sdk/opengl/androidoglcontext.hpp
  app/alhaswamagellan/sdk/opengl/androidoglcontextfactory.hpp
  app/alhaswamagellan/sdk/opengl/gl3stub.h
  app/alhaswamagellan/sdk/platform/GuiThread.hpp
  app/alhaswamagellan/sdk/platform/AndroidPlatform.hpp
  app/alhaswamagellan/sdk/util/Distance.hpp
  app/alhaswamagellan/sdk/vulkan/android_vulkan_context_factory.hpp

  # JNI sources
  app/alhaswamagellan/sdk/search/DisplayedCategories.cpp
  app/alhaswamagellan/sdk/search/SearchEngine.cpp
  app/alhaswamagellan/sdk/search/SearchRecents.cpp
  app/alhaswamagellan/sdk/routing/RoutingJni.cpp
  app/alhaswamagellan/sdk/routing/RoutingJni.hpp
  app/alhaswamagellan/sdk/ChoosePositionMode.cpp
  app/alhaswamagellan/sdk/MapStyle.cpp
  app/alhaswamagellan/sdk/OrganicMaps.cpp
  app/alhaswamagellan/sdk/Router.cpp
  app/alhaswamagellan/sdk/core/jni_helper.cpp
  app/alhaswamagellan/sdk/core/jni_java_methods.cpp
  app/alhaswamagellan/sdk/core/logging.cpp
  app/alhaswamagellan/sdk/bookmarks/data/Bookmark.cpp
  app/alhaswamagellan/sdk/bookmarks/data/Bookmark.hpp
  app/alhaswamagellan/sdk/bookmarks/data/BookmarkCategory.cpp
  app/alhaswamagellan/sdk/bookmarks/data/BookmarkCategory.hpp
  app/alhaswamagellan/sdk/bookmarks/data/BookmarkManager.cpp
  app/alhaswamagellan/sdk/bookmarks/data/ElevationInfo.cpp
  app/alhaswamagellan/sdk/bookmarks/data/ElevationInfo.hpp
  app/alhaswamagellan/sdk/bookmarks/data/Icon.cpp
  app/alhaswamagellan/sdk/bookmarks/data/Icon.hpp
  app/alhaswamagellan/sdk/bookmarks/data/MapObject.cpp
  app/alhaswamagellan/sdk/bookmarks/data/MapObject.hpp
  app/alhaswamagellan/sdk/bookmarks/data/Metadata.cpp
  app/alhaswamagellan/sdk/bookmarks/data/Metadata.hpp
  app/alhaswamagellan/sdk/bookmarks/data/PredefinedColors.cpp
  app/alhaswamagellan/sdk/bookmarks/data/PredefinedColors.hpp
  app/alhaswamagellan/sdk/bookmarks/data/Track.cpp
  app/alhaswamagellan/sdk/bookmarks/data/Track.hpp
  app/alhaswamagellan/sdk/bookmarks/data/TrackStatistics.cpp
  app/alhaswamagellan/sdk/bookmarks/data/TrackStatistics.hpp
  app/alhaswamagellan/sdk/DownloadResourcesLegacyActivity.cpp
  app/alhaswamagellan/sdk/editor/Editor.cpp
  app/alhaswamagellan/sdk/editor/OpeningHours.cpp
  app/alhaswamagellan/sdk/editor/OsmOAuth.cpp
  app/alhaswamagellan/sdk/Framework.cpp
  app/alhaswamagellan/sdk/isolines/IsolinesManager.cpp
  app/alhaswamagellan/sdk/LocationState.cpp
  app/alhaswamagellan/sdk/Map.cpp
  app/alhaswamagellan/sdk/MapManager.cpp
  app/alhaswamagellan/sdk/settings/UnitLocale.cpp
  app/alhaswamagellan/sdk/settings/MapLanguageCode.cpp
  app/alhaswamagellan/sdk/sound/tts.cpp
  app/alhaswamagellan/sdk/subway/SubwayManager.cpp
  app/alhaswamagellan/sdk/TrackRecorder.cpp
  app/alhaswamagellan/sdk/TrafficState.cpp
  app/alhaswamagellan/sdk/opengl/android_gl_utils.cpp
  app/alhaswamagellan/sdk/opengl/androidoglcontext.cpp
  app/alhaswamagellan/sdk/opengl/androidoglcontextfactory.cpp
  app/alhaswamagellan/sdk/opengl/gl3stub.cpp
  app/alhaswamagellan/sdk/platform/GuiThread.cpp
  app/alhaswamagellan/sdk/platform/PlatformGuiThread.cpp
  app/alhaswamagellan/sdk/platform/Language.cpp
  app/alhaswamagellan/sdk/platform/Localization.cpp
  app/alhaswamagellan/sdk/platform/AndroidPlatform.cpp
  app/alhaswamagellan/sdk/platform/PThreadImpl.cpp
  app/alhaswamagellan/sdk/platform/SecureStorage.cpp
  app/alhaswamagellan/sdk/platform/SocketImpl.cpp
  app/alhaswamagellan/sdk/util/Config.cpp
  app/alhaswamagellan/sdk/util/GeoUtils.cpp
  app/alhaswamagellan/sdk/util/HttpClient.cpp
  app/alhaswamagellan/sdk/util/LogsManager.cpp
  app/alhaswamagellan/sdk/util/NetworkPolicy.cpp
  app/alhaswamagellan/sdk/util/StringUtils.cpp
  app/alhaswamagellan/sdk/util/UiThread.cpp
  app/alhaswamagellan/sdk/vulkan/android_vulkan_context_factory.cpp
)

omim_add_library(${PROJECT_NAME} SHARED ${SRC})

target_include_directories(${PROJECT_NAME} PRIVATE .)

target_link_libraries(${PROJECT_NAME} PRIVATE
  # Al-Haswa Magellan libs
  map
  # ge0
  # tracking
  # routing
  # traffic
  # routing_common
  # drape_frontend
  # shaders
  # search
  # storage
  # descriptions
  # drape
  # kml
  # editor
  # transit
  # indexer
  # platform
  # mwm_diff
  # bsdiff
  # geometry
  # coding
  # base
  # opening_hours
  # pugixml
  # expat
  # freetype
  # minizip
  # succinct
  # stb_image
  # icu
  # vulkan_wrapper

  # Android libs
  log
  android
  EGL
  GLESv2
  atomic
)

#target_link_options(${PROJECT_NAME} PRIVATE "-fuse-ld=gold")
