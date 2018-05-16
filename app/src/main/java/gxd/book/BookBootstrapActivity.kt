package gxd.book

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import com.example.work.myapplication.model.Cat
import com.example.work.myapplication.model.Dog
import com.example.work.myapplication.model.Person
import io.realm.Realm
import io.realm.Sort
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/**
 * Created by work on 2018/5/16.
 */

class BookBootstrapActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val headView = TextView().apply{
            text = "引导界面"
        }
        setContentView(headView)
        //setContentView(R.layout.activity_realm_basic_example)
    }
}

