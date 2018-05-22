package gxd.book.business

import android.content.Context
import android.graphics.Canvas
import android.widget.RelativeLayout
import gxd.book.android.lpMatchParent

/**
 * Created by Administrator on 2018/5/22.
 */
class BmpHost(ctx:Context):RelativeLayout(ctx){
    lateinit var bmpView:BmpView
    private val dragPinchManager: DragPinchManager
    init {
        //setWillNotDraw(false)
        bmpView = BmpView(ctx).also {
            addView(it, ctx.lpMatchParent())
        }
        dragPinchManager = DragPinchManager(bmpView, ctx).apply {
            enable()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //可用来调试

    }

}