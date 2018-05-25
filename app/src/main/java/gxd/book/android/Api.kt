package gxd.book.android

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import org.jetbrains.anko.*

/**
 * Created by work on 2018/5/25.
 * Api一定是经常使用的、经过验证的、有实际场景需要的、模型完备的函数组合
 */

//region    lp[Math(W|H)|Wrap(W|H)|Size]
val lpMatchParent: ViewGroup.LayoutParams
     = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)
val lpWrapContent: ViewGroup.LayoutParams
     = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT)
fun Context.lpMatchWidth(dipHeight:Int)
    = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dip(dipHeight))
fun Context.lpMatchHeight(dipWidth: Int)
        = ViewGroup.LayoutParams(dip(dipWidth),ViewGroup.LayoutParams.MATCH_PARENT)
fun Context.lpWarpWidth(dipHeight:Int)
        = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dip(dipHeight))
fun Context.lpWrapHeight(dipWidth:Int)
        = ViewGroup.LayoutParams(dip(dipWidth),ViewGroup.LayoutParams.WRAP_CONTENT)
fun Context.lpSize(dipWidth: Int, dipHeight: Int)
        = ViewGroup.LayoutParams(dip(dipWidth), dip(dipHeight))
//endregion

/**
 * 获取代理反射名称中的代理名称
 * refName: 命名空间$代理名称$数字
 */
inline fun getFunName(refName:String):String {
    val begPos = refName.indexOfFirst { it == '$' }
    val endPos = refName.indexOfLast { it == '$' }
    if (begPos != -1 && begPos < endPos) {
        return refName.substring(begPos + 1, endPos)
    }
    return refName
}

/**
 * 水平排列的按钮（使用ViewGroup.add(btns)进行添加）
 */
//region    horizontalBtns btn可设定宽度，可回调
fun Context.horizontalBtns(vararg onDown:()->Unit) =
        horizontalBtns(onDown.toList())
fun Context.horizontalBtnvs(vararg onDown:(Button)->Unit) =
        horizontalBtnvs(onDown.toList())
fun Context.horizontalBtns(onDowns: List<() -> Unit>) = horizontalScrollView {
    linearLayout {
        orientation = LinearLayout.HORIZONTAL
        onDowns.forEach { function ->
            button {
                text = getFunName(function::class.java.name)
                setOnClickListener { function.invoke() }
            }.lparams(wrapContent, matchParent)
        }
    }
}
fun Context.horizontalBtnvs(onDowns: List<(Button) -> Unit>) = horizontalScrollView {
    linearLayout {
        orientation = LinearLayout.HORIZONTAL
        onDowns.forEach { function ->
            button {
                text = getFunName(function::class.java.name)
                setOnClickListener { function.invoke(this) }
            }.lparams(wrapContent, matchParent)
        }
    }
}

fun Context.horizontalBtns(dipWidth:Int, onDowns: List<()->Unit>)= horizontalScrollView {
    linearLayout {
        orientation = LinearLayout.HORIZONTAL
        onDowns.forEach { function ->
            button {
                text = getFunName(function::class.java.name)
                setOnClickListener { function.invoke() }
            }.lparams(dip(dipWidth), matchParent)
        }
    }
}
fun Context.horizontalBtns(dipWidth:Int, vararg onDown:()->Unit)=
        horizontalBtns(dipWidth, onDown.toList())
fun Context.horizontalBtnvs(dipWidth:Int, vararg onDown:(Button)->Unit)=
        horizontalBtnvs(dipWidth, onDown.toList())
fun Context.horizontalBtnvs(dipWidth:Int, onDowns: List<(Button)->Unit>)= horizontalScrollView {
    linearLayout {
        orientation = LinearLayout.HORIZONTAL
        onDowns.forEach { function ->
            button {
                text = getFunName(function::class.java.name)
                setOnClickListener { function.invoke(this) }
            }.lparams(dip(dipWidth), matchParent)
        }
    }
}
//endregion
//region    verticalBtns
fun Context.verticalBtns(vararg onDown:()->Unit) = verticalBtns(onDown.toList())
fun Context.verticalBtnvs(vararg onDown:(Button)->Unit) = verticalBtnvs(onDown.toList())
fun Context.verticalBtns(onDowns: List<() -> Unit>) = scrollView {
    verticalLayout {
        onDowns.forEach { function ->
            button {
                text = getFunName(function::class.java.name)
                setOnClickListener { function.invoke() }
            }.lparams(matchParent, wrapContent)
        }
    }
}
fun Context.verticalBtnvs(onDowns: List<(Button) -> Unit>) = scrollView {
    verticalLayout {
        onDowns.forEach { function ->
            button {
                text = getFunName(function::class.java.name)
                setOnClickListener { function.invoke(this) }
            }.lparams(matchParent, wrapContent)
        }
    }
}
//endregion




