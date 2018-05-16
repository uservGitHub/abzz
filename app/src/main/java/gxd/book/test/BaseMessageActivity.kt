package gxd.book.test

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import org.jetbrains.anko.horizontalScrollView
import org.jetbrains.anko.textView
import org.jetbrains.anko.verticalLayout

/**
 * Created by work on 2018/5/16.
 */

class BaseMessageActivity:AppCompatActivity(){
    lateinit var asyncMsg:TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verticalLayout {
            asyncMsg = textView {
                text = "没有信息"
            }
            horizontalScrollView {

            }
        }
    }
    private fun clrMsg(){
        asyncMsg.text = "重置"
    }
    class HolderItem(val activity:AppCompatActivity, val message:TextView)
}