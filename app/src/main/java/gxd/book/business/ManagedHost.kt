package gxd.book.business

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.support.v4.app.INotificationSideChannel
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import gxd.book.android.randColor
import gxd.book.android.renderSize
import gxd.book.utils.DrawUtils
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.dip
import org.jetbrains.anko.info
import org.jetbrains.anko.switch
import java.util.*

/**
 * Created by work on 2018/5/23.
 * canvas对区域的控制
 * https://blog.csdn.net/lonelyroamer/article/details/8349601
 * 托管的宿主
 */

class ManagedHost(ctx:Context):RelativeLayout(ctx){
    companion object {
        internal fun drawEqualParts(canvas: Canvas, partCount: Int, width: Float, height: Float) {
            //横纵 等分，打印起点坐标
            val dx = width / partCount
            val dy = height / partCount

            var indX = -1
            var x = 0F

            while (++indX < partCount) {
                var indY = -1
                var y = 0F

                while (++indY < partCount) {
                    //region    遍历
                    val rectF = RectF(x, y, x + dx, y + dy)
                    DrawUtils.drawRect(canvas, rectF)
                    DrawUtils.drawText(canvas, rectF, "$x,$y")
                    //endregion
                    y += dy
                }
                x += dx
            }

            /*for (x in 0..width step dx){
            for (y in 0..height step dy){
                val rect = Rect(x,y,x+dx,y+dy)
                DrawUtils.drawRect(canvas, rect)
                //\n无法换行
                DrawUtils.drawText(canvas, rect, "$x,$y")
            }
        }*/
        }
    }
    //region    世界是静态的全部的，我在世界的位置PworldBeg,PworldEnd
    internal var worldBegX: Float = 0F
    internal val worldEndX: Float get() = worldEndX + width
    internal var worldBegY: Float = 0F
    internal val worldEndY: Float get() = worldEndY + height
    //endregion
    private val partLeft:PartView
    private val partRight:PartView
    init {
        setWillNotDraw(false)
        partLeft = PartView(0F,0F)
        partRight = PartView(0F,0F)
    }
    public fun moveOffset(dx:Float, dy:Float) {
        if (modeHor) {
            if (selectedLeft) {
                partLeft.also {
                    it.worldBegX += dx
                    it.worldBegY += dy
                }
            }
            if (selectedRight) {
                partRight.also {
                    it.worldBegX += dx
                    it.worldBegY += dy
                }
            }
        }
        /*worldBegX += dx
        worldBegY += dy*/
        invalidate()
    }
    public var selectedLeft:Boolean = true
    public var selectedRight:Boolean = true

    private var modeHor:Boolean = true
    private var modeVer:Boolean = false
    /*override fun onDraw(canvas: Canvas) {
        //region    我是这样呈现世界的
        canvas.translate(-worldBegX, -worldBegY)
        //世界的大小是不变的，从(0,0)到(width,height)，下面使用的变量
        drawEqualParts(canvas,3, width.toFloat(), height.toFloat())
        canvas.translate(worldBegX, worldBegY)
        //endregion
    }*/
    override fun onDraw(canvas: Canvas) {
        if (modeHor) {
            //region    左部分
            canvas.save()
            canvas.clipRect(0, 0, width / 2, height)
            canvas.translate(partLeft.worldBegX, partLeft.worldBegY)
            drawEqualParts(canvas, 3, width.toFloat(), height.toFloat())
            canvas.restore()
            //endregion

            //region    右部分
            canvas.clipRect(width / 2, 0, width, height)
            canvas.translate(partRight.worldBegX, partRight.worldBegY)
            drawEqualParts(canvas, 3, width.toFloat(), height.toFloat())
            //endregion
        }
    }

    inner class PartView(var worldBegX:Float, var worldBegY:Float){
    }

    class CanvasView(ctx:Context):View(ctx) {
        //region    shell.在外层屏幕坐标 world.画布坐标
        internal var shellX: Float = 0F
        internal var shellY: Float = 0F
        internal var worldBegX: Float = 0F
        internal val worldEndX: Float get() = worldEndX + width
        internal var worldBegY: Float = 0F
        internal val worldEndY: Float get() = worldEndY + height
        //endregion
        fun setOnRender(f:((Canvas,CanvasView)->Unit)?) {
            onRender = f
        }

