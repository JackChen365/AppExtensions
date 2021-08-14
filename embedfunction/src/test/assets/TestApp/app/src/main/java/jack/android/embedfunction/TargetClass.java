package jack.android.embedfunction;

public class TargetClass {
    private InternalClass internalClass;

    public TargetClass(){
        internalClass = new InternalClass();
    }

    public void printMessage(){
        System.out.println("Hello from TargetClass");
    }
}
