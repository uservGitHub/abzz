

/**
 * 只进行添加元素的缓存区。
 * 适用场景：同一时刻只能在一端操作；在一端操作时，也需要顺序添加；不能多线程操作。
 * 1 满了以后会自动覆盖
 * 2 提供元素的顺序遍历
 */
open class OnlyAddBuffer(val size:Int) {

    protected val container = Array<Any?>(size, { null })
    protected var beforeDelete: ((Any) -> Unit)? = null
    protected var afterAdd: ((Any) -> Unit)? = null

    //region    可访问的范围[startIndex, endExcludeIndex]；-1 表示无效值，两个值相等表示满了
    //修改：可访问的范围[startIndex,endIndex]；-1 表示无效值，endIndex在startIndex前面，表示满了
    internal var startIndex: Int = -1
        private set(value) {
            if (moving) {
                if(!isEmpty) {
                    beforeDelete?.invoke(container[field]!!)
                    println("del [$field]=${container[field]}")
                }
                //不更新另一端，后移（被动更新）
                //field = if (value==size) 0 else value
                field = value
            } else {
                moving = true
                //前移
                field = if (value < 0) size - 1 else value
                if (field == endIndex) {
                    endIndex = if (field == 0) size - 1 else field - 1
                }
                moving = false
            }
        }
    internal var endIndex: Int = -1
        private set(value) {
            if (moving) {
                if (!isEmpty) {
                    beforeDelete?.invoke(container[field]!!)
                    println("del [$field]=${container[field]}")
                }
                //不更新另一端，前移（被动更新）
                //field = if (value<0) size-1 else value
                field = value
            } else {
                moving = true
                //后移
                field = if (value == size) 0 else value
                if (field == startIndex) {
                    startIndex = if (field == size - 1) 0 else field + 1
                }
                moving = false
            }
        }
    //endregion

    private fun addOfEmpty(ele: Any) {
        container[0] = ele
        println("int [0]=$ele")
        afterAdd?.invoke(container[0]!!)
        moving = true
        endIndex = 0
        startIndex = 0
        moving = false
    }

    //@Volatile
    private var moving = false

    fun addBackward(ele: Any) {
        if (isEmpty) addOfEmpty(ele)
        else {
            container[++endIndex] = ele
            println("bak [$endIndex]=$ele")
            afterAdd?.invoke(container[endIndex]!!)
        }
    }

    fun addForward(ele: Any) {
        if (isEmpty) addOfEmpty(ele)
        else {
            container[--startIndex] = ele
            println("fod [$startIndex]=$ele")
            afterAdd?.invoke(container[startIndex]!!)
        }
    }

    fun all(f: (ele: Any) -> Unit) {
        if (isEmpty) return

        when {
            startIndex == endIndex -> {
                f(container[startIndex]!!)
            }
            startIndex < endIndex -> {
                for (ind in startIndex..endIndex) {
                    f(container[ind]!!)
                }
            }
            else -> {
                for (ind in startIndex until size) {
                    f(container[ind]!!)
                }
                for (ind in 0..endIndex) {
                    f(container[ind]!!)
                }
            }
        }
    }

    val isEmpty: Boolean get() = startIndex == -1 || endIndex == -1

}