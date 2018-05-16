package gxd.book.utils

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import gxd.android.endBundle
import gxd.android.startBundle
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.button
import org.jetbrains.anko.editText
import org.jetbrains.anko.textColor
import org.jetbrains.anko.verticalLayout

/**
 * Created by work on 2018/5/11.
 */

class FileRootActivity:AppCompatActivity(){
    companion object {
        val SELECTED = 1
        val CANCELLED = 2

        fun select(from:AppCompatActivity, pathname:String? = null){
            from.startBundle(FileRootActivity::class.java){
                if (pathname != null){
                    putString(KEY_PATHNAME, pathname)
                }
            }
        }
        private val KEY_PATHNAME = "pathname"
    }

    lateinit var etFileRoot: EditText
    private var edited = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pathname = endBundle?.getString(KEY_PATHNAME)
        verticalLayout {
            etFileRoot = editText {
                setText(pathname)
                textSize = 40F
                textColor = Color.BLACK
                addTextChangedListener(object :TextWatcher{
                    override fun afterTextChanged(s: Editable?) {
                        edited = true
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                    }
                })
            }
            button {
                text = "关闭"
                setOnClickListener {
                    EventBus.getDefault().post(MessageEvent(SELECTED,data = etFileRoot.text.toString()))
                    finish()
                }
            }
        }
    }
}