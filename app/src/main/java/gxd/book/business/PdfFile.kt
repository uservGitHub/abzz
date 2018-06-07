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

class PdfFile(val file: File){
    constructor(path:String):this(File(path))
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

/**
 * 垂直滑动
 * 1 不存在世界坐标系。
 * 2 提供位图坐标系（0,0）对应zeroPageInd的左上点。
 * 3 位图垂直坐标系有效范围[startBoundary,endBoundary]，是活动边界，差值不超过maxBoundaryWidth。
 * 4 位图水平坐标系的范围[0,sideLength]，是固定边界。
 *  注意：3和4中，像素值的有效范围是终止点-1，如水平像素范围[0,sideLength-1]。
 * 5 单个位图宽是sideLength，高不超过maxBitmapLength
 * 6 位图坐标系（0,0）对应的世界坐标点（zeroWorldX,zeroWorlY）。
 */
class VerticalSlide private constructor(
        val isVertical: Boolean,
        sideLength:Int,
        pdfFile: PdfFile,
        startPageInd:Int) {
    constructor(length: Int, pdf: PdfFile) : this(true, length, pdf, 0)

    var sideLength: Int
        private set
    var pdfFile: PdfFile
        private set
    var startBoundary:Int = 0
        private set
    var endBoundary:Int = 0
        private set
    var zeroPageInd:Int = -1
        private set
    var maxBoundaryWidth:Int = 20000
        private set
    var maxBitmapLength:Int = 2000
        private set

    val pageList:MutableList<Block> = mutableListOf()

    init {
        this.sideLength = sideLength
        this.pdfFile = pdfFile
        this.zeroPageInd = startPageInd

        val firstBlock = buildAndFillBlock(startPageInd)
        pageList.add(firstBlock)
        endBoundary = firstBlock.pageLength
    }

    private fun buildAndFillBlock(pageInd: Int):Block{
        val size = pdfFile.pageSize(pageInd)
        //变换后的长度(Int)
        val calcLength =
                if (isVertical) size.height * sideLength / size.width
                else size.width * sideLength / size.height

        //Int
        val factor = 1 + calcLength / maxBitmapLength
        //Int,尽可能等分像素点
        val avgLength = calcLength / factor

        return Block(pageInd, calcLength, avgLength).apply {
            fillBlock(this, factor)
        }
    }

    /**
     * 不考虑特别大的页，一次填充（暂时的）
     * 以后考虑前向/后向部分填充
     */
    private fun fillBlock(block:Block, factor:Int) {
        var sumLength = 0
        for (bmpInd in 0..factor) {
            sumLength += block.avgLength
            val bmp = Bitmap.createBitmap(
                    if (isVertical) sideLength else block.avgLength,
                    if (isVertical) block.avgLength else sideLength,
                    Bitmap.Config.RGB_565)
            block.addFrist(bmp)
        }
        val remainLength = block.pageLength - sumLength
        if (remainLength > 0) {
            val bmp = Bitmap.createBitmap(
                    if (isVertical) sideLength else remainLength,
                    if (isVertical) remainLength else sideLength,
                    Bitmap.Config.RGB_565)
            block.addFrist(bmp)
        }
    }

    @Volatile
    var pulling:Boolean = false
        private set

    var zeroWorldX: Float = 0F
        private set
    var zeroWorldY: Float = 0F
        private set

    /**
     * 零点在世界坐标系中的位置
     */
    fun pageZeroFromWorld(x:Float, y:Float){
        zeroWorldX = x
        zeroWorldY = y
    }

    /**
     * 下拉加载（边界上移）
     */
    fun pullDownLoad(worldTopY:Float){
        if (pulling) return

    }

    /**
     * 上拉加载（边界下移）
     */
    fun pullUpLoad(worldBottomY:Float){

    }

    //页索引号，页变换后的长度（更加sideLength变换），位图列表
    inner class Block(val pageInd: Int,val pageLength: Int,val avgLength:Int) {
        private val bmpList: MutableList<Bitmap>

        init {
            bmpList = mutableListOf()
        }


        private fun remove(bmp: Bitmap): Int {
            val length = if (isVertical) bmp.height else bmp.width
            if (bmp.isRecycled) {
                bmp.recycle()
            }
            bmpList.remove(bmp)
            return length
        }

        /**
         * 后向访问，位图坐标增大
         */
        val backwardList: List<Bitmap> get() = bmpList
        /**
         * 前向访问，位图坐标减小
         */
        val forwardList: List<Bitmap> get() = bmpList.reversed()
        fun removeFirst() =
                if (bmpList.size == 0) 0
                else remove(bmpList.first())

        fun removeLast() =
                if (bmpList.size == 0) 0
                else remove(bmpList.last())

        fun addFrist(bmp: Bitmap) = bmpList.add(0, bmp)
        fun addLast(bmp: Bitmap) = bmpList.add(bmp)
        fun clear(): Int {
            var sumLength = 0
            bmpList.forEach { sumLength += remove(it) }
            bmpList.clear()
            return sumLength
        }
    }
}




