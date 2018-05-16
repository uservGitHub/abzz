package gxd.book.crud

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import io.realm.RealmObject

/**
 * Created by work on 2018/5/15.
 */

class WhatAdapter<T:RealmObject>:BaseAdapter(){
    fun setData(details:List<T>?){
        if (details == null){
            items.clear()
        }else{
            items.addAll(details)
        }
        notifyDataSetChanged()
    }
    fun add(t:T){
        items.add(t)
        notifyDataSetChanged()
    }
    private var items = mutableListOf<T>()
    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): T {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
    override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
        return WhatView.toView(parent.context, items[position])
    }
}