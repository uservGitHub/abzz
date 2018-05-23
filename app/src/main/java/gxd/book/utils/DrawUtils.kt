package gxd.book.utils

import android.graphics.*
import android.view.View

/**
 * Created by Administrator on 2018/5/22.
 */

class DrawUtils{
    companion object {
        //region    可动态改变
        /*val FONT_RINGVALUE = 20.rangeTo(30).step(2).toValue("FontSize")!!
        val FACE_RINGVALUE = listOf(Typeface.DEFAULT,
                Typeface.DEFAULT_BOLD,
                Typeface.MONOSPACE,
                Typeface.SANS_SERIF,
                Typeface.MONOSPACE).toValue("FontType")!!
        val FONT_SIZE: Float
            get() = FONT_RINGVALUE.value.toFloat()
        val FONT_FACE: Typeface
            get() = FACE_RINGVALUE.value

        val FONT_COLOR: Int
            get() = Color.BLACK*/
        //endregion

        //目前不使用
        fun View.drawDefaultText(canvas: Canvas, text: String, isBold: Boolean = true) {
            if (text.isNotEmpty()) {
                textDrawCenter(canvas,
                        if (isBold) TEXT_PAINT_BOLD else TEXT_PAINT_BOLD,
                        Rect(0, 0, width, height),
                        text)
            }
        }

        fun drawText(canvas: Canvas, rect: Rect, text: String, isBold: Boolean = true){
            if (text.isNotEmpty()) {
                textDrawCenter(canvas,
                        if (isBold) TEXT_PAINT_BOLD else TEXT_PAINT_BOLD,
                        rect,
                        text)
            }
        }
        fun drawText(canvas: Canvas, rect: RectF, text: String, isBold: Boolean = true){
            if (text.isNotEmpty()) {
                textDrawCenter(canvas,
                        if (isBold) TEXT_PAINT_BOLD else TEXT_PAINT_BOLD,
                        rect,
                        text)
            }
        }
        fun drawRect(canvas: Canvas, rect: Rect){
            //实际的边框可能是给定值的一半，或全部（扩展到矩形外面）
            canvas.drawRect(rect, STROKE_PAINT)
        }
        fun drawRect(canvas: Canvas, rect: RectF){
            //实际的边框可能是给定值的一半，或全部（扩展到矩形外面）
            canvas.drawRect(rect, STROKE_PAINT)
        }
        val FONT_SIZE: Float
            get() = 32F
        val FONT_FACE: Typeface
            get() = Typeface.MONOSPACE
        val FONT_COLOR: Int
            get() = Color.BLACK
        val STROKE_COLOR: Int
            get() = Color.RED
        val STROKE_SIZE: Float
            get() = 10F


        private val TEXT_PAINT:Paint
            get() = textPaint(FONT_COLOR, FONT_SIZE)
        private val TEXT_PAINT_BOLD:Paint
            get() = textPaint(FONT_COLOR, FONT_SIZE).apply { isFakeBoldText = true }
        private val STROKE_PAINT:Paint
            get() = strokePaint(STROKE_COLOR, STROKE_SIZE)

        private inline fun textDrawCenter(canvas: Canvas, textPaint: Paint, rect: Rect, msg: String) {
            canvas.drawText(msg,
                    rect.exactCenterX(), (rect.exactCenterY() - deltaCenterHeightFromFont(textPaint)),
                    textPaint)
        }
        private inline fun textDrawCenter(canvas: Canvas, textPaint: Paint, rect: RectF, msg: String) {
            canvas.drawText(msg,
                    rect.centerX(), (rect.centerY() - deltaCenterHeightFromFont(textPaint)),
                    textPaint)
        }
        private inline fun textPaint(colorInt: Int, fontSize: Float) =
                Paint().apply {
                    color = colorInt
                    style = Paint.Style.FILL
                    textAlign = Paint.Align.CENTER
                    textSize = fontSize
                    flags = Paint.ANTI_ALIAS_FLAG
                    typeface = FONT_FACE
                }

        private inline fun strokePaint(colorInt: Int, width:Float) =
                Paint().apply {
                    color = colorInt
                    style = Paint.Style.STROKE
                    strokeWidth = width
                }

        private inline fun deltaCenterHeightFromFont(paint: Paint) =
                paint.fontMetricsInt.let {
                    (it.top + it.bottom) / 2
                }
    }
}