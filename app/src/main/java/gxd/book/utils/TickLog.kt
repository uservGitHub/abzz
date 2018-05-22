package gxd.book.utils

/**
 * Created by Administrator on 2018/5/22.
 */

class TickLog {
    companion object {
        private val sb by lazy {
            StringBuilder()
        }
        val lines:String
            get() = sb.toString()
        fun ms(f:()->Unit){
            val startTick = System.currentTimeMillis()
            f.invoke()
            val endTick = System.currentTimeMillis()
            sb.append("${f::class.java.name},${endTick-startTick}\n")
        }

    }
}