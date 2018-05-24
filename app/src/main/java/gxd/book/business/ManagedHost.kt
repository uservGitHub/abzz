package gxd.book.business

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import gxd.book.android.renderSize
import gxd.book.utils.DrawUtils
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

class ManagedHostAdv(ctx:Context):RelativeLayout(ctx) {
    val visRects = mutableListOf<VisRect>()
    private val visChangedLocker = Any()
    private val dragPinch:MHostDragPinch
    init {
        setWillNotDraw(false)
        dragPinch = MHostDragPinch(this).apply {
            enable()
        }
    }

    /**
     * 更新视区布局（执行横/纵向，无数量检查）
     */
    private fun updateVisRectLayout(isHor:Boolean){
        val count = visRects.size
        if (isHor){
            val dWidth = width/count
            var x = 0
            visRects.forEach {
                it.also {
                    it.clipX = x
                    it.clipY = 0
                    it.width = dWidth
                    it.hegith = height
                }
                x += dWidth
            }
        }else{
            val dHeight = height/count
            var y = 0
            visRects.forEach {
                it.also {
                    it.clipX = 0
                    it.clipY = y
                    it.width = width
                    it.hegith = dHeight
                }
                y += dHeight
            }
        }
        invalidate()
    }
    //region    横/纵向添加，限定视区数量为[0,2]
    fun addHor():Boolean {
        synchronized(visChangedLocker) {
            if (visRects.size > 1) return false
            visRects.add(VisRect(width, height))
            updateVisRectLayout(true)
            return true
        }
    }
    fun removeHor(visRect: VisRect){
        synchronized(visChangedLocker) {
            if (visRects.size == 0) return
            visRects.remove(visRect)
            updateVisRectLayout(true)
        }
    }
    fun addVer():Boolean{
        synchronized(visChangedLocker) {
            if (visRects.size > 1) return false
            visRects.add(VisRect(width, height))
            updateVisRectLayout(false)
            return true
        }
    }
    fun removeVer(visRect: VisRect){
        synchronized(visChangedLocker) {
            if (visRects.size == 0) return
            visRects.remove(visRect)
            updateVisRectLayout(false)
        }
    }
    //endregion

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //region    每视区单独调用绘制
        val r = Random()
        synchronized(visChangedLocker) {
            val dt = 80
            visRects.forEach {
                val fillColor = Color.rgb(r.nextInt(256), r.nextInt(256), r.nextInt(256))
                val rect = Rect(dt, dt, width - 2 * dt, height - 2 * dt)
                it.look(canvas) {
                    canvas.drawRect(rect, Paint().apply { color = fillColor })
                }
            }
        }
        //endregion
    }

    //region    驱动视区
    fun moveOffset(dx:Float, dy:Float) {
        synchronized(visChangedLocker) {
            val idx = dx.toInt()
            val idy = dy.toInt()
            visRects.forEach {
                if (it.canDrive) {
                    it.worldBegX += idx
                    it.worldBegY += idy
                }
            }
        }
        postInvalidate()
    }
    fun onDown(e:MotionEvent):Boolean{
        val x = e.rawX.toInt()
        val y = e.rawY.toInt()
        synchronized(visChangedLocker){
            visRects.find { it.clipRect.contains(x,y) }?.also {
                it.down = true
                postInvalidate()
            }
        }
        return true
    }
    fun onUp(e:MotionEvent):Boolean{
        synchronized(visChangedLocker){
            visRects.forEach {
                it.down = false
            }
            postInvalidate()
        }
        return true
    }
    fun onScroll(e1:MotionEvent,e2:MotionEvent,dx: Float,dy: Float):Boolean{
        val x2 = e2.rawX.toInt()
        val y2 = e2.rawY.toInt()
        var find = false
        synchronized(visChangedLocker){
            visRects.find { it.down }?.also {
                if(it.clipRect.contains(x2,y2)){
                    find = true
                }
            }
        }
        if (find){
            moveOffset(dx, dy)
            return true
        }else{
            onUp(e2)
            return false
        }
    }
    //endregion
}

class VisRect(
        var width:Int,
        var hegith:Int,
        var clipX:Int = 0,
        var clipY:Int=0,
        var worldBegX:Int = 0,
        var worldBegY:Int = 0
) {
    var canDrive = true
    var down = false
    val worldEndX: Int get() = worldBegX + width
    val worldEndY: Int get() = worldBegY + hegith
    val clipRect: Rect get() = Rect(clipX, clipY, clipX + width, clipY + hegith)
    val worldRect: Rect get() = Rect(worldBegX, worldBegY, worldEndX, worldEndY)

    fun look(canvas: Canvas, f: (Canvas) -> Unit) {
        canvas.save()
        canvas.clipRect(clipRect)   //指定区域
        if (down){
            canvas.drawColor(Color.LTGRAY)
        }
        //look的位置
        canvas.translate(-worldBegX.toFloat(), -worldBegY.toFloat())
        //draw world
        f.invoke(canvas)
        canvas.restore()
    }
}



















