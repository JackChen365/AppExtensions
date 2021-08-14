package jack.android.gradle.embedfunction.asm;

import com.google.common.annotations.VisibleForTesting;
import jack.android.embedfunction.api.Decorate;

/**
 * Created on 2021/8/7.
 *
 * @author Jack Chen
 * @email bingo110@126.com
 */
@Decorate(target = TargetClass.class)
public class DecorateClass {
    @VisibleForTesting
    private InternalClass internalClass;

    public DecorateClass(){
        internalClass = new InternalClass();
        internalClass.testFunction();
    }

    public static void testMethod(){
        System.out.println("Method:testMethod1 from TargetClass");
    }

    public void testMethod2(){
        System.out.println("Method:testMethod2 from TargetClass");
    }

    public void printMessage(){
        System.out.println("printMessage2 from TargetClass");
        final InternalClass internalClass = new InternalClass();
        internalClass.testFunction();
    }
}
