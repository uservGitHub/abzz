package gxd.book

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.View
import android.widget.TextView
import org.jetbrains.anko.ctx
import org.jetbrains.anko.dip
import org.jetbrains.anko.sp

/**
 * Created by work on 2018/5/16.
 */

class BookBootstrapActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val headView = TextView(ctx).apply {
            text = "引导主界面"
            textSize = sp(30).toFloat()
            gravity = Gravity.CENTER
        }
        setContentView(headView)
        //setContentView(R.layout.activity_realm_basic_example)
    }
}

