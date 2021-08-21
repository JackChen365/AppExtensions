package jack.android.embedfunction

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cache.test.LRUCache
import jack.android.embedfunction.api.Decorate

/**
 * Created on 2021/8/18.
 *
 * @author Jack Chen
 * @email zhenchen@tubi.tv
 */
@Decorate(target = MainActivity::class)
class MainActivity_Decorate : AppCompatActivity() {
    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Toast.makeText(applicationContext, "Test message.", Toast.LENGTH_SHORT).show()
        val view = findViewById<TextView>(R.id.text_view)
        view.setOnClickListener { //Test class in jar
            val cache = LRUCache<Int>(3)
            cache.push(1)
            cache.push(2)
            cache.push(3)
            cache.push(4)
            cache.pop()
            cache.pop()
            cache.pop()

            //Test class in source code
            Toast.makeText(
                applicationContext,
                "Click from MainActivity_Decorate",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}