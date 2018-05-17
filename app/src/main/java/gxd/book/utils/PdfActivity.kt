package gxd.book.utils

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import gxd.book.android.*
import org.jetbrains.anko.ctx
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
    lateinit var outHostView: TextView
    private fun normalStart() {
        verticalLayout {
            titleSuccess("参数正确")
            titleNotice(file!!.absolutePath)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        endBundle?.getString(KEY_FILENAME).also {
            val f = File(it)
            if (f.exists() && f.isFile && f.canRead() && f.name.endsWith(".pdf",true)){
                file = f
            }
        }
        if (file ==null){
            setContentView(titleError("无效的参数"))
            return
        }

        normalStart()
    }
}