        override fun onDraw(canvas: Canvas) {
            onRender?.let {
                //region    画布不懂，拖动窗口shell.
                canvas.translate(-shellX, -shellY)
                it.invoke(canvas, this)
                //endregion
            }
        }

        //region    私有隐藏
        private var onRender:((Canvas,CanvasView)->Unit)? = null
        //endregion
    }
}

/**
 * 模型：一个表面容器，n（大于等于0）个可视区
 * 触控：只能在一个可视区内作用，即变小的表面容器。
 * 偏移量：分世界坐标（world）偏移量和窗口（clip）坐标偏移量
 * 窗口偏移量在容器外无效：在clipRegion时进行裁剪，使其在容器内
 * 可视区有纵坐标（小值先绘制，会被覆盖）
 * 可视区有回弹/发射功能
 * 用视区而不用控件的好处：性能优化，侵入式控制，多方协调，没有控件Size的改变引发的其他问题
 * 视区的背景是透明的，选中A的时候，可能选中的是B，因为B的这部分是透明的，且在A上面
 */
class ManagedHostAdv(ctx:Context):RelativeLayout(ctx),AnkoLogger {
    override val loggerTag: String
        get() = "_mha"
    val visRects = mutableListOf<VisRect>()
    //重点用在添加和删除中，添加和删除时应先中断其他事件（加载数据、动画等）
    private val visChangedLocker = Any()
    private var flagArrangeType = 0
    private var flagMovingType = 0
    private var flagConfigureChanged = false
    private val dragPinch:MHostDragPinch
    init {
        setWillNotDraw(false)
        dragPinch = MHostDragPinch(this).apply {
            enable()
            //setClickListener { Toast.makeText(ctx,"${it.x},${it.y}",Toast.LENGTH_LONG).show();true }
        }
    }

    //region    横/纵向添加，限定视区数量为[0,2]
    fun add(visRect:VisRect) {
        synchronized(visChangedLocker) {
            val maxIndex = visRects.maxBy { it.zIndex }?.zIndex ?: 0
            visRects.add(visRect.also { it.zIndex = maxIndex + 1 })
        }
        invalidate()
    }

    fun add(type: Int){
        if (type == 0){
            val visRect = VisRect(width, height)
            add(visRect)
        }else{
            NotImplementedError("未实现")
        }
    }

    fun remove(visRect: VisRect){
        synchronized(visChangedLocker){
            visRects.remove(visRect)
        }
        invalidate()
    }
    fun typeMoving(type: Int){
        flagMovingType = type
    }
    fun typeArrange(type: Int){
        flagArrangeType = type
        val rect = Rect(0,0,width,height)
        //屏幕坐标
        //getHitRect(rect)
        VisRect.nativeArrangeVisRectLayout(visRects, rect, type)
        invalidate()
    }
    fun reverse() {
        if (visRects.size == 0) {
            return
        }
        val maxZIndex = visRects.maxBy { it.zIndex }!!.zIndex
        visRects.forEach { visRect ->
            visRect.zIndex = maxZIndex - visRect.zIndex + 1
        }
        invalidate()
    }

    //endregion

