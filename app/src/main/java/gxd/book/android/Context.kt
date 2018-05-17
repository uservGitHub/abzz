package gxd.book.android

import android.content.Context
import android.graphics.Color
import android.os.Environment
import android.os.storage.StorageManager
import android.view.Gravity
import android.widget.TextView
import org.jetbrains.anko.sp
import org.jetbrains.anko.textColor
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
