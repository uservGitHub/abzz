package gxd.book.business

class FileManager{
    companion object {

    }
}

class NewPage

class Node<T> {
    @Volatile
    internal var locking = false
        private set

    var obj: T? = null
        set(value) {
            locking = true
            field = value
            locking = false
        }
}

class TwoWayList(val size:Int) {
    private val container: Array<Node<NewPage>>
    internal var startInd = -1
        private set
    internal var endInd = -1
        private set(value) {
            field = (value+1).rem(size)
        }
    internal var realSize = -1
    init {
        container = Array<Node<NewPage>>(size, { Node<NewPage>() })
    }
    fun addBackward(page:NewPage){
        when{
            startInd == -1 || endInd == -1 ->{
                addOfEmpty(page)
            }
            realSize == size ->{
                container[++endInd].obj = page
            }
        }
    }
    fun addForward(page: NewPage){
        when{
            startInd == -1 || endInd == -1 ->{
                addOfEmpty(page)
            }

        }
    }
    private fun addOfEmpty(page: NewPage){
        container[0].obj = page
        realSize = 1
        startInd = 0
        endInd = 0
    }
}





