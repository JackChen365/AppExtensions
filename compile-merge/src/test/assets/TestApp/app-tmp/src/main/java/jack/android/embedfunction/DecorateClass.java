package jack.android.embedfunction;

import jack.android.embedfunction.api.Decorate;

/**
 * Created on 2021/8/7.
 *
 * @author Jack Chen
 * @email bingo110@126.com
 */
@Decorate(target = TargetClass.class)
public class DecorateClass {
    private InternalClass internalClass;
    private CallListener listener;

    public DecorateClass() {
        internalClass = new InternalClass();
        internalClass.testFunction();
    }

    public static void testMethod() {
        System.out.println("Method:testMethod1 from TargetClass");
    }

    public void testMethod2() {
        System.out.println("Method:testMethod2 from TargetClass");
    }

    public void printMessage() {
        System.out.println("printMessage2 from TargetClass");
        final InternalClass internalClass = new InternalClass();
        internalClass.testFunction();
        setCallListener(new CallListener() {
            @Override public void onCall(final String message) {
                internalClass.testFunction();
                System.out.println(message);
            }
        });
        this.listener.onCall("call from inner class.");
    }

    public void setCallListener(CallListener listener) {
        this.listener = listener;
    }

    interface CallListener {
        void onCall(String message);
    }
}
