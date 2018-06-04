package gxd.book.test

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import gxd.book.android.buttons
import gxd.book.android.lpMatchParent
import org.jetbrains.anko.ctx
import org.jetbrains.anko.toast
import org.jetbrains.anko.verticalLayout

/**
 * Created by Administrator on 2018/5/27.
 */

class TestManagedHost:AppCompatActivity(){
    //lateinit var target: ManagedHostAdv

    /*val addLast :() ->Unit = {target.add(0)}
    val remLast:()->Unit = {if (target.visRects.size>0) target.remove(target.visRects.last())}
    val remFirst:()->Unit = {if (target.visRects.size>0) target.remove(target.visRects.first())}
    val horArrange:()->Unit = {target.typeArrange(VisRect.HOR_ALL)}
    val verArrange:()->Unit = {target.typeArrange(VisRect.VER_ALL)}
    val count:()->Unit={toast("${target.visRects.size}")}
    val reverse:()->Unit = {target.reverse()}
    val moveClip:()->Unit = {target.typeMoving(VisRect.MOVE_CLIP)}
    val moveWorld:()->Unit = {target.typeMoving(VisRect.MOVE_WORLD)}
*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //target = ManagedHostAdv(ctx)
        //val controlPanel = ctx.buttons(moveClip,moveWorld,count,reverse,addLast, remFirst, remLast, horArrange, verArrange)//(addHor,addVer,removeHor,removeVer)

        verticalLayout {
            //只能手动加入
            //addView(controlPanel)
            //addView(target, lpMatchParent)
        }
    }
}