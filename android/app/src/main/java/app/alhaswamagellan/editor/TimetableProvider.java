package app.alhaswamagellan.editor;

import androidx.annotation.Nullable;

interface TimetableProvider
{
  @Nullable
  String getTimetables();
  void setTimetables(@Nullable String timetables);
}
