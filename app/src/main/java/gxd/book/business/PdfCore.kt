package gxd.book.business

import android.content.Context
import com.shockwave.pdfium.PdfiumCore

/**
 * Created by Administrator on 2018/5/16.
 */

class PdfCore {
    companion object {
        @Volatile
        var globalPdfiumCore: PdfiumCore? = null
            private set

        fun init(ctx: Context) {
            if (globalPdfiumCore == null) {
                globalPdfiumCore = PdfiumCore(ctx)
            }
        }
    }
}