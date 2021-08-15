package jack.android.embedfunction

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import jack.android.embedfunction.api.Decorate

@Decorate(target = MainActivity::class)
class MainActivity_Decorated : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.text_view).setOnClickListener {
            InternalClass().testFunction()
            Toast.makeText(applicationContext,"Test",Toast.LENGTH_SHORT).show()
        }
    }


    fun add(){
        InternalClass().testFunction()
    }
}