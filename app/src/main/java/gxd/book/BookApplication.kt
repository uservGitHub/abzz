package gxd.book

import android.app.Application
import io.realm.Realm

/**
 * Created by work on 2018/5/16.
 */
class BookApplication:Application(){
    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
    }
}