package jack.android.embedfunction;

import android.content.Context;
import android.widget.Toast;

/**
 * Created on 2021/8/7.
 *
 * @author Jack Chen
 * @email bingo110@126.com
 */
class InternalClass {
    public void testFunction(){
        System.out.println("Test from Internal");
    }

    public void testFunction(Context context){
        Toast.makeText(context,"Test from Internal", Toast.LENGTH_SHORT).show();
    }
}
