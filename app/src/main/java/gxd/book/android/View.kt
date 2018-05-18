package gxd.book.android

import android.view.View
import android.view.ViewGroup
import android.widget.TextView

val View.lpWidth:Int
    inline get() = layoutParams.width

val View.lpHeight:Int
    inline get() = layoutParams.height

inline fun View.lpUdate(updateLp:ViewGroup.LayoutParams.() -> Unit) {
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

