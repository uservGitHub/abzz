package gxd.book.business

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import gxd.book.android.renderSize
import gxd.book.utils.DrawUtils

/**
 * Created by Administrator on 2018/5/22.
 */

class BmpView(ctx:Context):View(ctx),DragPinchRawDriver{
    private var onRender:((Canvas)->Unit)? = null
    init {
        renderSize {
            w = it.first
            h = it.second
        }
    }
    //region    在大图中的位置：起点（x,y），终点（xEnd，yEnd）,及窗口大小（w,h）
    var xStart:Int = 0
        internal set
    var yStart:Int = 0
        internal set
    var w:Int = 0
        private set
    var h:Int = 0
        private set
    val xEnd:Int get() = xStart+h
    val yEnd:Int get() = yStart+w
    //endregion
    var shockX:Float = 0F
    var shockY:Float = 0F

    fun setOnRender(f:((Canvas)->Unit)?) {
        onRender = f
    }

    override val host: View
        get() = this
    override var isDownSource = false
    override var canUse: Boolean = true
    override var isFollow: Boolean = false
    override fun moveTo(x: Int, y: Int) {
/*        this.xStart = x
        this.yStart = y
        invalidate()*/
    }

    override fun moveOffset(dx: Float, dy: Float) {
        shockX += dx
        shockY += dy

        this.xStart = shockX.toInt()
        this.yStart = shockY.toInt()
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        onRender?.invoke(canvas)

        //横纵4等分，打印起点坐标
        val dx = (width/4).toInt()
        val dy = (height/4).toInt()

        canvas.translate(-shockX, -shockY)
        for (x in 0..width step dx){
            for (y in 0..height step dy){
                val rect = Rect(x,y,x+dx,y+dy)
                DrawUtils.drawRect(canvas, rect)
                //\n无法换行
                DrawUtils.drawText(canvas, rect, "$x,$y")
            }
        }
    }
}