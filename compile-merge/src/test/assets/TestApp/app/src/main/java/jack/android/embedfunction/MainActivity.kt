package jack.android.embedfunction

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import jack.android.embedfunction.R
import android.widget.Toast

/**
 * Created on 2021/8/18.
 *
 * @author Jack Chen
 * @email zhenchen@tubi.tv
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val view = findViewById<TextView>(R.id.text_view)
        view.setOnClickListener {
            Toast.makeText(applicationContext, "Click from MainActivity", Toast.LENGTH_SHORT).show()
        }
    }
}