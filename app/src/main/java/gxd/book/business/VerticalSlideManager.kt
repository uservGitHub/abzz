package gxd.book.business

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.OverScroller
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.util.*

private val VIEW_TAG = "_View"
private val randColor:Int get() {
    val r = Random()
    return Color.rgb(r.nextInt(256), r.nextInt(256), r.nextInt(256))
}

class HostView(ctx:Context):View(ctx){
    internal fun moveTo(x:Float, y:Float) = Unit
    internal fun moveToX(x:Float) = Unit
    internal fun moveToY(y:Float) = Unit

    //dragpinch
    internal fun hiting(e:MotionEvent):Boolean = true
    internal val isSwipeVertical = false
    internal fun moveOffset(dx:Float, dy:Float) = Unit
    internal val visX:Float = 0F
    internal val visY:Float = 0F
    internal val visBoundX:Float = 0F
    internal val visBoundY:Float = 0F
}

//修改加速滑动：只能向一个方向
class AnimationManger(private val host:HostView): AnkoLogger {
    override val loggerTag: String
        get() = VIEW_TAG
    private val scroller: OverScroller
    private var animation: ValueAnimator? = null
    private var fling = false
    private var beginFlingTick:Long = 0
    private var beginAnimationTick:Long = 0
    private var skipToEnd:Boolean = false
    private var endValue:Float = 0F

    init {
        scroller = OverScroller(host.context)
    }

    //region startXAnimation startYAnimation startFlingAnimation
    fun startXAnimation(xFrom: Float, xTo: Float, toEnd:Boolean = false) {
        info { "startXAnimation(${xFrom.toInt()},${xTo.toInt()})" }
        stopAll()
        skipToEnd = toEnd
        endValue = xTo
        beginAnimationTick = System.currentTimeMillis()
        animation = ValueAnimator.ofFloat(xFrom, xTo).apply {
            interpolator = DecelerateInterpolator()
            duration = 400
            val xAnimation = XAnimation()
            addUpdateListener(xAnimation)
            addListener(xAnimation)
            start()
        }
    }

    fun startYAnimation(yFrom: Float, yTo: Float, toEnd:Boolean = false) {
        info { "startYAnimation(${yFrom.toInt()},${yTo.toInt()})" }
        stopAll()
        skipToEnd = toEnd
        endValue = yTo
        beginAnimationTick = System.currentTimeMillis()
        animation = ValueAnimator.ofFloat(yFrom, yTo).apply {
            interpolator = DecelerateInterpolator()
            duration = 400
            val yAnimation = YAnimation()
            addUpdateListener(yAnimation)
            addListener(yAnimation)
            start()
        }
    }
    fun startFlingAnimation(startX: Int, startY: Int, velX: Int, velY: Int,
                            minX: Int, maxX: Int, minY: Int, maxY: Int) {
        info { "startFlingAnimation($startX,$velX,$minX - $maxX),($startY,$velY,$minY - $maxY)" }
        stopAll()
        beginFlingTick = System.currentTimeMillis()
        fling = true
        scroller.fling(startX, startY, velX, velY, minX, maxX, minY, maxY)
    }
    //endregion

    //region    stopAll stopFling
    fun stopAll() {
        animation?.cancel()
        animation = null
        stopFiling()
    }

    /**
     * 停止加速（是否要到结束点）
     */
    fun stopFiling(toFinal: Boolean = false) {
        if (fling) {
            info { "stopFling" }
            if (toFinal) {
                scroller.abortAnimation()
                host.postInvalidate()
            } else {
                scroller.forceFinished(true)
                host.invalidate()
            }
        }
    }
    //endregion

