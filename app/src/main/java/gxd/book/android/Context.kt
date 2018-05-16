package gxd.book.android

import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import java.io.File

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
