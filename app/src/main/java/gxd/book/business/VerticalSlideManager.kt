package gxd.book.business

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.OverScroller
import gxd.book.android.sdPath
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.util.*

private val VIEW_TAG = "_View"
private val randColor:Int get() {
    val r = Random()
    return Color.rgb(r.nextInt(256), r.nextInt(256), r.nextInt(256))
}

class HostView(ctx:Context):View(ctx),AnkoLogger{
    override val loggerTag: String
        get() = VIEW_TAG
    private val animationManger:AnimationManger
    private val dragPinchManager:DragPinchManager
    internal fun moveTo(x:Float, y:Float) {
        visRects.forEach {
            if (it.flagHiting || !it.locked){
                it.worldX = x
                it.worldY = y
            }
        }
        invalidate()
    }
    internal fun moveToX(x:Float) = moveTo(x, visY)
    internal fun moveToY(y:Float) = moveTo(visX, y)

    //dragpinch
    internal fun hiting(e:MotionEvent){
        visRects.forEach {
            it.flagHiting = it.clipRect.contains(e.x, e.y)
        }
    }
    internal val isSwipeVertical = true
    internal fun moveOffset(dx:Float, dy:Float) = moveTo(visX+dx, visY+dy)
    internal val visX:Float get() = selectedVisRect.worldX
    internal val visY:Float get() = selectedVisRect.worldY
    internal val visBoundX:Float = 0F
    internal val visBoundY:Float = 0F
    private inline val selectedVisRect:VisRect get() = visRects.first()

    private var hasSize = false
    private var visType:Int = VisRect.FILL_ALL
    internal val visRects = mutableListOf<VisRect>()
    init {
        setWillNotDraw(false)
        animationManger = AnimationManger(this)
        dragPinchManager = DragPinchManager(this, animationManger)
        //visRects.add(VisRect())
    }
    private fun createPageManager():PageManager{
        val filename = "${context.sdPath}/gxd.book/atest/testpdf.pdf"
        val pdfFile = PdfFile(filename)
        pdfFile.openFile()
        pdfFile.openDoc()
        val pageManager = PageManager(width,pdfFile).apply {
            setOnLoadFinished {
                dragPinchManager.enable()
                invalidate()
            }
        }
        return pageManager
    }


    //region internal
    /**
     * 添加、调整布局、刷新
     */
    internal fun autoAdd() {
        if (visRects.size != 1) {
            return
        }
        visRects.add(VisRect(createPageManager()))
        updateVisRectLayout()
        invalidate()
    }

    /**
     * 删除、调整布局、刷新
     */
    internal fun remove(visRect: VisRect){
        if (visRects.size != 2){
            return
        }
        visRects.remove(visRect)
        updateVisRectLayout()
        invalidate()
    }

    internal fun reverse(){
        if (visRects.size != 2){
            return
        }
        updateVisRectLayout(true)
        invalidate()
    }

    /**
     * 调整布局
     */
    private fun updateVisRectLayout(reverse:Boolean = false, isAuto:Boolean = true){
        when{
            reverse -> {
                visType = if (visType == VisRect.HOR_ALL)VisRect.VER_ALL else VisRect.HOR_ALL
            }
            isAuto -> {
                if (height == width){
                    info { "visType=0" }
                    visType = VisRect.FILL_ALL
                }else {
                    visType = if (height > width) VisRect.VER_ALL else VisRect.HOR_ALL
                }
            }
        }
        val rectF = RectF(0F, 0F, width.toFloat(), height.toFloat())
        info { "updateVisLayout(count=${visRects.size},visType=$visType)" }
        VisRect.nativeArrangeVisRectLayout(visRects, rectF, visType)
    }

    /**
     * 回弹
     */
    internal fun springBack() {
        /*if (isSwipeVertical) {
            if (visBoundY != visY) {
                animationManager.startYAnimation(visY, visBoundY, true)
            }
        } else {
            if (visBoundX != visX) {
                animationManager.startXAnimation(visX, visBoundX, true)
            }
        }*/
    }
    //endregion

