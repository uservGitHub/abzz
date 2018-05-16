package gxd.book.android

import android.app.Activity
import android.app.Fragment
import android.os.Bundle

/**
 * 实际参数
 * 需要Fragment在 onActivityCreated 中进行解析处理
 */
inline fun Fragment.actualArgs(init:Bundle.()->Unit) =apply {
    arguments = Bundle().apply(init)
}