    fun computeFling() {
        if (scroller.computeScrollOffset()) {
            host.moveTo(scroller.currX.toFloat(), scroller.currY.toFloat())
        } else if (fling) {
            fling = false
            host.moveTo(scroller.currX.toFloat(), scroller.currY.toFloat())
            val sp = "(${System.currentTimeMillis()-beginFlingTick})ms"
            info { "EndFling$sp(${scroller.currX} - ${scroller.finalX}),(${scroller.currY} - ${scroller.finalY})" }
        }
    }

    //region    inner class
    inner class XAnimation() : AnimatorListenerAdapter(), ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val offset = animation.animatedValue as Float
            host.moveToX(offset)
        }

        override fun onAnimationCancel(animation: Animator?) {
            if (skipToEnd){
                host.moveToX(endValue)
            }
            info { "CancelX(${System.currentTimeMillis()-beginAnimationTick})ms" }
        }

        override fun onAnimationEnd(animation: Animator?) {
            info { "EndX(${System.currentTimeMillis()-beginAnimationTick})ms($endValue)" }
        }
    }

    inner class YAnimation() : AnimatorListenerAdapter(), ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val offset = animation.animatedValue as Float
            host.moveToY(offset)
        }

        override fun onAnimationCancel(animation: Animator?) {
            if (skipToEnd){
                host.moveToY(endValue)
            }
            info { "CancelY(${System.currentTimeMillis()-beginAnimationTick})ms($endValue)" }
        }

        override fun onAnimationEnd(animation: Animator?) {
            info { "EndY(${System.currentTimeMillis()-beginAnimationTick})ms(${endValue})" }
        }
    }

    //endregion
}

//region    ViewDragPinchManager

//增加控制：1 增量只能朝一个方向进行，直到UP；
class DragPinchManager(private val host: HostView, private val animationManger: AnimationManger):
        GestureDetector.OnGestureListener, View.OnTouchListener, GestureDetector.OnDoubleTapListener,AnkoLogger{
    override val loggerTag: String
        get() = VIEW_TAG
    private val gestureDetector: GestureDetector
    private var scrolling = false
    private var enabled = false
    private var dbClickTick: Long = 0

    init {
        gestureDetector = GestureDetector(host.context, this)
        host.setOnTouchListener(this)
    }
    fun enable(){
        enabled = true
    }
    fun disable(){
        enabled = false
    }
    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        //内部控件的单击处理，比如ScrollHandled的处理
        //...
        info { "hostPreClick" }
        host.performClick()
        return true
    }

    //DoubleClick后面为什么要触发一个Down操作呢？
    override fun onDoubleTap(e: MotionEvent): Boolean {
        //可以进行放大等双击操作
        dbClickTick = System.currentTimeMillis()
        info { "hostDbClick" }
        return true
    }

    override fun onLongPress(e: MotionEvent?) = Unit
    override fun onSingleTapUp(e: MotionEvent?) = false
    override fun onDoubleTapEvent(e: MotionEvent?) = false
    override fun onShowPress(e: MotionEvent?) = Unit
    override fun onDown(e: MotionEvent): Boolean {
        val tick = System.currentTimeMillis()
        //防止DbClick后面紧跟着执行stopAll操作
        if (tick - dbClickTick > 50) {
            info { "Down-StopAll" }
            animationManger.stopAll()
        }
        host.hiting(e)
        return true
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        if (!scrolling){
            info { "BeginScroll" }
            scrolling = true
        }
        if (host.isSwipeVertical){
            if (Math.abs(distanceX)>Math.abs(distanceY)*1.5F){
                host.moveOffset(-distanceX, 0F)
            }else{
                host.moveOffset(0F, -distanceY)
            }
        }
        return true
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        if (host.isSwipeVertical) {
            val maxVelY = host.height * 1
            val velY = if (velocityY > maxVelY) maxVelY else velocityY.toInt()
            val y = host.visY.toInt()
            val minY = y - 8000
            val maxY = y + 8000
            animationManger.startFlingAnimation(host.visX.toInt(), y,
                    0, velY,
                    0, 0,
                    minY, maxY)
        } else {
            val maxVelX = host.width * 1
            val velX = if (velocityX > maxVelX) maxVelX else velocityX.toInt()
            val x = host.visX.toInt()
            //模拟无限
            val minX = x - 8000
            val maxX = x + 8000
            animationManger.startFlingAnimation(x, host.visY.toInt(),
                    velX, 0,
                    minX, maxX,
                    0, 0)
        }
        return true
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (!enabled){
            return false
        }
        val retVal = gestureDetector.onTouchEvent(event)

        if (event.action == MotionEvent.ACTION_UP){
            if (scrolling){
                scrolling = false
                onScrollEnd(event)
            }
        }
        return retVal
    }
    private fun onScrollEnd(event: MotionEvent){
        info { "EndScroll" }
        //回弹不能移动的边
        if (host.isSwipeVertical){
            if (host.visBoundX != host.visX){
                animationManger.startXAnimation(host.visX, host.visBoundX, true)
            }
        }else{
            if (host.visBoundY != host.visY){
                animationManger.startYAnimation(host.visY, host.visBoundY, true)
            }
        }
    }
}

