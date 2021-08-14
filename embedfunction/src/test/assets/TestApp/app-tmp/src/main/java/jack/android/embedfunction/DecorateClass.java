package jack.android.embedfunction;

import androidx.annotation.VisibleForTesting;
import jack.android.embedfunction.api.Decorate;

/**
 * Decorate the class that in source folder.
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
