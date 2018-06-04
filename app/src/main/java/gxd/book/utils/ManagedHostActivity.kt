package gxd.book.utils

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import gxd.book.android.buttons
import gxd.book.android.lpMatchParent
import org.jetbrains.anko.*

/**
 * Created by work on 2018/5/23.
 */

class ManagedHostActivity:AppCompatActivity(){
    /*lateinit var target:ManagedHostAdv
    val addLast :() ->Unit = {target.add(0)}
    val remLast:()->Unit = {if (target.visRects.size>0) target.remove(target.visRects.last())}
    val remFirst:()->Unit = {if (target.visRects.size>0) target.remove(target.visRects.first())}
    val horArrange:()->Unit = {target.typeArrange(VisRect.HOR_ALL)}
    val verArrange:()->Unit = {target.typeArrange(VisRect.VER_ALL)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        target = ManagedHostAdv(ctx)
        val controlPanel = ctx.buttons(addLast, remFirst, remLast, horArrange, verArrange)//(addHor,addVer,removeHor,removeVer)

        verticalLayout {
            //只能手动加入
            addView(controlPanel)
            addView(target, lpMatchParent)
        }
        //setContentView(ctx.buttons(addVisRect,popVisRect))
    }*/
}