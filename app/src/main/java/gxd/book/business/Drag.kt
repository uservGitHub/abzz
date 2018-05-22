package gxd.book.business

/**
 * Created by Administrator on 2018/5/22.
 */

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import org.jetbrains.anko.AnkoLogger


/**
 * Created by work on 2018/2/14.
 * 拖拽、点击等控制，输出控制驱动
 * 驱动的实施是通过绑定控件的接口来实现的
 */

//hostList 要驱动的接口列表，ctx 操控的整个表面
//缩放功能作用整个表面，其他（点击、滑动）作用
//滑动过程：moveOffset n 个, scrollEnd
//惯性过程：滑动过程, movingEnd

class DragPinchManager(driver: DragPinchRawDriver, ctx:Context):
        View.OnTouchListener, GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,ScaleGestureDetector.OnScaleGestureListener, AnkoLogger {

    override val loggerTag: String
        get() = "_DPM"


    private var scrolling = false
    private var scaling = false
    private var enabled = false
    private var doubleClickListener: ((event:MotionEvent)->Boolean)? = null

    private val gestureDetector: GestureDetector
    private val scaleGestureDetetor:ScaleGestureDetector

    val hostList:MutableList<DragPinchRawDriver>

    init {
        gestureDetector = GestureDetector(ctx, this)
        scaleGestureDetetor = ScaleGestureDetector(ctx, this)

        driver.host.setOnTouchListener(this)
        hostList = MutableList<DragPinchRawDriver>(1, {driver})
        /*hostList.forEach {
            it.host.setOnTouchListener(this)
        }*/
    }
    fun addDriver(driver: DragPinchRawDriver){
        driver.host.setOnTouchListener(this)
        hostList.add(driver)
    }
    fun removeDriver(driver: DragPinchRawDriver){
        driver.host.setOnTouchListener(null)
        hostList.remove(driver)
    }

    fun enable() {
        enabled = true
    }

    fun disable() {
        enabled = false
    }

    fun setDoublClickListener(listener:(event:MotionEvent)->Boolean){
        doubleClickListener = listener
    }

    //region    OnDoubleTapListener
    override fun onDoubleTap(e: MotionEvent): Boolean {
        //如果存在外层事件，优先处理外层事件
        doubleClickListener?.let {
            if(it.invoke(e)){
                return true
            }
        }

        hostList.forEach {
            if (it.hiting(e)){
                it.doubleClickAction(e)
                return true
            }
        }
        //不再扩散
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent?) = false

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        hostList.forEach {
            if (it.hiting(e)){
                it.clickAction(e)
                return true
            }
        }
        return false
    }
    //endregion

    //region    OnScaleGestureListener
    override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onScale(detector: ScaleGestureDetector?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onScaleEnd(detector: ScaleGestureDetector?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    //endregion

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (!enabled) {
            return false
        }
        var retVal = false
        retVal = scaleGestureDetetor.onTouchEvent(event)
        retVal = gestureDetector.onTouchEvent(event) || retVal
        if (event.action == MotionEvent.ACTION_UP) {
            if (scrolling) {
                scrolling = false
                hostList.forEach {
                    if (it.isDownSource) {
                        it.scrollEndAction(event)
                    } else {
                        if (it.isFollow) {
                            it.scrollEndAction(event)
                        }
                    }
                }
            }
        }
        return retVal
    }
/*    private fun onScrollEnd(event: MotionEvent) {
        //region    过界回滚
        if (true) {
            val pt = currentLeftTop()
            val frame = ptRange()
            val endX = if (pt.x < frame.left) frame.left
            else if (pt.x > frame.right) frame.right
            else pt.x
            val endY = if (pt.y < frame.top) frame.top
            else if (pt.y > frame.bottom) frame.bottom
            else pt.y
            if (pt.x != endX || pt.y != endY) {
                animation.startXYAnimation(pt.x, pt.y, endX, endY)
            }
        }
        //endregion
        //scrollEnd()
    }*/

    //中断加速,记录按下源
    override fun onDown(e: MotionEvent): Boolean {
        hostList.forEach {
            if (it.hiting(e)){
                it.isDownSource = true
                it.stopFling()
            }else{
                it.isDownSource = false
                if (it.canUse && it.isFollow){
                    it.stopFling()
                }
            }
        }
        return true
    }

    //输出偏移量
    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        scrolling = true
        hostList.forEach {
            if (it.isDownSource) {
                it.moveOffset(distanceX, distanceY)
            } else {
                if (it.isFollow) {
                    it.moveOffset(distanceX, distanceY)
                }
            }
        }
        return true
    }

    //输出加速度
    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
/*        val pt = currentLeftTop()
        val frame = ptRange()
        animation.startFlingAnimation(pt.x, pt.y, -velocityX.toInt(), -velocityY.toInt(),
                frame.left, frame.right, frame.top, frame.bottom)*/
        hostList.forEach {
            if (it.isDownSource) {
                it.moveVelocity(velocityX, velocityY)
            } else {
                if (it.isFollow) {
                    it.moveVelocity(velocityX, velocityY)
                }
            }
        }
        return true
    }

    override fun onShowPress(e: MotionEvent?) = Unit
    override fun onLongPress(e: MotionEvent?) = Unit
    override fun onSingleTapUp(e: MotionEvent?) = false


}