package gxd.book.utils

/**
 * Created by Administrator on 2018/5/12.
 */

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import org.reactivestreams.Subscription
import java.util.*

/**
 * RxJava日志（输出行信息的回调）
 * 多次调用不需要重新设置
 * 每次excute(是一个过程，结束时执行异步回调)后清理打印信息
 */
class RxLog private constructor() {
    /**
     * 调用方式：
     * 1. 重置（总控设置、分控设置）
     * 2. 加入桩信息
     * 3. 执行（启动Rx，可自定义订阅）
     * 4、分控回调执行、总控回调执行
     */
    companion object {
        val INSTANCE: RxLog by lazy {
            RxLog()
        }
    }

    //region    具体接口

    /**
     * 总控设置，覆盖现有（配置项立即生效，回调项最后执行）
     */
    fun resetRxModel(allEndAction: ((RxLog) -> Unit)? = null):Boolean {
        //运行中，无法设置
        if (checkRunning()){
            return false
        }
        endActionFromRxModel = allEndAction
        return true
    }

    /**
     * 分控设置，本次有效，最后要重置
     */
    fun reset(endAction:((RxLog)->Unit)? = null, hasAutoPillNext:Boolean = false):Boolean{
        //运行中，无法设置
        if (checkRunning()){
            return false
        }
        isNext = hasAutoPillNext
        this.endAction = endAction
        return true
    }

    /**
     * 过程信息
     */
    val dumpMessage:String
        get() = sb.toString()

    /**
     * 执行，执行以下步骤
     * 1. 确定执行起点（清理）
     * 2. 过程（第一次，异常，结束）
     * 3. 结束回调（清理）
     */
    fun excute(source:Observable<out Any>,
               onSubNext:((Any)->Unit)?=null,
               onSubError:(()->Unit)?=null,
               onSubComplete:(()->Unit)?=null) {
        /*if (isEnd != null) {
            //这条信息无法输出到用户，在reset已经检查，去掉
            sb.append("StartError:无法开始（未重置或无输出）！\n")
            return
        }*/

        //初始化
        initialize()

        //启动
        start()

        innerExcute(source,onSubNext,onSubError,onSubComplete)
    }

    //region    只执行一次的特定桩（带标志带线程名）
    fun p1() = pN(1)
    fun p2() = pN(2)
    fun p3() = pN(3)
    fun p4() = pN(4)
    fun p5() = pN(5)
    fun p6() = pN(6)
    fun p7() = pN(7)
    fun p8() = pN(8)
    //endregion

    /**
     * 一般桩函数（带线程名）
     */
    fun pill(tag:String){
        sb.append("${startDT()} P::$tag ${threadName()}\n")
    }

    //endregion

    //region    线程访问变量
    @Volatile
    private var isEnd:Boolean? = true
        set(value) {
            synchronized(lock) {
                if (value != field) {
                    synchronized(lock) {
                        field = value
                    }
                }
            }
        }
    //8个特定桩
    @Volatile
    private var flowPArray = arrayOf<String?>(null,null,null,null,null,null,null,null)
    @Volatile
    private var isNext:Boolean = true
    @Volatile
    private var sb = StringBuilder(10*1024)
    @Volatile
    private var startTick = 0L  //第一次动作时的时刻
    @Volatile
    private var nextCount = 0
    //endregion

    //region innner 函数或属性
    private inline fun checkRunning() = synchronized(lock){
        isEnd == false
    }
    private val lock:Any = Any()
    private var endActionFromRxModel:((RxLog)->Unit)? = null
    private var endAction:((RxLog)->Unit)? = null
    private inline fun pN(id:Int){
        if (flowPArray[id] != null) return
        flowPArray[id] = "${startDT()} P$id:${threadName()}\n"
        sb.append(flowPArray[id])
    }

    private fun innerExcute(source:Observable<out Any>,
                            onSubNext:((Any)->Unit)?,
                            onSubError:(()->Unit)?,
                            onSubComplete:(()->Unit)?){
        source
                .doOnDispose { postBreak() }
                //.subscribe(::preNext,::preError,::postComplete,::preSubscribe)
                .subscribe({
                    preNext(it)
                    onSubNext?.invoke(it)
                },{
                    preError(it)
                    onSubError?.invoke()
                },{
                    onSubComplete?.invoke()
                    postComplete()
                },::preSubscribe)
    }


    private inline fun initialize(){
        isEnd = false
        clear()
    }
    private inline fun end(){
        if (isEnd == false) {
            isEnd = true
            endAction?.invoke(this)
            endActionFromRxModel?.invoke(this)
            clear()
        }
    }
    private inline fun clear(){
        endAction = null
        sb.delete(0, sb.length)
        subscriptor = null
        disposer = null
        for (i in 0 until flowPArray.size){
            flowPArray[i] = null
        }
        nextCount = 0
        startTick = 0
    }
    private inline fun start(){
        sb.append("启动时间：${Date()}\n")
    }


    var disposer: Disposable? = null
        private set
    var subscriptor: Subscription? = null
        private set


    private inline fun startDT() = (System.currentTimeMillis()-startTick).no4()
    private inline fun threadName():String {
        val name = Thread.currentThread().name
        if (name.startsWith("Rx")) {
            return name.substring(2, 4) + name.substring(name.length - 2)
        }
        return name
    }

    //region    右对齐整数或序号的格式化
    private inline fun Int.no2() = when {
        this in 0..9 -> " $this"
        else -> "$this"
    }

    private inline fun Long.no4() = when (this) {
        in 0..9 -> "   $this"
        in 10..99 -> "  $this"
        in 100..999 -> " $this"
        else -> "$this"
    }
    //endregion

    //region    标准打印信息
    private inline fun postBreak(){
        val flow = "${startDT()} Bk($nextCount)\n"
        sb.append(flow)
        end()
    }

    private inline fun preNext(t:Any){
        if (isNext) {
            val flow = "${startDT()} Nt[${nextCount.no2()}]\n"
            sb.append(flow)
        }
        nextCount++
    }
    private inline fun preError(t:Throwable){
        t.printStackTrace()
        val flow = "${startDT()} Er($nextCount)\n"
        sb.append(flow)
        end()
    }
    private inline fun postComplete(){
        val  flow = "${startDT()} Ce($nextCount)\n"
        sb.append(flow)
        end()
    }
    //endregion

    /**
     * 前置Subscribe，引用Dispable
     */
    private inline fun preSubscribe(t: Disposable) {
        disposer = t
        startTick = System.currentTimeMillis()
    }

    /**
     * 前置Subscribe，引用Subscription
     */
    private inline fun preSubscribe(t: Subscription) {
        subscriptor = t
        startTick = System.currentTimeMillis()
    }
    //endregion
}