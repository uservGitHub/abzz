package gxd.book.business

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region
import android.view.View
import android.widget.RelativeLayout
import gxd.book.android.renderSize
import gxd.book.utils.DrawUtils

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