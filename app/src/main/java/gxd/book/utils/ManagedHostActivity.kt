package gxd.book.utils

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.LinearLayout
import gxd.book.android.buttons
import gxd.book.android.lpMatchParent
import gxd.book.business.ManagedHost
import gxd.book.business.ManagedHostAdv
import org.jetbrains.anko.*

/**
 * Created by work on 2018/5/23.
 */

class ManagedHostActivity:AppCompatActivity(){
    lateinit var target:ManagedHostAdv

    val addHor:()->Unit = {target.addHor()}
    val addVer:()->Unit = {target.addVer()}
    val removeHor:()->Unit = {if (target.visRects.size>0) target.removeHor(target.visRects[0])}
    val removeVer:()->Unit = {if (target.visRects.size>0) target.removeVer(target.visRects[0])}
    val addNew:()->Unit = {target.addNewVisRect()}
    val removeLast:()->Unit = {target.removeLastVisRect()}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        target = ManagedHostAdv(ctx)
        val controlPanel = ctx.buttons(addNew, removeLast)//(addHor,addVer,removeHor,removeVer)

        verticalLayout {
            //只能手动加入
            addView(controlPanel)
            addView(target, lpMatchParent())
        }
        //setContentView(ctx.buttons(addVisRect,popVisRect))
    }
}