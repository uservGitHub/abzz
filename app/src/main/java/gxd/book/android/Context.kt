package gxd.book.android

import android.content.Context
import android.graphics.Color
import android.os.Environment
import android.os.storage.StorageManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.jetbrains.anko.*
import java.io.File

inline fun Context.titleError(title:String) = TextView(this).apply {
    text = title
    textSize = sp(30).toFloat()
    textColor = Color.RED
    gravity = Gravity.CENTER
}
inline fun Context.titleSuccess(title:String) = TextView(this).apply {
    text = title
    textSize = sp(30).toFloat()
    textColor = Color.GREEN
    gravity = Gravity.CENTER
}
inline fun Context.titleNotice(title:String) = TextView(this).apply {
    text = title
    textSize = sp(30).toFloat()
    textColor = Color.BLUE
    gravity = Gravity.CENTER
}
inline fun Context.titleMessage(title:String) = TextView(this).apply {
    text = title
    textSize = sp(26).toFloat()
    textColor = Color.BLACK
    gravity = Gravity.LEFT
}

inline fun Context.lpFromSize(widthDip:Int, heightDip:Int) =
        ViewGroup.LayoutParams(dip(widthDip), dip(heightDip))
inline fun Context.lpFromWidth(widthDip:Int) =
        ViewGroup.LayoutParams(dip(widthDip), matchParent)
inline fun Context.lpFromHeight(heightDip:Int) =
        ViewGroup.LayoutParams(matchParent, dip(heightDip))
inline fun Context.lpMatchParent() =
        ViewGroup.LayoutParams(matchParent,matchParent)
inline fun Context.lpWrapContent() =
        ViewGroup.LayoutParams(wrapContent,wrapContent)
inline fun Context.lpMatchWidth() =
        ViewGroup.LayoutParams(matchParent,wrapContent)
inline fun Context.lpMatchHeight() =
        ViewGroup.LayoutParams(wrapContent,matchParent)

fun Context.buttons(vararg clicks:()->Unit) = horizontalScrollView {
    linearLayout {
        orientation = LinearLayout.HORIZONTAL
        clicks.forEachIndexed { index, function ->
            button {
                //如果可以反射出function的名称，就用反射出的名称
                val str = function::class.java.name
                val begPos = str.indexOfFirst { it == '$' }
                val endPos = str.indexOfLast { it == '$' }
                text = str.substring(begPos+1, endPos)
                setOnClickListener {
                    function.invoke()
                }
            }
        }
    }
}

//(View)->Unit : 看设置Button的View，及执行其他代码
fun Context.controlHeader(up:(View)->Boolean,
                                 down:(View)->Boolean,
                                 left:(View)->Boolean,
                                 right:(View)->Boolean,
                          update:(View)->Boolean)= horizontalScrollView {
    linearLayout {
        orientation = LinearLayout.HORIZONTAL
        val updateColor = fun(v: View, result: Boolean) {
            v.backgroundColor = if (result) Color.LTGRAY else Color.DKGRAY
        }
        button {
            text = "Up";setOnClickListener { updateColor(this, up(this)) }
        }
        button {
            text = "Down";setOnClickListener { updateColor(this, down(this)) }
        }
        button {
            text = "Left";setOnClickListener { updateColor(this, left(this)) }
        }
        button {
            text = "Right";setOnClickListener { updateColor(this, right(this)) }
        }
        button {
            text = "Update";setOnClickListener { updateColor(this, update(this)) }
        }
    }
}




val Context.sdPath:File
    inline get() = Environment.getExternalStorageDirectory()
val Context.sdOutPath:File?
    get() {
        if (_sdOutPath != null) {
            val storageManager = this.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            _sdOutPath = getOutMemory(storageManager)
        }
        return if (_sdOutPath == null) null
        else File(_sdOutPath)
    }

private var _sdOutPath:String? = null

private inline fun getOutMemory(storageManager:StorageManager):String?{
    try {
        val storageVolumeClass = Class.forName("android.os.storage.StorageVolume")
        val volumeList = storageManager.javaClass.getMethod("getVolumeList")
        val path = storageVolumeClass!!.getMethod("getPath")
        val removable = storageVolumeClass.getMethod("isRemovable")
        val result = volumeList.invoke(storageManager)
        val length = java.lang.reflect.Array.getLength(result)
        for (i in 0 until length){
            val storageVolumeElement = java.lang.reflect.Array.get(result, i)
            val name = path.invoke(storageVolumeElement) as String
            val canRemove = removable.invoke(storageVolumeElement) as Boolean
            if (canRemove) {
                return name
            }
        }
    }catch (e:Exception){
        e.printStackTrace()
    }
    return null
}
