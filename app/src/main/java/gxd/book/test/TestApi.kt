package gxd.book.test

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import gxd.book.android.*
import org.jetbrains.anko.*

/**
 * Created by Administrator on 2018/5/25.
 */

class TestApi:AppCompatActivity(),AnkoLogger{
    var toastNumIndex = 0
    val toastNum:()->Unit = {toast("Num: ${++toastNumIndex}")}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = verticalLayout {
            val toastList = mutableListOf<()->Unit>()
            1.rangeTo(10).forEach { toastList.add(toastNum) }

            addView(titleMessage("btn的高是matchParent宽是wrapContent,容器高wrapContent"))
            addView(horizontalBtns(toastList))
            addView(titleMessage("btn的高是matchParent宽是wrapContent,容器高80dip"))
            addView(horizontalBtns(toastList),lpMatchWidth(80))
            addView(titleMessage("btn的高是matchParent宽是80dip,容器高80dip"))
            addView(horizontalBtns(80,toastList),lpMatchWidth(80))

            addView(titleMessage("btn的高是wrapContent宽是matchParent,容器宽wrapContent"))
            addView(verticalBtns(toastList.take(2)))
            addView(titleMessage("btn的高是wrapContent宽是matchParent,容器宽80dip"))
            addView(verticalBtns(toastList), lpMatchHeight(80))
        }

    }
}