package gxd.book.test

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.LinearLayout
import android.widget.TextView
import gxd.book.android.sdPath
import gxd.book.android.startBundle
import gxd.book.android.titleNotice
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
        asyncMsg = titleNotice("信息显示")


        verticalLayout {
            addView(asyncMsg)
            scrollView {
                linearLayout {
                    orientation = LinearLayout.HORIZONTAL
                    button {
                        text = "存储权限请求"
                        setOnClickListener {
                            RequestStorage.check(this@BaseMessageActivity)
                        }
                    }
                    button {
                        text = "打开文档"
                        setOnClickListener {
                            //val filename = "${ctx.sdPath}/one/lkjk75.PDF"
                            val filename = "${ctx.sdPath}/gxd.book/atest/testpdf.pdf"
                            val callType = true //正确的调用
                            if (callType){
                                PdfActivity.fromFileName(this@BaseMessageActivity, filename)
                            }else{
                                startBundle(PdfActivity::class.java)
                            }
                        }
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