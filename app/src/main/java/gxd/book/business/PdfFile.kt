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

/**
 * Created by Administrator on 2018/5/22.
 * 后期会有缓存机制
 *
 * 分步骤执行，方便多线程分层调用
 * 1. 打开文件 openFile [0,1]
 * 2. 打开文档 openDoc [10,100]
 * 3. 打开页  loadPage [20,30]
 * 4. 呈现页  bitmap [8,20]
 */

class PdfFile(val path:String){
    //region    资源释放后为null
    private var doc:PdfDocument? = null
    private var openedPages:SparseBooleanArray? = null
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
                val ptr = pdfCore.openPage(doc!!, ind)
                debugErrorPages.append("$ptr,")
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
            //检查pfd，预期已清理
            assert(pfd!=null)
            doc = null
            //暂不清除，测试打开页是否每次都变化，预期是每次都变化
            //openedPages!!.clear()
        }
    }
}