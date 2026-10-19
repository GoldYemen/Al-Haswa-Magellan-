@file:JvmName("ExportMenuItems")

package app.alhaswamagellan.util.bottomsheet

import app.alhaswamagellan.R
import app.alhaswamagellan.sdk.bookmarks.data.FileType
import java.util.function.Consumer

fun create(onSelected: Consumer<FileType>): ArrayList<MenuBottomSheetItem> = arrayListOf(
    MenuBottomSheetItem(R.string.export_file, R.drawable.ic_file_kmz) { onSelected.accept(FileType.Kml) },
    MenuBottomSheetItem(R.string.export_file_gpx, R.drawable.ic_file_gpx) { onSelected.accept(FileType.Gpx) },
    MenuBottomSheetItem(R.string.export_file_geojson, R.drawable.ic_file_geojson) {
        onSelected.accept(FileType.GeoJson)
    },
)
