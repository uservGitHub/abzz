package gxd.book.utils

import java.util.concurrent.ExecutorService

/**
 * 自定义调度，只能使用已创建的CS1...CS4
 * 全部调度ALLCS
 * 某一个调度CS1.hook(),CS1.unHook()
 */
class CustomScheduler private constructor() {
    companion object {
        //所有自定义调度
        val ALLCS :List<CustomScheduler>
            get() = instances

        //region    自定义调度1-4
        val CS1: CustomScheduler by lazy {
            CustomScheduler()
        }
        val CS2: CustomScheduler by lazy {
            CustomScheduler()
        }
        val CS3: CustomScheduler by lazy {
            CustomScheduler()
        }
        val CS4: CustomScheduler by lazy {
            CustomScheduler()
        }
        //endregion
        private val instances = mutableListOf<CustomScheduler>()
    }
    private var scheduler: ExecutorService? = null
    private var clean: (() -> Unit)? = null
    init {
        instances.add(this)
    }

    /**
     * 钩子勾住
     */
    fun hook(currentHost: ExecutorService, clean: (() -> Unit)? = null) {
        scheduler = currentHost
        this.clean = clean
    }

    /**
     * 摘下钩子
     */
    fun unHook() {
        clean?.let {
            it.invoke()
            clean = null
        }
        scheduler?.let {
            if (!it.isShutdown) {
                it.shutdownNow()
            }
            scheduler = null
        }
    }
}