    //region    override


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        hasSize = true
        super.onSizeChanged(w, h, oldw, oldh)
        if (isInEditMode) {
            return
        }
        animationManger.stopAll()
        if (visRects.size == 0){
            visRects.add(VisRect(createPageManager()).apply {
                flagHiting = true
            })
        }
        updateVisRectLayout(false, false)
    }

    override fun onDraw(canvas: Canvas) {
        if (isInEditMode) {
            return
        }

        //region    draw background
        var isDrawBackground = false
        background?.let {
            isDrawBackground = true
            it.draw(canvas)
        }
        if (!isDrawBackground) {
            canvas.drawColor(Color.YELLOW)
        }
        //endregion

        val paint = Paint()
        canvas.drawFilter = PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        visRects.forEachIndexed { index, visRect ->
            //val rect = Rect(dt, dt, width - dt, height - dt)
            info { visRect }
            visRect.look(canvas){
                visRect.pageManager.renderPages(it, paint,
                        visRect.worldX,visRect.worldY, visRect.height.toInt())
                //canvas.drawRect(rect, Paint().also { it.color = visRect.testColor })
                /*val paint = textPaintFrom(visRect.testColor, sp(fontSize + 4*(index+1)).toFloat())
                canvas.drawText(
                        "$index:(${visX.toInt()},${visY.toInt()}),visRect$index:($visRect)",
                        visRect.worldX+visRect.clipX,visRect.worldY+visRect.clipY,paint)*/
            }
        }
    }

    override fun computeScroll() {
        super.computeScroll()
        if (isInEditMode) {
            return
        }
        animationManger.computeFling()
    }
    //endregion
}

//region    VisRect
class VisRect(val pageManager: PageManager,val testColor: Int = randColor){
    var width: Float = 0F
    var height: Float = 0F
    var clipX: Float = 0F
    var clipY: Float = 0F

    val boundaryMin:Float get() = pageManager.startBoundarySlide
    val boundaryMax :Float get() = pageManager.endBoundarySlide

    companion object {
        /**
         * 水平排列，从左向右，等分
         */
        val FILL_ALL = 0    //全屏布置
        val HOR_ALL = 1 //水平排列，从左向右，等分
        val VER_ALL = 2 //垂直排列，从上向下，等分

        val MOVE_CLIP = 1   //移动窗口
        val MOVE_WORLD = 2  //移动世界坐标系

        val MOVE_CLIP_L = 1   //移动左

        val MOVE_CLIP_T = 2   //移动左

        //region    native模型函数
        /**
         * 重新排列所有可视区
         */
        internal fun nativeArrangeVisRectLayout(list: List<VisRect>, rect: RectF, type: Int) {

            val count = list.size
            if (count == 0) {
                return
            }

            when (type) {
                VisRect.HOR_ALL -> {
                    val dWidth = rect.width() / count
                    var x = rect.left
                    list.forEachIndexed { index, visRect ->
                        visRect.also {
                            it.clipX = x
                            it.clipY = rect.top
                            it.width = dWidth
                            it.height = rect.height()
                            //这个值添加的时候设置
                            //it.zIndex = index + 1 //从1开始
                        }
                        x += dWidth
                    }
                }
                VisRect.VER_ALL -> {
                    val dHeight = rect.height() / count
                    var y = rect.top
                    list.forEachIndexed { index, visRect ->
                        visRect.also {
                            it.clipX = 0F
                            it.clipY = y
                            it.width = rect.width()
                            it.height = dHeight
                            //这个值添加的时候设置
                            //it.zIndex = index + 1 //从1开始
                        }
                        y += dHeight
                    }
                }
                VisRect.FILL_ALL ->{
                    list.forEachIndexed { index, visRect ->
                        visRect.also {
                            it.clipX = 0F
                            it.clipY = 0F
                            it.width = rect.width()
                            it.height = rect.height()
                            //这个值添加的时候设置
                            //it.zIndex = index + 1 //从1开始
                        }
                    }
                }
            }
        }
        //endregion
    }

    var worldX: Float = 0F
    var worldY: Float = 0F
    var zIndex = 0
    var locked = true   //不随动
    var flagHiting = false  //点击命中，选中，只能有一个选中
    //var flagWorldMoving = false //世界坐标系可移动
    //var flagClipMoving = false //视区起点可移动
    val clipRect: RectF get() = RectF(clipX, clipY, clipX + width, clipY + height)

