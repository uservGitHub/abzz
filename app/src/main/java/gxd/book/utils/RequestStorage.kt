package gxd.book.utils

import android.Manifest
import android.support.v7.app.AppCompatActivity
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import gxd.book.android.startBundle
import org.greenrobot.eventbus.EventBus

class RequestStorage:AppCompatActivity(){
    companion object {
        val SUCCESS = 1
        val FAILURE = 2
        val ALLREADY = 3

        fun check(activity: AppCompatActivity) {
            activity.startBundle(RequestStorage::class.java)
        }
    }
    private val permissionArray = arrayOf<String>(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private val permissionCode = 1000
    private val allreadyText = "权限已存在"
    private val successText = "授权成功"
    private val failureText = "授权失败"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkPermission()){
            //存在立即返回
            EventBus.getDefault().post(MessageEvent(ALLREADY, allreadyText))
            finish()
        }else {
            //请求权限
            ActivityCompat.requestPermissions(
                    this@RequestStorage,
                    permissionArray,
                    permissionCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        var notOk = true
        if (requestCode == permissionCode) {
            notOk = grantResults.any {
                PackageManager.PERMISSION_GRANTED != it
            }
        }

        EventBus.getDefault().post(
                MessageEvent(
                        if (notOk) FAILURE else SUCCESS,
                        if (notOk) failureText else successText))
        finish()
    }

    private fun checkPermission() = !permissionArray.any {
        PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this@RequestStorage, it)
    }
}