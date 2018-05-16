package gxd.book.crud

/**
 * Created by work on 2018/5/15.
 */

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import io.realm.Realm
import io.realm.RealmObject
import org.jetbrains.anko.*
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.KClass

/**
 * Created by Administrator on 2018/5/12.
 * 通用反射模型
 * 根据类名称（数据类），进行属性的反射并打包
 * 包装：对象相关的View及对象，列表项相关的序号分类等
 *
 * 输入：类名称及对象
 * 输出：属性名、值及其包装
 *
 * 作用：对象 <--> 字段视图
 * 对象 --> 字段视图
 * 字段视图 --> 对象
 */

class WhatView private constructor(val className: String) {
    companion object {
        val ENDKEY = ""
        val BEGKEY = "set"
        //val regGetMethod by lazy { "get(.*)$ENDKEY".toRegex() }
        //val regSetMethod by lazy { "set(.*)$ENDKEY".toRegex() }
        private val modelMap = hashMapOf<String, WhatView>()

        private inline fun modelFromClassName(className: String): WhatView {
            if (!modelMap.containsKey(className)) {
                modelMap.put(className, WhatView(className))
            }
            return modelMap[className]!!
        }

        //private inline fun getModel(obj: Any) = modelFromClassName(obj.javaClass.name)
        private inline fun getModel(obj: Any) = modelFromClassName(obj::class.java.name)

        /**
         * 创建对象的视图（Panel）
         * 1 从方法中提前属性名
         * 2 生成视图和位置信息(一次)
         */
        private fun createView(ctx: Context, obj: Any, isEdit: Boolean): View {
            val model = getModel(obj)
            val lines = model.lines

            var rowInd = -1
            var colInd = -1

            val FONT_SIZE = ctx.sp(26).toFloat()

            return ctx.UI {
                verticalLayout {
                    padding = ctx.dip(15)
                    layoutParams = ViewGroup.LayoutParams(matchParent, wrapContent)
                    backgroundColor = Color.YELLOW
                    lines.forEach {
                        //val name = it.getMName
                        val getM = model.methods[it.getMIndex]
                        val proValue = getM.invoke(obj)
                        val valueString = if (proValue==null)"null" else proValue.toString()
                        rowInd++
                        colInd = -1

                        linearLayout {

                            setPadding(0, dip(6), 0, dip(6))
                            backgroundColor = Color.GREEN
                            textView {
                                text = it.propName
                                textSize = FONT_SIZE
                                colInd++
                            }.lparams(width = dip(100))
                            val valueView =
                            if (isEdit) {
                                editText {
                                    setText(valueString)
                                    textSize = FONT_SIZE
                                    colInd++
                                    it.position(rowInd, colInd)
                                }
                            } else {
                                textView {
                                    text = valueString
                                    textSize = FONT_SIZE
                                    colInd++
                                    it.position(rowInd, colInd)
                                }
                            }
                            valueView.lparams(matchParent)
                        }.lparams(matchParent, wrapContent)
                    }
                }
            }.view
        }

        /**
         * 从对象到视图
         */
        fun toView(ctx: Context, fromObj: Any) = createView(ctx, fromObj, false)

        /**
         * 从对象到可编辑视图
         */
        fun toEditView(ctx: Context, fromObj: Any) = createView(ctx, fromObj, true)

        /**
         * 从视图更新对象
         */
        fun updateObj(obj: Any, fromView: View) {
            val model = getModel(obj)

            var root = fromView as ViewGroup
            model.lines.forEach {
                val line = root.getChildAt(it.rowInd) as ViewGroup
                val targetView = line.getChildAt(it.colInd) as TextView
                val proValueString = targetView.text.toString()
                //val name = it.setMName
                val setM = model.methods[it.setMIndex]
                setM.invoke(obj, proValueString)
            }
        }

        fun updateView(view: View, fromObj: Any) {
            val model = getModel(fromObj)

            var root = view as ViewGroup
            model.lines.forEach {
                val line = root.getChildAt(it.rowInd) as ViewGroup
                //val name = it.getMName
                val getM = model.methods[it.getMIndex]
                val proValue = getM.invoke(fromObj)
                val targetView = line.getChildAt(it.colInd)
                when {
                    targetView is EditText -> targetView.setText("$proValue")
                    targetView is TextView -> targetView.text = "$proValue"
                }
            }
        }
    }

    private val clazz: Class<*>
    private val _lines: MutableList<MethodData>
    private val lines: List<MethodData>
        inline get() = _lines
    private val methods: Array<Method>
        inline get() = clazz.declaredMethods

    init {
        clazz = Class.forName(className)
        _lines = mutableListOf<MethodData>()

        val regSetMethod = "$BEGKEY.*$ENDKEY".toRegex()
        //定义排序
        val filterMethods = methods
                .filter { regSetMethod.matches(it.name) }
                .sortedBy { it.name.length }

        val trimStarLength = BEGKEY.length
        //生成行索引
        filterMethods.forEach {
            val methodName = it.name
            val key = methodName.substring(trimStarLength, methodName.length)
            val propName = key[0].toLowerCase() + key.substring(1)
            val setMName = "set$key"
            val getMName = "get$key"
            val setInd = methods.indexOfFirst { it.name == setMName }
            val getInd = methods.indexOfFirst { it.name == getMName }
            _lines.add(MethodData(propName, setMName, getMName, setInd, getInd))
        }
    }

    /**
     * 解决位置和顺序的问题(属性名,set方法名,get方法名)
     */
    class MethodData(val propName: String,
                     val setMName: String, val getMName: String,
                     val setMIndex: Int, val getMIndex: Int) {
        //对象panel中的位置
        var rowInd = 0
            private set
        var colInd = 0
            private set

        fun position(rowIndex: Int, colIndex: Int) {
            rowInd = rowIndex
            colInd = colIndex
        }
    }
}
