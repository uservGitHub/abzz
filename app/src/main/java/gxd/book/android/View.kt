package gxd.book.android

import android.view.MotionEvent
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

inline fun View.hiting(event: MotionEvent, offset:Int = 0):Boolean {
    val pt = IntArray(2, { 0 })
    getLocationOnScreen(pt)
    val left = pt[0] - offset
    val top = pt[1] - offset
    val right = left + measuredWidth + offset
    val bottom = top + measuredHeight + offset
    val x = event.rawX
    val y = event.rawY
    if (x > left && x < right && y > top && y < bottom) {
        return true
    }
    return false
}

/**
 * View的呈现大小，在Activity.onResume之后触发回调
 */
fun View.renderSize(callback:(Pair<Int,Int>)->Unit) =
        viewTreeObserver.addOnPreDrawListener {
            callback(Pair<Int, Int>(width, height))
            return@addOnPreDrawListener true
        }

