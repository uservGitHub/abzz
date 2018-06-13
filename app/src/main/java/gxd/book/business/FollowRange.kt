/**
 * 接着上一次的范围（访问）
 */
class FollowRange(size:Int):OnlyAddBuffer(size){

    /**
     * 删除前的操作（基于该对象的释放资源、清理）
     */
    fun setOnDeleteListener(before:((Any)->Unit)){
        beforeDelete = before
    }

    /**
     * 添加后的操作（基于该对象的边界更新）
     */
    fun setOnAddListener(after:((Any)->Unit)){
        afterAdd = after
    }

    val startElement:Any get() = container[startIndex]!!
    val endElement:Any get() = container[endIndex]!!

    /**
     * 接着上次，从内部查询符合的范围，并顺序执行
     */
    fun allFollow(filterBeg:Any, filterEnd:Any, f:(Any)->Unit) {
        if (isEmpty) return

        val isNext = isToNext(filterBeg)

        val curFilterBegInd = if (lastSelectedBegInd == -1) startIndex
        else {
            var index = lastSelectedBegInd
            while (index != startIndex && index != endIndex) {
                //判断
                if (isSelectedElement(filterBeg, container[lastSelectedBegInd]!!)) {
                    break
                }
                //前移或后移
                index += if (isNext) 1 else -1
            }
            index
        }

        val curFilterEndInd = if (lastSelectedEndInd == -1) endIndex
        else{
            var index = lastSelectedEndInd
            while (index != startIndex && index != endIndex) {
                //判断
                if (isSelectedElement(filterEnd, container[lastSelectedBegInd]!!)) {
                    break
                }
                //前移或后移
                index += if (isNext) 1 else -1
            }
            index
        }

        //遍历
        //...

        //更新
        lastFilterBeg = filterBeg
        lastFilterEnd = filterEnd
        lastSelectedBegInd = curFilterBegInd
        lastSelectedEndInd = curFilterEndInd
    }
    private fun isToNext(curFilterBegin:Any):Boolean{
        //模拟两个过滤起始值的比较，当前值大，向后
        return curFilterBegin as Int > lastFilterBeg as Int
    }
    private fun isSelectedElement(curFilter:Any, ele:Any):Boolean{
        //模拟选中元素的条件，相等选中
        return curFilter as Int == ele as Int
    }
    //前一次过滤起始值
    var lastFilterBeg:Any? = null
        private set
    //前一次过滤终止值
    var lastFilterEnd:Any? = null
        private set
    //前一次选中的起始索引
    var lastSelectedBegInd:Int = -1
        private set
    //前一次选中的终止索引
    var lastSelectedEndInd:Int = -1
        private set

}