@file:JvmName("RoutePointLabels")

package app.alhaswamagellan.routing

import androidx.annotation.StringRes
import app.alhaswamagellan.R
import app.alhaswamagellan.sdk.routing.RouteMarkType

@StringRes
fun pickTitle(pointType: RouteMarkType, isReplace: Boolean): Int = when (pointType) {
    RouteMarkType.Start -> if (isReplace) R.string.change_start_location else R.string.choose_start_location
    RouteMarkType.Intermediate -> if (isReplace) R.string.change_stop_along_route else R.string.placepage_add_stop
    RouteMarkType.Finish -> if (isReplace) R.string.change_destination else R.string.choose_destination
}
