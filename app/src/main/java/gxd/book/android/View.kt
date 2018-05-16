package gxd.book.android

import android.view.View
import android.view.ViewGroup

val View.actualWidth:Int
    inline get() = layoutParams.width

val View.actualHeight:Int
    inline get() = layoutParams.height

inline fun View.actualLp(updateLp:ViewGroup.LayoutParams.() -> Unit) {
    layoutParams.updateLp()
    requestLayout()
}

/**
 * View的呈现大小，在Activity.onResume之后触发回调
 */
fun View.renderSize(callback:(Pair<Int,Int>)->Unit) =
        viewTreeObserver.addOnPreDrawListener {
            callback(Pair<Int, Int>(width, height))
            return@addOnPreDrawListener true
        }
