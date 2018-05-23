package gxd.book.utils

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.LinearLayout
import gxd.book.android.lpMatchParent
import gxd.book.business.ManagedHost
import org.jetbrains.anko.*

/**
 * Created by work on 2018/5/23.
 */

class ManagedHostActivity:AppCompatActivity(){
    lateinit var target:ManagedHost
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        target = ManagedHost(ctx)
        verticalLayout {
            linearLayout {
                orientation = LinearLayout.HORIZONTAL
                button {
                    text = "Big"
                    setOnClickListener {
                        target.moveOffset(40F,40F)
                    }
                }
                button {
                    text = "Small"
                    setOnClickListener {
                        target.moveOffset(-40F,-40F)
                    }
                }
                checkBox {
                    text = "左"
                    isChecked = true
                    setOnCheckedChangeListener { buttonView, isChecked ->
                        target.selectedLeft = isChecked
                    }
                }
                checkBox {
                    text = "右"
                    isChecked = true
                    setOnCheckedChangeListener { buttonView, isChecked ->
                        target.selectedRight = isChecked
                    }
                }
            }

            addView(target, lpMatchParent())
        }
    }
}