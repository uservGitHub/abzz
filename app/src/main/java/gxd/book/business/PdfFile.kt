package gxd.book.business

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.SparseBooleanArray
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import com.shockwave.pdfium.util.Size
import java.io.File
import java.nio.channels.FileLock

/**
 * Created by Administrator on 2018/5/22.
 * 后期会有缓存机制
 *
 * 分步骤执行，方便多线程分层调用
 * 1. 打开文件 openFile [0,1]
 * 2. 打开文档 openDoc [10,100]
 * 3. 打开页  loadPage [20,30]
 * 4. 呈现页  bitmap [8,20]
 *
 * 加载大文件不会有明显耗时，因此推断页的Ptr是指向文件的地址（错误的推断），
 * 只能推断出文件不会一次加载到内存中。
 */

class PdfFile(val path:String){
    //region    资源释放后为null
    private var doc:PdfDocument? = null
    //pagePtr在doc关闭后也无法使用
    private var openedPages:SparseBooleanArray? = null
    //pfd在doc关闭后无法使用，因此没有缓存的必要
    private var pfd:ParcelFileDescriptor? = null
    //endregion

    //时间戳，访问时更新
    private var time:Long = -1

    private val pdfCore:PdfiumCore
    //调试项
    val debugErrorPages = StringBuilder()

    init {
        pdfCore = PdfCore.globalPdfiumCore!!
    }

    private inline fun openPage(ind:Int):Boolean{
        if (ind<0) return false

        //保障页ind只被打开一次（含错误页）
        if (openedPages!!.indexOfKey(ind)<0){
            try {
                pdfCore.openPage(doc!!, ind)
                openedPages!!.put(ind, true)
            }catch (e:Exception){
                openedPages!!.put(ind, false)
                debugErrorPages.append("\nind=$ind,err=${e.toString()}\n")
            }
        }
        return openedPages!!.get(ind)
    }

    val pageCount:Int by lazy(LazyThreadSafetyMode.NONE) {
        pdfCore.getPageCount(doc!!)
    }
    fun pageSize(ind: Int) = pdfCore.getPageSize(doc!!, ind)

    fun bmpFromRect(rect: Rect, zoom:Float, ind: Int): Bitmap? {
        if (openPage(ind)) {
            time = System.currentTimeMillis()
            val bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.RGB_565)
            val rectSize = pageSize(ind)
            val pageWidth = (zoom * rectSize.width).toInt()
            val pageHeight = (zoom * rectSize.height).toInt()
            pdfCore.renderPageBitmap(doc!!, bitmap, ind,
                    -rect.left, -rect.top, pageWidth, pageHeight)
            return bitmap
        }
        return null
    }

    fun loadPages(ind: Int, count:Int=1) {
        val lastInd = Math.min(ind + count - 1, pageCount - 1)
        for (pageInd in ind..lastInd) {
            openPage(pageInd)
        }
    }
    fun openFile() {
        if (pfd == null) {
            val file = File(path)
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }
    }
    fun openDoc(){
        if (doc == null){
            doc = pdfCore.newDocument(pfd)
            if (openedPages == null){
                openedPages = SparseBooleanArray(15)
            }
        }
    }
    fun closeDoc(){
        if (doc != null){
            pdfCore.closeDocument(doc)
            //检查pfd，已经没有引用且close()，不能再用了
            pfd = null
            doc = null
            time = -1
            openedPages!!.clear()
        }
    }
}

class FileManager(fileName:String){
    private val pdf:PdfFile
    init {
        pdf = PdfFile(fileName)
        pdf.openFile()
        pdf.openDoc()
    }
    fun close(){
        pdf.closeDoc()
    }
    fun loadPages(){

    }

    class RenderPages(val pages:List<RenderPage>) {
        fun fetch(begY: Int, endY:Int): List<Bitmap> {
            var begInd = pages.indexOfFirst { begY>=it.y }
            var endInt = pages.indexOfLast { endY>it.y }
            return pages.subList(begInd,endInt).map { it.bmp }
        }
    }
    class RenderPage(val ind: Int,val bmp:Bitmap, val y:Int){
        internal fun dispose(){
            if (bmp.isRecycled){
                bmp.recycle()
            }
        }
    }
}

/**
 * 滑动加载（处理器）
 * 滚动方向：默认垂直，定制像素长度，窗口长度
 */
