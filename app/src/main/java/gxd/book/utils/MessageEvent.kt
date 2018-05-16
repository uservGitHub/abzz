package gxd.book.utils

/**
 * Created by Administrator on 2018/5/10.
 */

/**
 * 消息事件：
 * 代码，主题，数据
 */
data class MessageEvent(val code:Int, val title:String? = null, val data:Any? = null)