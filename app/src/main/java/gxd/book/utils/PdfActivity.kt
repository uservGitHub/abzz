package gxd.book.utils

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.ImageView
import gxd.book.android.*
import gxd.book.business.NormalView
import gxd.book.business.PdfFile
import org.jetbrains.anko.button
import org.jetbrains.anko.ctx
import org.jetbrains.anko.imageView
import org.jetbrains.anko.verticalLayout
import java.io.File

/**
 * Created by work on 2018/5/17.
 */

class PdfActivity:AppCompatActivity() {
    companion object {
        val KEY_FILENAME = "filename"
        fun fromFileName(activity: AppCompatActivity, fileName: String) {
            activity.startBundle(PdfActivity::class.java) {
                putString(KEY_FILENAME, fileName)
            }
        }
    }

    var file: File? = null
    lateinit var img:ImageView
    lateinit var pdffile:PdfFile
    lateinit var bmpHost:NormalView
    private var time = 0L
    private val sb = StringBuilder()
    private inline fun calc(f:()->Unit){

        val current = System.currentTimeMillis()
        sb.append("${current-time}\n")
        time = current
    }
    private inline fun restart(){
        time = System.currentTimeMillis()
    }
    var pageInd = 0
    private fun loadBitmap(){
        lateinit var bmp:Bitmap
        TickLog.ms {pdffile.openFile()}
        TickLog.ms { pdffile.openDoc() }
        TickLog.ms { pdffile.loadPages(10,10) }
        val rect = Rect(0,0, 1000, 1200)
        TickLog.ms { bmp = pdffile.bmpFromRect(rect, 0.5F, pageInd.rem(10)+10)!! }
        pageInd++
        TickLog.ms { pdffile.closeDoc() }

        img.setImageBitmap(bmp)

        Log.v("_PA", "${TickLog.lines}")
        Log.v("_PA", "${pdffile.debugErrorPages.toString()}")
    }
    private fun normalStart() {
        pdffile = PdfFile(file!!.absolutePath)
        bmpHost = NormalView(ctx)
        verticalLayout {
            /*addView(titleSuccess("参数正确"))
            addView(titleNotice(file!!.absolutePath))
            button {
                text = "加载文件"
                setOnClickListener {
                    loadBitmap()
                }
            }
            img = imageView {
            }*/
            addView(bmpHost, ctx.lpMatchParent())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var errMsg = "参数检查中..."
        endBundle?.getString(KEY_FILENAME)?.also {
            val f = File(it)
            when {
                !f.exists() -> errMsg = "文件不存在：$it"
                !f.isFile -> errMsg = "不是文件"
                !f.canRead() -> errMsg = "无法读取"
                !f.name.endsWith(".pdf", true) -> errMsg = "后缀不正确：${f.name}"
                else -> {
                    errMsg = ""
                    file = f
                }
            }
        }
        if (file ==null){
            setContentView(titleError(errMsg))
            return
        }

        normalStart()
    }
}