//endregion

class PageManager private constructor(
        val isVertical: Boolean,
        sideLength:Int,
        pdfFile: PdfFile,
        startPageInd:Int):AnkoLogger {
    override val loggerTag: String
        get() = "_View"
    constructor(length: Int, pdf: PdfFile) : this(true, length, pdf, 21)

    var sideLength: Int
        private set
    var pdfFile: PdfFile
        private set
    var startBoundarySlide: Float = 0F
        private set
    var endBoundarySlide: Float = 0F
        private set
    private var startBoundaryPageInd: Int = -1
        private set
    private var endBoundaryPageInd: Int = -1
        private set

    private val maxPageCount = 12
    private val initPageCount = 10
    private val pageListUpdateLock = Any()
    private var loadFinish:(()->Unit)? = null
    @Volatile
    private var loading = false

    private val pageList: MutableList<Page> = mutableListOf()

    init {
        this.sideLength = sideLength
        this.pdfFile = pdfFile

        backwardAdd(startPageInd, initPageCount)
    }

    fun renderPages(canvas: Canvas, visX:Float, visY:Float, visLength:Int){
        val visValue = if (isVertical) visY else visX
        val visEnd = visValue + visLength
        val begInd = pageList.indexOfFirst { it.begAnixValue<=visEnd && it.endAnxiValue>= visValue}
        val endInd = pageList.indexOfLast { it.begAnixValue<=visEnd && it.endAnxiValue>= visValue }

        info { "begInd,endInd = $begInd,$endInd ${visValue.toInt()},${visEnd.toInt()}" }
        if (begInd == -1 || endInd == -1) return

        info { "${pageList[begInd].begAnixValue},${pageList[endInd].endAnxiValue}" }
        //val antialiasFilter = PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val paint = Paint()
        //canvas.drawFilter = antialiasFilter

        try {
            canvas.translate(-visX, -visY)
            for (ind in begInd..endInd) {
                val page = pageList[ind]
                canvas.drawBitmap(page.bmp, visX, page.begAnixValue, paint)
            }
            canvas.translate(visX, visY)
        }catch (e:Exception){
            Log.v("_View", "${e.message}")
        }
    }

    fun setOnLoadFinished(finished:(()->Unit)?){
        loadFinish = finished
    }

    fun backwardLoad() {
        if (loading || !canNextPage) return
        loading = true

        backwardAdd(pageList.last().pageInd + 1)
        if (pageList.size > maxPageCount) {
            pageList.first().remove()
        }
        loadFinish?.invoke()
        loading = false
    }

    fun forwardLoad() {
        if (loading || !canPrePage) return
        loading = true

        forwardAdd(pageList.first().pageInd - 1)
        if (pageList.size > maxPageCount) {
            pageList.last().remove()
        }
        loadFinish?.invoke()
        loading = false
    }

    //region    两个页边界（执行构造函数时，已经加载了一页）
    private val canPrePage: Boolean
        get() = when {
            pageList.first().pageInd <= 0 -> false
            else -> pdfFile.openPage(pageList.first().pageInd - 1)
        }
    private val canNextPage: Boolean
        get() = when {
            pageList.last().pageInd + 1 >= pdfFile.pageCount -> false
            else -> pdfFile.openPage(pageList.last().pageInd + 1)
        }
    //endregion

    private fun backwardAdd(pageInd: Int, count: Int = 1) {
        val lastInd = Math.min(pageInd + count - 1, pdfFile.pageCount - 1)
        for (ind in pageInd..lastInd) {
            if (!pdfFile.openPage(ind)) return
            Page(ind)
        }
    }

    private fun forwardAdd(pageInd: Int, count: Int = 1) {
        val firstInd = Math.max(pageInd - count + 1, 0)
        for (ind in firstInd..pageInd) {
            if (!pdfFile.openPage(ind)) return
            Page(ind)
        }
    }

    //创建时，自动添加到列表中；remove自动从列表中删除
    inner private class Page(val pageInd: Int) {
        var begAnixValue: Float = 0F
            private set
        val endAnxiValue: Float
            get() =
                begAnixValue + if (isVertical) bmp.height else bmp.width
        val bmp: Bitmap

        init {
            if (pageInd in startBoundaryPageInd..endBoundaryPageInd) {
                throw IllegalArgumentException("pageInd($pageInd)已经属于[$startBoundaryPageInd,$endBoundaryPageInd]")
            }

            val size = pdfFile.pageSize(pageInd)
            //变换后的长度(Int)
            val calcLength =
                    if (isVertical) size.height * sideLength / size.width
                    else size.width * sideLength / size.height
            bmp = Bitmap.createBitmap(
                    if (isVertical) sideLength else calcLength,
                    if (isVertical) calcLength else sideLength,
                    Bitmap.Config.RGB_565).apply {
                //eraseColor(debugRandColor)
                pdfFile.writeBmp(pageInd, this)
            }

            synchronized(pageListUpdateLock) {
                if (startBoundaryPageInd == -1 || endBoundaryPageInd == -1) {
                    pageList.add(this)
                    //重置一下比较好
                    startBoundaryPageInd = pageInd
                    endBoundaryPageInd = pageInd
                    startBoundarySlide = 0F
                    endBoundarySlide = 0F
                    begAnixValue = startBoundarySlide
                } else if (pageInd > endBoundaryPageInd) {
                    pageList.add(this)
                    begAnixValue = endBoundarySlide
                    endBoundaryPageInd = pageInd
                    endBoundarySlide += calcLength
                } else if (pageInd < startBoundaryPageInd) {
                    pageList.add(0, this)
                    startBoundaryPageInd = pageInd
                    startBoundarySlide -= calcLength
                    begAnixValue = startBoundarySlide
                }
            }
        }

        fun remove(): Boolean {
            val calcLength =
                    if (isVertical) bmp.height
                    else bmp.width
            if (!bmp.isRecycled) bmp.recycle()

            synchronized(pageListUpdateLock) {
                if (pageList.size == 1) {
                    startBoundarySlide = 0F
                    endBoundarySlide = 0F
                    startBoundaryPageInd = -1
                    pageList.removeAt(0)
                    return true
                } else if (pageInd == startBoundaryPageInd) {
                    startBoundarySlide += calcLength
                    startBoundaryPageInd = pageList[1].pageInd
                    pageList.removeAt(0)
                    return true
                } else if (pageInd == endBoundaryPageInd) {
                    endBoundarySlide -= calcLength
                    endBoundaryPageInd = pageList[pageList.size - 2].pageInd
                    pageList.removeAt(pageList.lastIndex)
                    return true
                }
            }
            return false
        }
    }
}