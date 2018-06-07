package gxd.book.business

import android.graphics.*
import android.util.Log
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.util.*

private val debugRandColor:Int get() {
    val r = Random()
    return Color.rgb(r.nextInt(256), r.nextInt(256), r.nextInt(256))
}

/**
 * 滑动页管理
 * 初始载入的第一页的左上点为（0,0）
 * 一个固定维度，单位是像素；一个可变维度，单位是页
 * 呈现范围（在可变维度上）：[startBoundary,endBoundary]，单位是像素
 * 访问方式：输入startValue，length（一定在呈现范围内），输出页的序列
 * 下拉/上拉操作：维护指定的最大页数量，滚动页，更新呈现范围
 * 初始载入小于等于3页
 * 滚动一次1页
 * 最大页数量7页
 * 目前限制：只能进行垂直滚动
 */
class SlidePageManager private constructor(
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