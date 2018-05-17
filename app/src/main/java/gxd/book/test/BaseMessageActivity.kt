package gxd.book.test

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import gxd.book.android.sdPath
import gxd.book.android.startBundle
import gxd.book.utils.MessageEvent
import gxd.book.utils.PdfActivity
import gxd.book.utils.RequestStorage
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.*

/**
 * Created by work on 2018/5/16.
 */

class BaseMessageActivity:AppCompatActivity(){
    lateinit var asyncMsg:TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)

        verticalLayout {
            asyncMsg = textView {
                text = "没有信息"
            }
            scrollView {
                button {
                    text = "存储权限请求"
                    setOnClickListener {
                        RequestStorage.check(this@BaseMessageActivity)
                    }
                }
                button {
                    text = "打开文档"
                    setOnClickListener {
                        val filename = "${ctx.sdPath}/one/lkjk75.PDF"
                        PdfActivity.fromFileName(this@BaseMessageActivity, filename)
                    }
                }
            }
        }
    }
    private fun clrMsg(){
        asyncMsg.text = "重置"
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent) {
        asyncMsg.text = "$event"
    }
    class HolderItem(val activity:AppCompatActivity, val message:TextView)
}