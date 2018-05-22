package gxd.book

import android.app.Application
import android.util.Log
import gxd.book.business.PdfCore
import gxd.book.utils.TickLog
import io.realm.Realm

/**
 * Created by work on 2018/5/16.
 * https://www.jianshu.com/p/f665366b2a47
 */
class BookApplication:Application(){
    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
        PdfCore.init(this)

/*        TickLog.ms { Realm.init(this) }
        TickLog.ms { PdfCore.init(this) }
        Log.v("_BA", "${TickLog.lines}")*/
    }

    override fun onLowMemory() {
        super.onLowMemory()

    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
    }

    override fun onTerminate() {
        super.onTerminate()
    }
}