    //region    override
    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        flagConfigureChanged = true
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (flagConfigureChanged) {
            flagConfigureChanged = false
            val rect = Rect(0, 0, width, height)
            VisRect.nativeArrangeVisRectLayout(visRects, rect, flagArrangeType)
        }
        //region    每视区单独调用绘制
        lateinit var visList:List<VisRect>
        synchronized(visChangedLocker){
            visList = visRects.sortedBy { it.zIndex }
        }
        val dt = dip(30)
        visList.forEachIndexed { _, visRect ->
            val rect = Rect(dt, dt, width - dt, height - dt)
            visRect.look(canvas){
                canvas.drawRect(rect, Paint().also { it.color = visRect.testColor })
            }
        }
        //endregion
    }
    //endregion

    //region    驱动视区
    fun onDown(e:MotionEvent):Boolean {
        val x = e.x.toInt()
        val y = e.y.toInt()
        var finding = false
        val list = visRects.sortedByDescending { it.zIndex }
        list.forEach {
            if (!finding && it.clipRect.contains(x, y)) {
                it.flagHiting = true
                it.flagClipMoving = (flagMovingType and VisRect.MOVE_CLIP > 0)
                it.flagWorldMoving = (flagMovingType and VisRect.MOVE_WORLD > 0)
                finding = true
                info { "down:${it.clipRect}" }
            } else {
                it.flagHiting = false
            }
        }
        if (finding){
            invalidate()
        }
        return finding
    }
    fun onUp(e:MotionEvent):Boolean{
        visRects.forEach {
            it.flagHiting = false
        }
        info { "up" }
        invalidate()
        return true
    }
    fun onScroll(e1:MotionEvent,e2:MotionEvent,dx: Float,dy: Float):Boolean {
        val x2 = e2.x.toInt()
        val y2 = e2.y.toInt()
        val x1 = e1.x.toInt()
        val y1 = e1.y.toInt()
        visRects.find { it.flagHiting }?.let {
            if (it.clipRect.contains(x2, y2)) {
                info { "moveBeg:${it.clipRect}" }
                it.moving(dx, dy)
                info { "moveEnd:${it.clipRect}" }
                invalidate()
                return true
            } else if (it.clipRect.contains(x1, y1)) {
                onUp(e2)
                return false
            }
        }
        return false
    }
    //endregion
}

class VisRect(
        var width:Int,
        var hegith:Int,
        var clipX:Int = 0,  //视区起点
        var clipY:Int = 0,
        var testColor:Int = randColor
) {
    companion object {
        /**
         * 水平排列，从左向右，等分
         */
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
        internal fun nativeArrangeVisRectLayout(list: List<VisRect>, rect: Rect, type: Int) {
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
                            it.hegith = rect.height()
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
                            it.clipX = 0
                            it.clipY = y
                            it.width = rect.width()
                            it.hegith = dHeight
                            //这个值添加的时候设置
                            //it.zIndex = index + 1 //从1开始
                        }
                        y += dHeight
                    }
                }
            }
        }

        /**
         * 可视区偏移，绘制内容不移动,世界坐标系也要做相应改变
         */
        private inline fun nativeOffsetVisible(dx:Float, dy:Float, visRect: VisRect){
            val begX = visRect.worldBegX
            val begY = visRect.worldBegY
            visRect.shockX -= dx
            visRect.shockY -= dy
            visRect.clipX += begX - visRect.worldBegX
            visRect.clipY += begY - visRect.worldBegY
        }

        //endregion
    }
    var worldDx:Int = 0
        private set
    var worldDy:Int = 0
        private set

    var shockX:Float = 0F   //世界坐标偏移积累
        set(value) {
            val beg = field.toInt()
            field = value
            worldDx = field.toInt() - beg
        }
    var shockY:Float = 0F
        set(value) {
            val beg = field.toInt()
            field = value
            worldDy = field.toInt() - beg
        }

    val worldBegX:Int get() = shockX.toInt()
    val worldBegY:Int get() = shockY.toInt()
    var zIndex = 0  //垂直排序，大的后画
    var flagHiting = false  //点击命中
    var flagWorldMoving = false //世界坐标系可移动
    var flagClipMoving = false //视区起点可移动
    var flagAutoMoving = false  //视区自动（动画）移动/回弹


    val worldEndX: Int get() = worldBegX + width
    val worldEndY: Int get() = worldBegY + hegith
    val clipRect: Rect get() = Rect(clipX, clipY, clipX + width, clipY + hegith)
    val worldRect: Rect get() = Rect(worldBegX, worldBegY, worldEndX, worldEndY)

    fun moving(dx: Float, dy: Float){
        val it = this
        when{
            it.flagWorldMoving -> {
                it.shockX += dx
                it.shockY += dy
            }
            it.flagClipMoving -> {
                //只移动左上点
                it.shockX += dx
                it.shockY += dy
                it.clipX -= it.worldDx
                it.clipY -= it.worldDy
            }
        }
    }
    fun look(canvas: Canvas, f: (Canvas) -> Unit) {
        canvas.save()
        canvas.clipRect(clipRect)   //指定区域
        if (flagHiting){
            canvas.drawColor(Color.LTGRAY)
        }
        //look的位置
        canvas.translate(-worldBegX.toFloat(), -worldBegY.toFloat())
        //draw world
        f.invoke(canvas)
        canvas.restore()
    }
}



















