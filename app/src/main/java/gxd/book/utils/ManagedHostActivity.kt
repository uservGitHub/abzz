package gxd.book.utils

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import gxd.book.android.lpMatchParent
import gxd.book.business.ManagedHost
import org.jetbrains.anko.button
import org.jetbrains.anko.ctx
import org.jetbrains.anko.verticalLayout

/**
 * Created by work on 2018/5/23.
 */

class ManagedHostActivity:AppCompatActivity(){
    lateinit var target:ManagedHost
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        target = ManagedHost(ctx)
        verticalLayout {
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

            addView(target, lpMatchParent())
        }
    }
}