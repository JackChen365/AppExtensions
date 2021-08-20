package jack.android.embedfunction

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import jack.android.embedfunction.api.Decorate

@Decorate(target = MainActivity::class)
class MainActivity_Decorated : AppCompatActivity() {
    private var instance: MainActivity_Decorated?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.text_view).setOnClickListener {
            testFunction()
        }
    }

    fun testFunction(){
        InternalClass().testFunction(this@MainActivity_Decorated)
    }
}