class SlideLoad(val isVertical:Boolean = true,val customLength:Int,val length:Int){
    var minBound:Float = 0F
        private set
    var maxBound:Float = 0F
        private set


    private lateinit var pdfFile: PdfFile
    private lateinit var frames:MutableList<PageFrame>
    fun jump(pageInd:Int){
        //pdfFile.loadPages(pageInd, 10)

    }



    fun load(begPageInd:Int, length: Int) {
        var sumLength = 0
        for (pInd in begPageInd..pdfFile.pageCount - 1) {
            val size = pdfFile.pageSize(pInd)

            val calcLength =
                    if (isVertical) customLength / size.width * size.height
                    else customLength / size.height * size.width

            if (sumLength >= length) {
                break
            }
        }
    }

    data class PageFrame(val pageInd:Int,val pageLength:Int)
}


class TestSideLoading{
    companion object {
        private lateinit var pdfFile: PdfFile
        private var fixedLength: Int = 100
        private var isVertical = true
        private var maxBitmapLength = 1600
        private lateinit var pageFrames: MutableList<PageFrame>
        private lateinit var bitmapFrames: MutableList<BitmapFrame>

        /**
         * 初始化pdfFile
         * 打开文件，打开PdfDocument
         */
        fun initPdf() {
            pdfFile.openFile()
            pdfFile.openDoc()
        }

        /**
         * 向后加载：从指定页fromInd（可从0开始），向后装载指定的长度visLength（像素）
         * 更新pageFrames(前向添加或后向添加)
         * 返回：toInd（截止页）
         */
        fun loadTail(fromInd: Int, visLength: Int): Int {
            assert(fromInd in 0..pdfFile.pageCount - 1)

            var toInd = -1
            var sumLength = 0
            for (ind in fromInd until pdfFile.pageCount) {
                val size = pdfFile.pageSize(ind)
                val calcLength =
                        if (isVertical) size.height * fixedLength / size.width
                        else size.width * fixedLength / size.height

                pageFrames.add(PageFrame(ind, calcLength, mutableListOf<Int>()))

                sumLength += calcLength
                toInd = ind
                if (sumLength >= visLength) {
                    break
                }
            }

            return toInd
        }

        /**
         * 向前加载：从指定页toInd（可从0开始），向前装载指定的visLength（像素）
         * 更新pageFrames(前向添加或后向添加)
         * 返回：fromInd（截止页）
         */
        fun loadHead(toInd: Int, visLength: Int): Int {
            assert(toInd in 0..pdfFile.pageCount - 1)

            var fromInd = -1
            var sumLength = 0
            for (ind in toInd downTo 0) {
                val size = pdfFile.pageSize(ind)
                val calcLength =
                        if (isVertical) size.height * fixedLength / size.width
                        else size.width * fixedLength / size.height

                pageFrames.add(0, PageFrame(ind, calcLength))
                sumLength += calcLength
                fromInd = ind
                if (sumLength >= visLength) {
                    break
                }
            }

            return fromInd
        }

        /**
         * 关闭pdfFile
         */
        fun closePdf() {
            pdfFile.closeDoc()
        }

        /**
         * 填充长度
         */
        fun fillHeadAvgLength(pageFrame: PageFrame, visLength: Int) {
            if (pageFrame.pageLength <= maxBitmapLength) {
                pageFrame.bmpList.add(pageFrame.pageLength)
                pageFrame.isFillFinish = true
                return
            }

            //尽可能等分像素点
            val factor = 1 + pageFrame.pageLength / maxBitmapLength
            val avgLength = pageFrame.pageLength / factor

            var sumLength = 0
            for (i in 0 until factor) {
                pageFrame.bmpList.add(avgLength)
                sumLength += avgLength

                if (sumLength > visLength) {
                    pageFrame.isFillFinish = false
                    return
                }
            }
            val remain = pageFrame.pageLength - sumLength
            if (remain > 0) {
                pageFrame.bmpList.add(remain)
            }
            pageFrame.isFillFinish = true
        }
    }

    data class PageFrame(val pageInd:Int,val pageLength:Int,val bmpList:MutableList<Int>,var isFillFinish:Boolean)
    data class BitmapFrame(val bmp: Bitmap, val worldAxisValue:Int) //小值坐标值
}



