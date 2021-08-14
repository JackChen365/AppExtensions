package jack.android.embedfunction

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import cache.test.LRUCache

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val cache = LRUCache<Int>(4)
        cache.push(1)
        cache.push(2)
        cache.push(3)
        cache.push(1)
        cache.push(4)
        cache.push(5)
        cache.push(2)
        cache.push(2)
        cache.push(1)
        println(1 == cache.pop())
        println(2 == cache.pop())
        println(5 == cache.pop())
        println(4 == cache.pop())
    }
}