    fun look(canvas: Canvas, f: (Canvas) -> Unit) {
        canvas.save()
        canvas.clipRect(clipRect)   //指定区域
        if (flagHiting) {
            //canvas.drawColor(Color.LTGRAY)
        }
        //look的位置
        //canvas.translate(-worldX, -worldY)
        //draw world
        f.invoke(canvas)
        canvas.restore()
    }

    fun moveWorldOffset(dx: Float, dy: Float) {
        worldX += dx
        worldY += dy
    }

    fun moveClipOffset(dx: Float, dy: Float) {
        //要求内容world也要随之移动
        clipX += dx
        clipY += dy
        moveWorldOffset(-dx, -dy)
    }

    override fun toString(): String {
        return "clip:$clipRect,world:${PointF(worldX,worldY)}"
    }
}


//endregion

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

/**
 * 页管理，核心概念：
 * 1 绘制时，自动进行页的异步加载和删除
 * 2 提供世界坐标系的可变界值
 * 3 提供页码的范围
 * 注意：绘制时，只需传递可视起点在世界坐标系中的位置和窗口长度
 */
class PageManager private constructor(
        val isVertical: Boolean,
        sideLength:Int,
        pdfFile: PdfFile,
        startPageInd:Int):AnkoLogger {
    override val loggerTag: String
        get() = "_View"
    constructor(length: Int, pdf: PdfFile, startPageInd: Int = 0) : this(true, length, pdf, startPageInd)

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
    //var requestPage:io.reactivex.Observable<out Int>? = null

    private val maxPageCount = 12
    private val initPageCount = 2
    private val pageCountPerTime = 2
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

    fun renderPages(canvas: Canvas, paint:Paint, visX:Float, visY:Float, visLength:Int) {
        if (pageList.size == 0) {
            return
        }

        val visStart = if (isVertical) visY else visX
        val visEnd = visStart + visLength

        //无交集返回
        if (startBoundarySlide >= visEnd || endBoundarySlide <= visStart) {
            return
        }

        canvas.translate(-visStart, -visEnd)
        synchronized(pageListUpdateLock) {
            pageList.forEach { page ->
                if (visStart >= page.begAnixValue && visStart < page.endAnixValue) {
                    canvas.drawBitmap(page.bmp, visX, page.begAnixValue, paint)
                } else if (visEnd <= page.begAnixValue) {
                    return@forEach
                }
            }
        }
        canvas.translate(visStart, visEnd)

        val offset = 2 * visLength
        if (Math.abs(endBoundarySlide - visEnd) < offset && canNextPage) {
            backwardLoad()
            return
        }
        if (Math.abs(startBoundarySlide - visStart) < offset && canPrePage) {
            forwardLoad()
            return
        }
    }

    fun setOnLoadFinished(finished:(()->Unit)?){
        loadFinish = finished
    }

    fun backwardLoad() {
        if (loading || !canNextPage) return
        loading = true

        Observable.just(pageList.last().pageInd+1)
                .subscribeOn(Schedulers.io())
                .map { backwardAdd(it, pageCountPerTime) }
                .observeOn(Schedulers.io())
                .subscribe {
                    while (pageList.size > maxPageCount){
                        pageList.first().remove()
                    }
                    loadFinish?.invoke()
                    loading = false
                }
    }

    fun forwardLoad() {
        if (loading || !canPrePage) return
        loading = true

        Observable.just(pageList.first().pageInd - 1)
                .subscribeOn(Schedulers.io())
                .map { forwardAdd(it, pageCountPerTime) }
                .observeOn(Schedulers.io())
                .subscribe {
                    while (pageList.size > maxPageCount){
                        pageList.last().remove()
                    }
                    loadFinish?.invoke()
                    loading = false
                }
    }

    //region    native函数，页码能否向前或向后；添加新页，前面加或后面加

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

    //endregion


    //创建时，自动添加到列表中；remove自动从列表中删除
    /**
     * 页，核心概念：
     * 1 创建对象（由pageInd）时，自动填充，并进维护
     * 2 删除对象（由对象实施），自动维护
     * 3 只读的起终点（位图在世界坐标系中的可变一维）及位图
     * 维护（重点）：页列表，页的范围[startBoundaryPageInd,endBoundaryPageInd]，
     * 可变一维坐标系的范围[startBoundarySlide,endBoundarySlide]
     */
    inner private class Page(val pageInd: Int) {
        var begAnixValue: Float = 0F
            private set
        val endAnixValue: Float
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